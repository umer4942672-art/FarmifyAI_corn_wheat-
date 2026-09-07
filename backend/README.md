# FarmifyAI Backend

Secure FastAPI backend for FarmifyAI. It validates Supabase JWTs, synchronizes farmer data, retrieves agriculture knowledge, and calls Google Gemini server-side.

## Run
```bash
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload
```

Set Supabase and Gemini secrets in `.env`. Never expose service-role or Gemini keys to the Android client.
