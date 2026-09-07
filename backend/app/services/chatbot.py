from app.services.gemini import gemini
from app.services.rag import rag
from app.services.supabase import supabase

SYSTEM_PROMPT = """You are FarmifyAI's Kisan Dost AI, a practical agriculture assistant for Pakistani farmers.
Rules: answer directly and simply; answer Urdu script when language=ur, otherwise English.
Use supplied FarmifyAI knowledge as baseline. Never invent current mandi prices, weather, pesticide doses, registrations, schemes, or disease facts.
For chemical doses, advise following the registered product label and local agriculture expert if not verified.
Keep answers concise, safe and actionable.
""".strip()

class CustomAgricultureChatbot:
    async def answer(self, user_id: str, message: str, language: str, history: list[dict]) -> dict:
        documents = await rag.retrieve(message)
        context = rag.format_context(documents)
        recent_history = []
        for item in history[-6:]:
            role = "assistant" if item.get("role") in {"assistant", "model"} else "user"
            text = str(item.get("text", "")).strip()
            if text: recent_history.append({"role": role, "content": text[:4000]})
        prompt = f"language={language}\n\nVerified FarmifyAI knowledge:\n{context}\n\nFarmer question:\n{message}"
        answer = await gemini.chat(SYSTEM_PROMPT, [*recent_history, {"role": "user", "content": prompt}])
        try:
            await self._save_message(user_id, "user", message, language)
            await self._save_message(user_id, "assistant", answer, language)
        except Exception:
            pass
        return {"answer": answer, "rag_used": bool(documents), "sources": [{"title": d.get("title"), "crop": d.get("crop"), "score": d.get("similarity")} for d in documents]}

    async def _save_message(self, user_id, role, content, language):
        await supabase.insert("chat_messages", {"user_id": user_id, "role": role, "content": content, "language": language})

chatbot = CustomAgricultureChatbot()
