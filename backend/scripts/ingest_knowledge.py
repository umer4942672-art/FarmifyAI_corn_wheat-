import asyncio
import json
from pathlib import Path

from app.services.supabase import supabase


async def main():
    if not supabase.configured:
        raise SystemExit("Set Supabase credentials in backend/.env first")

    source = Path(__file__).resolve().parents[1] / "knowledge" / "agriculture_knowledge.json"
    documents = json.loads(source.read_text(encoding="utf-8"))
    for doc in documents:
        payload = {**doc, "embedding": None}
        code, data = await supabase.upsert("agriculture_documents", payload)
        if code >= 400:
            raise RuntimeError(f"Failed to ingest {doc['slug']}: {data}")
        print(f"Ingested: {doc['slug']}")


if __name__ == "__main__":
    asyncio.run(main())
