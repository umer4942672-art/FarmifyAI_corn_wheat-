"""
Exercise the whole contractor verification flow against an in-memory stand-in
for Supabase, so the permission rules and the balance arithmetic are checked
without a live project.
"""
import os
import uuid

os.environ.update(SUPABASE_URL="https://x.supabase.co", SUPABASE_ANON_KEY="a",
                  SUPABASE_SERVICE_ROLE_KEY="b", GEMINI_API_KEY="c")

from fastapi import Header
from fastapi.testclient import TestClient

import app.dependencies as deps
import app.routers.work as work
from app.main import app


class FakeSupabase:
    configured = True

    def __init__(self):
        self.t = {k: [] for k in ["profiles", "contractor_links", "work_orders",
                                  "work_proofs", "work_payments", "khata_transactions",
                                  "work_messages"]}

    # --- query helpers -----------------------------------------------------
    def _match(self, row, params):
        for k, v in params.items():
            if k in ("select", "order", "limit"):
                continue
            op, _, val = str(v).partition(".")
            cell = row.get(k)
            if op == "eq" and str(cell) != val:
                return False
            if op == "neq" and str(cell) == val:
                return False
            if op == "in" and str(cell) not in val.strip("()").split(","):
                return False
            if op == "is" and val == "null" and cell is not None:
                return False
        return True

    def _view(self):
        out = []
        for w in self.t["work_orders"]:
            pays = [p for p in self.t["work_payments"] if p["work_order_id"] == w["id"]]
            paid = sum(p["amount"] for p in pays if p["status"] == "confirmed" and p["kind"] != "expense")
            payable = w["rate_per_acre"] * (w.get("verified_acres") or 0)
            out.append({"work_order_id": w["id"], "landowner_id": w["landowner_id"],
                        "contractor_id": w["contractor_id"], "agreed_amount": w["total_amount"],
                        "payable": payable, "paid": paid, "balance": payable - paid})
        return out

    # --- API used by the router -------------------------------------------
    async def select(self, table, params=None):
        rows = self._view() if table == "work_order_balances" else self.t[table]
        res = [r for r in rows if self._match(r, params or {})]
        if (params or {}).get("limit"):
            res = res[: int(params["limit"])]
        return 200, [dict(r) for r in res]

    async def insert_returning(self, table, payload):
        row = {"id": str(uuid.uuid4()), **payload}
        if table == "work_orders":
            row.update(status="proposed", verified_acres=None,
                       total_amount=payload["area_acres"] * payload["rate_per_acre"])
        if table == "contractor_links":
            row["status"] = "pending"
        if table == "work_payments":
            row["status"] = "pending"
        if table == "work_proofs":
            row["uploaded_at"] = payload["captured_at"]
        if table == "work_messages":
            row.setdefault("sender_id", None)
            row.setdefault("read_by_landowner_at", None)
            row.setdefault("read_by_contractor_at", None)
        self.t[table].append(row)
        return 201, [dict(row)]

    async def insert(self, table, payload):
        row = {"id": str(uuid.uuid4()), **payload}
        if table == "work_messages":
            row.setdefault("sender_id", None)
            row.setdefault("read_by_landowner_at", None)
            row.setdefault("read_by_contractor_at", None)
        self.t[table].append(row)
        return 201, ""

    async def update(self, table, params, payload):
        hit = [r for r in self.t[table] if self._match(r, params)]
        for r in hit:
            r.update(payload)
        return 200, [dict(r) for r in hit]

    async def upload_storage(self, *a, **k):
        return 200, {}

    async def signed_url(self, *a, **k):
        return 200, {"url": "https://signed"}


fake = FakeSupabase()
work.supabase = fake

USERS = {"tok_owner": "owner", "tok_ctr": "ctr", "tok_other": "other"}
for uid, role, email in [("owner", "landowner", "o@x.pk"), ("ctr", "contractor", "c@x.pk"),
                         ("other", "landowner", "z@x.pk")]:
    fake.t["profiles"].append({"id": uid, "role": role, "email": email, "name": uid})


async def fake_user(authorization: str | None = Header(default=None)):
    return {"id": USERS[authorization.split(" ", 1)[1]]}


app.dependency_overrides[deps.require_user] = fake_user
c = TestClient(app)
H = lambda t: {"Authorization": f"Bearer {t}"}

checks = []


def check(name, cond):
    checks.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name)


# --- links ----------------------------------------------------------------
r = c.post("/api/contractors/invite", json={"email": "c@x.pk"}, headers=H("tok_owner"))
check("landowner invites contractor", r.status_code == 200)
link_id = r.json()["link_id"]

r = c.post("/api/contractors/invite", json={"email": "o@x.pk"}, headers=H("tok_ctr"))
check("contractor cannot invite", r.status_code == 403)

order_body = {"contractor_id": "ctr", "task_type": "spray", "field_name": "Khet 3",
              "area_acres": 10, "rate_per_acre": 1500, "advance_amount": 5000,
              "field_lat": 31.52, "field_lng": 74.35}
r = c.post("/api/work-orders", json=order_body, headers=H("tok_owner"))
check("cannot create work before invitation accepted", r.status_code == 403)

r = c.post(f"/api/contractors/{link_id}/accept", headers=H("tok_other"))
check("another user cannot accept someone else's invitation", r.status_code == 404)
c.post(f"/api/contractors/{link_id}/accept", headers=H("tok_ctr"))

# --- order ----------------------------------------------------------------
r = c.post("/api/work-orders", json={**order_body, "advance_amount": 99999}, headers=H("tok_owner"))
check("advance above total rejected", r.status_code == 422)

r = c.post("/api/work-orders", json=order_body, headers=H("tok_owner"))
check("work order created", r.status_code == 200)
oid = r.json()["order"]["id"]
check("total computed as 10 x 1500", r.json()["order"]["total_amount"] == 15000)

r = c.get(f"/api/work-orders/{oid}", headers=H("tok_other"))
check("unrelated user gets 404 on the order", r.status_code == 404)

r = c.post(f"/api/work-orders/{oid}/submit", headers=H("tok_ctr"))
check("cannot submit before accepting", r.status_code == 409)

r = c.post(f"/api/work-orders/{oid}/accept", headers=H("tok_owner"))
check("landowner cannot accept own job", r.status_code == 403)
r = c.post(f"/api/work-orders/{oid}/accept", headers=H("tok_ctr"))
check("contractor accepts rate", r.status_code == 200 and r.json()["order"]["status"] == "accepted")

# --- evidence -------------------------------------------------------------
r = c.post(f"/api/work-orders/{oid}/submit", headers=H("tok_ctr"))
check("cannot submit without an after photo", r.status_code == 422)

img = ("p.jpg", b"\xff\xd8\xff fake jpeg", "image/jpeg")
far = {"stage": "after", "latitude": "31.60", "longitude": "74.35",
       "captured_at": "2026-09-14T10:00:00+00:00"}
r = c.post(f"/api/work-orders/{oid}/proofs", data=far, files={"file": img}, headers=H("tok_ctr"))
check("proof uploaded", r.status_code == 200)
dist = r.json()["proof"]["distance_from_field_m"]
check(f"distance from field computed (~{dist:.0f} m)", 8000 < dist < 10000)

r = c.post(f"/api/work-orders/{oid}/proofs", data=far, files={"file": img}, headers=H("tok_owner"))
check("landowner cannot upload evidence", r.status_code == 403)

r = c.post(f"/api/work-orders/{oid}/submit", headers=H("tok_ctr"))
check("contractor submits", r.status_code == 200 and r.json()["order"]["status"] == "submitted")

# --- review ---------------------------------------------------------------
r = c.post(f"/api/work-orders/{oid}/review", json={"decision": "approved"}, headers=H("tok_ctr"))
check("contractor cannot review", r.status_code == 403)
r = c.post(f"/api/work-orders/{oid}/review", json={"decision": "disputed"}, headers=H("tok_owner"))
check("dispute needs a reason", r.status_code == 422)
r = c.post(f"/api/work-orders/{oid}/review", json={"decision": "partial", "verified_acres": 12},
           headers=H("tok_owner"))
check("partial acres above total rejected", r.status_code == 422)
r = c.post(f"/api/work-orders/{oid}/review", json={"decision": "partial", "verified_acres": 6},
           headers=H("tok_owner"))
check("partial approval of 6 acres", r.status_code == 200 and r.json()["order"]["status"] == "partial")

# --- payments -------------------------------------------------------------
r = c.post(f"/api/work-orders/{oid}/payments",
           json={"kind": "advance", "amount": 5000, "method": "jazzcash"}, headers=H("tok_owner"))
check("digital payment without transaction id rejected", r.status_code == 422)

r = c.post(f"/api/work-orders/{oid}/payments",
           json={"kind": "advance", "amount": 5000, "method": "cash"}, headers=H("tok_owner"))
pid = r.json()["payment"]["id"]
bal = c.get(f"/api/work-orders/{oid}", headers=H("tok_owner")).json()["balance"]
check("unconfirmed payment not counted", bal["paid"] == 0 and bal["payable"] == 9000)

r = c.post(f"/api/payments/{pid}/confirm", json={"received": True}, headers=H("tok_owner"))
check("payer cannot confirm own payment", r.status_code == 404)
r = c.post(f"/api/payments/{pid}/confirm", json={"received": True}, headers=H("tok_ctr"))
check("payee confirms receipt", r.status_code == 200)

bal = c.get(f"/api/work-orders/{oid}", headers=H("tok_owner")).json()["balance"]
check(f"balance = 6 x 1500 - 5000 = {bal['balance']}", bal["balance"] == 4000)

khata = fake.t["khata_transactions"]
check("expense posted to landowner khata", any(k["user_id"] == "owner" and k["type"] == "expense" for k in khata))
check("income posted to contractor khata", any(k["user_id"] == "ctr" and k["type"] == "income" for k in khata))

r = c.post(f"/api/work-orders/{oid}/payments",
           json={"kind": "balance", "amount": 4000, "method": "easypaisa", "transaction_ref": "EP123"},
           headers=H("tok_owner"))
c.post(f"/api/payments/{r.json()['payment']['id']}/confirm", json={"received": True}, headers=H("tok_ctr"))
status = c.get(f"/api/work-orders/{oid}", headers=H("tok_owner")).json()["order"]["status"]
check("job closes automatically once settled", status == "closed")

# --- messages -------------------------------------------------------------
r = c.post(f"/api/work-orders/{oid}/messages", json={"body": "  "}, headers=H("tok_owner"))
check("empty message rejected", r.status_code == 422)

r = c.post(f"/api/work-orders/{oid}/messages", json={"body": "Kal subah shuru karein"},
           headers=H("tok_owner"))
check("landowner sends a message", r.status_code == 200)

r = c.post(f"/api/work-orders/{oid}/messages", json={"body": "Theek hai"}, headers=H("tok_other"))
check("outsider cannot message on someone else's job", r.status_code == 404)

msgs = c.get(f"/api/work-orders/{oid}/messages", headers=H("tok_ctr")).json()["messages"]
system = [m for m in msgs if m["kind"] == "system"]
check(f"system notes recorded ({len(system)} of them)", len(system) >= 4)
check("accepted note present", any("accepted the job" in m["body"] for m in system))
check("review note records verified acres", any("verified 6.0" in m["body"] or "verified 6" in m["body"] for m in system))
check("payment note present", any("confirmed receiving" in m["body"] for m in system))
check("typed message present", any(m["body"] == "Kal subah shuru karein" for m in msgs))

orders = c.get("/api/work-orders", headers=H("tok_ctr")).json()["orders"]
this = next(o for o in orders if o["id"] == oid)
check("unread clears once the contractor has read", this["unread_messages"] == 0)

c.post(f"/api/work-orders/{oid}/messages", json={"body": "Ek aur baat"}, headers=H("tok_owner"))
orders = c.get("/api/work-orders", headers=H("tok_ctr")).json()["orders"]
this = next(o for o in orders if o["id"] == oid)
check("new message shows as unread to the other side", this["unread_messages"] == 1)

# The landowner has system notes waiting until they open the thread, which is
# correct. Read it first, then send, and only their own message is in play.
c.get(f"/api/work-orders/{oid}/messages", headers=H("tok_owner"))
c.post(f"/api/work-orders/{oid}/messages", json={"body": "Aur ek"}, headers=H("tok_owner"))
orders = c.get("/api/work-orders", headers=H("tok_owner")).json()["orders"]
this = next(o for o in orders if o["id"] == oid)
check("sender does not see their own message as unread", this["unread_messages"] == 0)

orders = c.get("/api/work-orders", headers=H("tok_ctr")).json()["orders"]
this = next(o for o in orders if o["id"] == oid)
check("the other side sees both new messages", this["unread_messages"] == 2)

passed = sum(1 for _, ok in checks if ok)
print(f"\n{passed}/{len(checks)} checks passed")
raise SystemExit(0 if passed == len(checks) else 1)
