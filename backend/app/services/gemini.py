import httpx
from app.config import settings


class GeminiService:
    @property
    def configured(self) -> bool:
        return bool(settings.gemini_api_key and settings.gemini_model)

    async def chat(self, system_prompt: str, messages: list[dict[str, str]]) -> str:
        if not self.configured:
            raise RuntimeError("GEMINI_API_KEY is missing")
        contents = []
        for m in messages:
            role = "model" if m.get("role") == "assistant" else "user"
            contents.append({"role": role, "parts": [{"text": m.get("content", "")} ]})
        payload = {
            "systemInstruction": {"parts": [{"text": system_prompt}]},
            "contents": contents,
            "generationConfig": {
                "temperature": settings.chat_temperature,
                "maxOutputTokens": settings.gemini_max_output_tokens,
            },
        }
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{settings.gemini_model}:generateContent"
        # The key goes in the x-goog-api-key header, not the ?key= query string.
        # Google has migrated from AIza "traffic keys" to AQ. "authentication keys",
        # and the header is the documented way to pass either kind. It also keeps
        # the key out of URLs, which end up in proxy and access logs.
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": settings.gemini_api_key,
        }
        async with httpx.AsyncClient(timeout=settings.gemini_timeout_seconds) as client:
            response = await client.post(url, json=payload, headers=headers)
            if response.status_code >= 400:
                raise RuntimeError(f"Gemini API {response.status_code}: {response.text[:500]}")
            data = response.json()
        try:
            text = "".join(p.get("text", "") for p in data["candidates"][0]["content"]["parts"]).strip()
        except (KeyError, IndexError, TypeError) as exc:
            raise RuntimeError("Gemini returned an empty or invalid response") from exc
        if not text:
            raise RuntimeError("Gemini returned an empty response")
        return text


gemini = GeminiService()
