from typing import Any

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from app.dependencies import require_user
from app.services.chatbot import chatbot
from app.services.gemini import gemini
from app.services.supabase import supabase

router = APIRouter(prefix="/api/chat", tags=["Gemini Agriculture Chatbot"])


class HistoryItem(BaseModel):
    role: str
    text: str


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)
    language: str = Field(default="en", max_length=5)
    history: list[HistoryItem] = Field(default_factory=list, max_length=12)


@router.post("")
async def chat(payload: ChatRequest, user: dict = Depends(require_user)) -> dict[str, Any]:
    if not supabase.configured:
        raise HTTPException(status_code=503, detail="Supabase is not configured")
    if not gemini.configured:
        raise HTTPException(status_code=503, detail="Gemini chatbot is not configured. Set GEMINI_API_KEY.")

    try:
        result = await chatbot.answer(
            user_id=user["id"],
            message=payload.message.strip(),
            language="ur" if payload.language.lower().startswith("ur") else "en",
            history=[item.model_dump() for item in payload.history],
        )
        return {"success": True, **result}
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"Gemini agriculture chatbot unavailable: {exc}") from exc
