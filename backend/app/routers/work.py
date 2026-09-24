"""
Landowner and contractor verification.

Every endpoint derives the caller from the validated token and then confirms the
caller is a party to the record before touching it. A record the caller is not
a party to returns 404 rather than 403, so the API cannot be used to discover
which ids exist.

Who may do what:
  landowner   create a job, review evidence, record a payment they made
  contractor  accept a job, upload evidence, submit, confirm a payment received
"""
import math
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

from app.dependencies import require_user
from app.services.supabase import supabase

router = APIRouter(prefix="/api", tags=["work"])

TASK_TYPES = {"spray", "plough", "fertilizer", "sowing", "harvest", "irrigation", "other"}
METHODS = {"cash", "jazzcash", "easypaisa", "bank"}

# A gap above this between the device clock and the server clock is flagged.
CLOCK_GAP_WARNING_SECONDS = 6 * 60 * 60


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------
def _now():
    return datetime.now(timezone.utc).isoformat()


def _haversine_m(lat1, lng1, lat2, lng2):
    r = 6_371_000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lng2 - lng1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


async def _one(table, params):
    code, rows = await supabase.select(table, {"select": "*", "limit": "1", **params})
    if code >= 400:
        raise HTTPException(code, str(rows))
    return rows[0] if rows else None


async def _role(user_id):
    profile = await _one("profiles", {"id": f"eq.{user_id}"})
    return (profile or {}).get("role") or "landowner"


async def _order_for(user, order_id):
    """Return the work order if the caller is one of its two parties."""
    order = await _one("work_orders", {"id": f"eq.{order_id}"})
    if not order or user["id"] not in (order["landowner_id"], order["contractor_id"]):
        raise HTTPException(404, "Work order not found")
    return order


def _require(condition, message, code=409):
    if not condition:
        raise HTTPException(code, message)


async def _system_note(order_id, text):
    """Record what just happened, so the history reads itself back.

    A failure here must not undo the action that triggered it, so the write is
    deliberately not allowed to raise.
    """
    await supabase.insert("work_messages", {
        "work_order_id": order_id,
        "sender_id": None,
        "kind": "system",
        "body": text,
    })


async def _set_status(order, **fields):
    code, rows = await supabase.update(
        "work_orders",
        {"id": f"eq.{order['id']}"},
        fields,
    )
    if code >= 400:
        raise HTTPException(code, str(rows))
    return rows[0] if rows else {**order, **fields}


# ---------------------------------------------------------------------------
# role
# ---------------------------------------------------------------------------
class RoleIn(BaseModel):
    role: str


@router.get("/work/role")
async def get_role(user: dict = Depends(require_user)):
    return {"success": True, "role": await _role(user["id"])}


@router.post("/work/role")
async def set_role(x: RoleIn, user: dict = Depends(require_user)):
    _require(x.role in ("landowner", "contractor"), "Role must be landowner or contractor", 422)
    code, rows = await supabase.update("profiles", {"id": f"eq.{user['id']}"}, {"role": x.role})
    if code >= 400:
        raise HTTPException(code, str(rows))
    return {"success": True, "role": x.role}


# ---------------------------------------------------------------------------
# contractor links
# ---------------------------------------------------------------------------
class InviteIn(BaseModel):
    email: str | None = None
    phone: str | None = None


@router.post("/contractors/invite")
async def invite_contractor(x: InviteIn, user: dict = Depends(require_user)):
    _require(await _role(user["id"]) == "landowner", "Only a landowner can add contractors", 403)
    lookup = x.email.strip().lower() if x.email else (x.phone or "").strip()
    _require(bool(lookup), "Enter the contractor's email or phone", 422)

    field = "email" if x.email else "phone"
    contractor = await _one("profiles", {field: f"eq.{lookup}"})
    # Same message whether the account is missing or is not a contractor, so the
    # endpoint cannot be used to probe who is registered.
    if not contractor or contractor.get("role") != "contractor" or contractor["id"] == user["id"]:
        raise HTTPException(404, "No contractor account found with those details")

    existing = await _one("contractor_links", {
        "landowner_id": f"eq.{user['id']}",
        "contractor_id": f"eq.{contractor['id']}",
    })
    if existing:
        if existing["status"] == "removed":
            await supabase.update("contractor_links", {"id": f"eq.{existing['id']}"}, {"status": "pending"})
        return {"success": True, "link_id": existing["id"], "status": existing["status"]}

    code, rows = await supabase.insert_returning("contractor_links", {
        "landowner_id": user["id"],
        "contractor_id": contractor["id"],
    })
    if code >= 400:
        raise HTTPException(code, str(rows))
    return {"success": True, "link_id": rows[0]["id"], "status": "pending"}


@router.post("/contractors/{link_id}/accept")
async def accept_link(link_id: str, user: dict = Depends(require_user)):
    link = await _one("contractor_links", {"id": f"eq.{link_id}"})
    if not link or link["contractor_id"] != user["id"]:
        raise HTTPException(404, "Invitation not found")
    await supabase.update("contractor_links", {"id": f"eq.{link_id}"}, {"status": "active"})
    return {"success": True}


@router.get("/contractors")
async def list_links(user: dict = Depends(require_user)):
    """Landowners see their contractors; contractors see who has invited them."""
    role = await _role(user["id"])
    key = "landowner_id" if role == "landowner" else "contractor_id"
    other = "contractor_id" if role == "landowner" else "landowner_id"
    code, links = await supabase.select("contractor_links", {
        "select": "*", key: f"eq.{user['id']}", "status": "neq.removed", "order": "created_at.desc",
    })
    if code >= 400:
        raise HTTPException(code, str(links))

    ids = [l[other] for l in links]
    names = {}
    if ids:
        code, profiles = await supabase.select("profiles", {
            "select": "id,name,phone,email", "id": f"in.({','.join(ids)})",
        })
        if code < 400:
            names = {p["id"]: p for p in profiles}

    return {"success": True, "role": role, "links": [
        {**l, "other_party": names.get(l[other], {})} for l in links
    ]}


# ---------------------------------------------------------------------------
# work orders
# ---------------------------------------------------------------------------
class WorkOrderIn(BaseModel):
    contractor_id: str
    task_type: str
    field_name: str
    crop_name: str | None = None
    notes: str | None = None
    area_acres: float = Field(gt=0)
    rate_per_acre: float = Field(ge=0)
    advance_amount: float = Field(default=0, ge=0)
    field_lat: float | None = None
    field_lng: float | None = None
    scheduled_date: str | None = None


@router.post("/work-orders")
async def create_order(x: WorkOrderIn, user: dict = Depends(require_user)):
    _require(await _role(user["id"]) == "landowner", "Only a landowner can create work", 403)
    _require(x.task_type in TASK_TYPES, "Unknown task type", 422)
    _require(x.advance_amount <= x.area_acres * x.rate_per_acre,
             "Advance cannot exceed the agreed total", 422)

    link = await _one("contractor_links", {
        "landowner_id": f"eq.{user['id']}",
        "contractor_id": f"eq.{x.contractor_id}",
        "status": "eq.active",
    })
    _require(link is not None, "This contractor has not accepted your invitation yet", 403)

    payload = x.model_dump()
    payload["landowner_id"] = user["id"]
    code, rows = await supabase.insert_returning("work_orders", payload)
    if code >= 400:
        raise HTTPException(code, str(rows))
    return {"success": True, "order": rows[0]}


async def _with_balances(orders):
    if not orders:
        return orders
    ids = ",".join(o["id"] for o in orders)
    code, balances = await supabase.select("work_order_balances", {
        "select": "*", "work_order_id": f"in.({ids})",
    })
    by_id = {b["work_order_id"]: b for b in balances} if code < 400 else {}
    return [{**o, "balance": by_id.get(o["id"])} for o in orders]


@router.get("/work-orders")
async def list_orders(user: dict = Depends(require_user)):
    role = await _role(user["id"])
    key = "landowner_id" if role == "landowner" else "contractor_id"
    code, orders = await supabase.select("work_orders", {
        "select": "*", key: f"eq.{user['id']}", "order": "created_at.desc",
    })
    if code >= 400:
        raise HTTPException(code, str(orders))

    orders = await _with_balances(orders)
    orders = await _with_unread_for(orders, user["id"], role)
    return {"success": True, "role": role, "orders": orders}


async def _with_unread_for(orders, user_id, role):
    """Attach each job's unread message count. A reader never sees their own
    message counted as unread."""
    if not orders:
        return orders
    ids = ",".join(o["id"] for o in orders)
    column = "read_by_landowner_at" if role == "landowner" else "read_by_contractor_at"
    code, rows = await supabase.select("work_messages", {
        "select": "work_order_id,sender_id",
        "work_order_id": f"in.({ids})",
        column: "is.null",
    })
    counts: dict[str, int] = {}
    if code < 400:
        for m in rows:
            if m.get("sender_id") == user_id:
                continue
            counts[m["work_order_id"]] = counts.get(m["work_order_id"], 0) + 1
    return [{**o, "unread_messages": counts.get(o["id"], 0)} for o in orders]


@router.get("/work-orders/{order_id}")
async def get_order(order_id: str, user: dict = Depends(require_user)):
    order = await _order_for(user, order_id)

    code, proofs = await supabase.select("work_proofs", {
        "select": "*", "work_order_id": f"eq.{order_id}", "order": "captured_at.asc",
    })
    proofs = proofs if code < 400 else []
    for p in proofs:
        sc, signed = await supabase.signed_url("work-proofs", p["image_path"], 3600)
        p["image_url"] = signed.get("url") if sc < 400 and isinstance(signed, dict) else None
        try:
            gap = abs((datetime.fromisoformat(p["uploaded_at"].replace("Z", "+00:00"))
                       - datetime.fromisoformat(p["captured_at"].replace("Z", "+00:00"))).total_seconds())
        except (KeyError, ValueError, AttributeError):
            gap = 0
        p["clock_warning"] = gap > CLOCK_GAP_WARNING_SECONDS

    code, payments = await supabase.select("work_payments", {
        "select": "*", "work_order_id": f"eq.{order_id}", "order": "created_at.asc",
    })
    payments = payments if code < 400 else []

    balance = await _one("work_order_balances", {"work_order_id": f"eq.{order_id}"})
    return {
        "success": True,
        "viewer_role": "landowner" if user["id"] == order["landowner_id"] else "contractor",
        "order": order,
        "proofs": proofs,
        "payments": payments,
        "balance": balance,
    }


@router.post("/work-orders/{order_id}/accept")
async def accept_order(order_id: str, user: dict = Depends(require_user)):
    order = await _order_for(user, order_id)
    _require(user["id"] == order["contractor_id"], "Only the assigned contractor can accept", 403)
    _require(order["status"] == "proposed", "This job is no longer awaiting acceptance")
    updated = await _set_status(order, status="accepted", accepted_at=_now())
    await _system_note(order_id, f"Contractor accepted the job at {order['rate_per_acre']} per acre.")
    return {"success": True, "order": updated}


@router.post("/work-orders/{order_id}/decline")
async def decline_order(order_id: str, user: dict = Depends(require_user)):
    order = await _order_for(user, order_id)
    _require(user["id"] == order["contractor_id"], "Only the assigned contractor can decline", 403)
    _require(order["status"] == "proposed", "This job is no longer awaiting acceptance")
    return {"success": True, "order": await _set_status(order, status="cancelled")}


@router.post("/work-orders/{order_id}/proofs")
async def upload_proof(
    order_id: str,
    file: UploadFile = File(...),
    stage: str = Form(...),
    latitude: float = Form(...),
    longitude: float = Form(...),
    captured_at: str = Form(...),
    accuracy_m: float | None = Form(None),
    is_mock_location: bool = Form(False),
    user: dict = Depends(require_user),
):
    order = await _order_for(user, order_id)
    _require(user["id"] == order["contractor_id"], "Only the assigned contractor can upload evidence", 403)
    _require(order["status"] in ("accepted", "disputed"), "Evidence can only be added to an accepted job")
    _require(stage in ("before", "after", "receipt"), "Unknown stage", 422)
    _require((file.content_type or "").startswith("image/"), "Only images are accepted", 400)

    content = await file.read()
    _require(len(content) <= 10 * 1024 * 1024, "Image exceeds 10 MB", 413)

    distance = None
    if order.get("field_lat") is not None and order.get("field_lng") is not None:
        distance = round(_haversine_m(order["field_lat"], order["field_lng"], latitude, longitude), 1)

    path = f"{user['id']}/{order_id}/{stage}-{uuid.uuid4().hex}.jpg"
    code, data = await supabase.upload_storage("work-proofs", path, content, file.content_type or "image/jpeg")
    if code >= 400:
        raise HTTPException(code, str(data))

    code, rows = await supabase.insert_returning("work_proofs", {
        "work_order_id": order_id,
        "contractor_id": user["id"],
        "stage": stage,
        "image_path": path,
        "latitude": latitude,
        "longitude": longitude,
        "accuracy_m": accuracy_m,
        "captured_at": captured_at,
        "distance_from_field_m": distance,
        "is_mock_location": is_mock_location,
    })
    if code >= 400:
        raise HTTPException(code, str(rows))
    return {"success": True, "proof": rows[0]}


@router.post("/work-orders/{order_id}/submit")
async def submit_order(order_id: str, user: dict = Depends(require_user)):
    order = await _order_for(user, order_id)
    _require(user["id"] == order["contractor_id"], "Only the assigned contractor can submit", 403)
    _require(order["status"] in ("accepted", "disputed"), "This job cannot be submitted now")

    code, proofs = await supabase.select("work_proofs", {
        "select": "stage", "work_order_id": f"eq.{order_id}",
    })
    stages = {p["stage"] for p in (proofs if code < 400 else [])}
    _require("after" in stages, "Add at least one photo of the finished work before submitting", 422)

    updated = await _set_status(order, status="submitted", submitted_at=_now())
    await _system_note(order_id, "Contractor submitted the work for review.")
    return {"success": True, "order": updated}


class ReviewIn(BaseModel):
    decision: str                 # approved | partial | disputed
    verified_acres: float | None = None
    note: str | None = None


@router.post("/work-orders/{order_id}/review")
async def review_order(order_id: str, x: ReviewIn, user: dict = Depends(require_user)):
    order = await _order_for(user, order_id)
    _require(user["id"] == order["landowner_id"], "Only the landowner can review this job", 403)
    _require(order["status"] == "submitted", "This job is not awaiting review")
    area = float(order["area_acres"])

    if x.decision == "approved":
        fields = {"status": "approved", "verified_acres": area}
    elif x.decision == "partial":
        _require(x.verified_acres is not None and 0 < x.verified_acres < area,
                 f"Verified acres must be more than 0 and less than {area}", 422)
        fields = {"status": "partial", "verified_acres": x.verified_acres}
    elif x.decision == "disputed":
        _require(bool((x.note or "").strip()), "Give a reason for the dispute", 422)
        fields = {"status": "disputed"}
    else:
        raise HTTPException(422, "Decision must be approved, partial or disputed")

    fields.update(review_note=(x.note or "").strip() or None, reviewed_at=_now())
    updated = await _set_status(order, **fields)

    if x.decision == "approved":
        note = f"Landowner approved all {area} acres."
    elif x.decision == "partial":
        note = f"Landowner verified {x.verified_acres} of {area} acres."
    else:
        note = "Landowner raised a dispute."
    if (x.note or "").strip():
        note += f" Note: {x.note.strip()}"
    await _system_note(order_id, note)

    return {"success": True, "order": updated}


# ---------------------------------------------------------------------------
# payments
# ---------------------------------------------------------------------------
class PaymentIn(BaseModel):
    kind: str
    amount: float = Field(gt=0)
    method: str
    transaction_ref: str | None = None
    note: str | None = None


@router.post("/work-orders/{order_id}/payments")
async def record_payment(order_id: str, x: PaymentIn, user: dict = Depends(require_user)):
    """The landowner records money paid; the contractor must confirm receipt."""
    order = await _order_for(user, order_id)
    _require(user["id"] == order["landowner_id"], "Only the landowner records payments", 403)
    _require(order["status"] not in ("proposed", "cancelled"),
             "Payments can be recorded once the contractor has accepted")
    _require(x.kind in ("advance", "balance", "expense"), "Unknown payment type", 422)
    _require(x.method in METHODS, "Unknown payment method", 422)
    if x.method != "cash":
        _require(bool((x.transaction_ref or "").strip()),
                 "A transaction ID is required for digital payments", 422)

    code, rows = await supabase.insert_returning("work_payments", {
        "work_order_id": order_id,
        "payer_id": order["landowner_id"],
        "payee_id": order["contractor_id"],
        "kind": x.kind,
        "amount": x.amount,
        "method": x.method,
        "transaction_ref": (x.transaction_ref or "").strip() or None,
        "note": (x.note or "").strip() or None,
    })
    if code >= 400:
        raise HTTPException(code, str(rows))
    return {"success": True, "payment": rows[0]}


class ConfirmIn(BaseModel):
    received: bool


@router.post("/payments/{payment_id}/confirm")
async def confirm_payment(payment_id: str, x: ConfirmIn, user: dict = Depends(require_user)):
    payment = await _one("work_payments", {"id": f"eq.{payment_id}"})
    # Only the person who received the money can confirm it.
    if not payment or payment["payee_id"] != user["id"]:
        raise HTTPException(404, "Payment not found")
    _require(payment["status"] == "pending", "This payment has already been answered")

    status = "confirmed" if x.received else "rejected"
    code, rows = await supabase.update("work_payments", {"id": f"eq.{payment_id}"}, {
        "status": status, "payee_confirmed_at": _now(),
    })
    if code >= 400:
        raise HTTPException(code, str(rows))

    if status == "confirmed":
        order = await _one("work_orders", {"id": f"eq.{payment['work_order_id']}"})
        await _post_to_ledgers(order, payment)
        await _system_note(
            order["id"],
            f"Contractor confirmed receiving {payment['amount']} by {payment['method']}.",
        )
        await _close_if_settled(order)
    else:
        await _system_note(
            payment["work_order_id"],
            f"Contractor reported not receiving {payment['amount']}.",
        )

    return {"success": True, "status": status}


async def _post_to_ledgers(order, payment):
    """Write the confirmed payment to both parties' Smart Khata."""
    label = f"Contractor work - {order['task_type']}, {order['field_name']}"
    today = datetime.now(timezone.utc).date().isoformat()
    common = {
        "category": "contractor",
        "crop_name": order.get("crop_name") or "General",
        "amount": float(payment["amount"]),
        "field_name": order["field_name"],
        "description": label,
        "transaction_date": today,
        "work_order_id": order["id"],
    }
    await supabase.insert("khata_transactions", {**common, "user_id": order["landowner_id"], "type": "expense"})
    await supabase.insert("khata_transactions", {**common, "user_id": order["contractor_id"], "type": "income"})


async def _close_if_settled(order):
    if order["status"] not in ("approved", "partial"):
        return
    balance = await _one("work_order_balances", {"work_order_id": f"eq.{order['id']}"})
    if balance and float(balance.get("balance") or 0) <= 0:
        await supabase.update("work_orders", {"id": f"eq.{order['id']}"}, {"status": "closed"})



# ---------------------------------------------------------------------------
# messages
# ---------------------------------------------------------------------------
class MessageIn(BaseModel):
    body: str


def _read_column(order, user_id):
    return ("read_by_landowner_at" if user_id == order["landowner_id"]
            else "read_by_contractor_at")


@router.get("/work-orders/{order_id}/messages")
async def list_messages(order_id: str, user: dict = Depends(require_user)):
    """Return the conversation and mark it read for whoever is reading it."""
    order = await _order_for(user, order_id)

    code, rows = await supabase.select("work_messages", {
        "select": "*", "work_order_id": f"eq.{order_id}", "order": "created_at.asc",
    })
    if code >= 400:
        raise HTTPException(code, str(rows))

    column = _read_column(order, user["id"])
    unread = [m for m in rows if not m.get(column)]
    if unread:
        await supabase.update(
            "work_messages",
            {"work_order_id": f"eq.{order_id}", column: "is.null"},
            {column: _now()},
        )

    return {"success": True, "messages": rows, "viewer_id": user["id"]}


@router.post("/work-orders/{order_id}/messages")
async def send_message(order_id: str, x: MessageIn, user: dict = Depends(require_user)):
    order = await _order_for(user, order_id)
    body = (x.body or "").strip()
    _require(bool(body), "Write a message first", 422)
    _require(len(body) <= 2000, "Message is too long", 422)
    _require(order["status"] != "cancelled", "This job has been cancelled")

    # The sender's own side is marked read at the moment of sending, so their
    # own message never comes back as unread to them.
    payload = {
        "work_order_id": order_id,
        "sender_id": user["id"],
        "kind": "text",
        "body": body,
        _read_column(order, user["id"]): _now(),
    }
    code, rows = await supabase.insert_returning("work_messages", payload)
    if code >= 400:
        raise HTTPException(code, str(rows))
    return {"success": True, "message": rows[0]}
