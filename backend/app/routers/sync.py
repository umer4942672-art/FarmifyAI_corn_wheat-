from datetime import date, datetime
from typing import Any
from uuid import NAMESPACE_URL, uuid5

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from app.dependencies import require_user
from app.services.supabase import supabase

router = APIRouter(prefix="/api", tags=["Sync"])


class Payload(BaseModel):
    data: dict[str, Any]


def stable_uuid(user_id: str, kind: str, local_id: Any) -> str:
    return str(uuid5(NAMESPACE_URL, f"farmifyai:{user_id}:{kind}:{local_id}"))


def normalized_date(value: Any) -> str:
    if isinstance(value, str):
        # Accept Android's "dd MMM yyyy" and ISO dates.
        for fmt in ("%d %b %Y", "%Y-%m-%d"):
            try:
                return datetime.strptime(value, fmt).date().isoformat()
            except (ValueError, AttributeError):
                pass
    return date.today().isoformat()


@router.post("/sync/profile")
async def profile(x: Payload, user: dict = Depends(require_user)):
    data = dict(x.data)
    data["id"] = user["id"]
    data.pop("full_name", None)
    data["name"] = data.get("name") or "Pakistani Kisan"

    code, text = await supabase.upsert("profiles", data)
    if code >= 400:
        raise HTTPException(code, text)
    return {"success": True}


@router.post("/sync/khata")
async def khata(x: Payload, user: dict = Depends(require_user)):
    source = dict(x.data)
    entry_type = str(source.get("type", "expense")).lower()
    if entry_type not in {"income", "expense"}:
        # FIELD_WORK is a local-only activity; store it as an expense transaction.
        entry_type = "expense"

    payload = {
        "id": stable_uuid(user["id"], "khata", source.get("local_id", "unknown")),
        "user_id": user["id"],
        "type": entry_type,
        "category": str(source.get("category") or entry_type).lower(),
        "crop_name": source.get("crop_name") or "General",
        "amount": max(float(source.get("amount") or 0), 0),
        "quantity": source.get("quantity"),
        "unit": source.get("unit") or "Mann",
        "field_name": source.get("field_name") or "Main Field",
        "buyer_or_mandi": source.get("buyer_or_mandi"),
        "description": source.get("description"),
        "transaction_date": normalized_date(source.get("transaction_date")),
    }

    code, text = await supabase.upsert("khata_transactions", payload)
    if code >= 400:
        raise HTTPException(code, text)
    return {"success": True}


@router.post("/sync/disease")
async def disease(x: Payload, user: dict = Depends(require_user)):
    source = dict(x.data)
    payload = {
        "id": stable_uuid(user["id"], "disease", source.get("local_id", "unknown")),
        "user_id": user["id"],
        "crop_name": source.get("crop_name"),
        "disease_name": source.get("disease_name"),
        "disease_name_ur": source.get("disease_name_ur"),
        "confidence": source.get("confidence"),
        "severity": source.get("severity"),
        "symptoms": source.get("symptoms"),
        "treatment_chemical": source.get("treatment_chemical"),
        "treatment_organic": source.get("treatment_organic"),
        "recommendation": source.get("recommendation"),
    }

    code, text = await supabase.upsert("disease_detections", payload)
    if code >= 400:
        raise HTTPException(code, text)
    return {"success": True}


@router.get("/mandi/rates")
async def mandi_rates():
    code, data = await supabase.select(
        "mandi_rates",
        params={"select": "*", "order": "city.asc,crop_name_en.asc"},
    )
    if code >= 400:
        raise HTTPException(code, str(data))
    return {"success": True, "rates": data}
