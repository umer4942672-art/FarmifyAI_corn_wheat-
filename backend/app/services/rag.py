import re
from app.config import settings
from app.services.supabase import supabase


class RagService:
    """Small Supabase-backed agriculture knowledge retriever.

    Gemini handles response generation. Supabase remains the
    app's verified agriculture knowledge store and chat history store.
    For the bundled knowledge base, a lightweight lexical ranker avoids a
    second hosted embedding service and keeps deployment simple.
    """

    async def retrieve(self, query: str) -> list[dict]:
        code, data = await supabase.select(
            "agriculture_documents",
            {"select": "id,slug,title,crop,language,content,metadata", "limit": "500"},
        )
        if code >= 400 or not isinstance(data, list):
            raise RuntimeError(f"Agriculture knowledge retrieval failed: {data}")

        query_tokens = self._tokens(query)
        if not query_tokens:
            return data[: settings.rag_top_k]

        scored = []
        for doc in data:
            haystack = " ".join(
                str(doc.get(key, "")) for key in ("title", "crop", "language", "content")
            ).lower()
            doc_tokens = self._tokens(haystack)
            overlap = len(query_tokens & doc_tokens)
            phrase_bonus = 0.0
            normalized_query = " ".join(query_tokens)
            if normalized_query and normalized_query in haystack:
                phrase_bonus = 2.0
            score = overlap + phrase_bonus
            if score > 0:
                scored.append((score, doc))

        scored.sort(key=lambda item: item[0], reverse=True)
        return [
            {**doc, "similarity": round(min(0.99, score / max(5.0, len(query_tokens))), 3)}
            for score, doc in scored[: settings.rag_top_k]
        ]

    @staticmethod
    def _tokens(text: str) -> set[str]:
        return {
            token
            for token in re.findall(r"[\w\u0600-\u06ff]+", text.lower())
            if len(token) >= 3
        }

    @staticmethod
    def format_context(documents: list[dict]) -> str:
        if not documents:
            return "No verified agriculture knowledge was retrieved for this question."
        blocks = []
        for index, doc in enumerate(documents, start=1):
            blocks.append(
                f"[SOURCE {index}] {doc.get('title', 'Agriculture knowledge')}\n"
                f"Crop: {doc.get('crop', 'General')}\n"
                f"Language: {doc.get('language', 'en')}\n"
                f"Content: {doc.get('content', '')}"
            )
        return "\n\n".join(blocks)


rag = RagService()
