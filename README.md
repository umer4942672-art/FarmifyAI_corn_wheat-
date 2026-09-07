# FarmifyAI

FarmifyAI is a production-oriented Android agriculture assistant for Pakistani farmers. It combines offline TensorFlow Lite plant disease detection with a secure FastAPI backend, Supabase persistence, and Google Gemini-powered agricultural chat.

## Architecture
- **Android:** Kotlin + Jetpack Compose + MVVM
- **Backend:** FastAPI deployed as a serverless API
- **Authentication & Database:** Supabase Auth + PostgreSQL + RLS
- **AI Chat:** Google Gemini, called only from backend
- **Disease Detection:** Bundled TensorFlow Lite models
- **Persistence:** Profiles, crops, khata records, disease history/images and chat history

## Security Principles
1. No AI API key is stored in the Android app.
2. Backend validates Supabase JWT before user-scoped operations.
3. Supabase RLS restricts private records to their owner.
4. Disease images should be stored in private user folders.
5. Service-role credentials are backend-only.

## Setup
1. Create a Supabase project and run `supabase/schema.sql`.
2. Copy `backend/.env.example` to `backend/.env` and set Supabase + Gemini secrets.
3. Install backend dependencies: `pip install -r requirements.txt`.
4. Run locally: `uvicorn app.main:app --reload` from `backend/`.
5. Set Android `backendBaseUrl` before building.

## Environment
Required backend secrets: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `GEMINI_API_KEY`.

## Repository Layout
`app/` Android client · `backend/` secure API · `supabase/` database schema · `assets/model/` source AI models.

## Production Checklist
- Restrict `ALLOWED_ORIGINS`.
- Never commit `.env`.
- Keep Supabase service-role key server-side only.
- Replace demo mandi seed data with a verified live data provider before production claims.
- Test RLS with multiple accounts.


## Cloud-first data flow
All Android cloud operations use the Vercel API only: `Auth -> Vercel -> Supabase Auth`, `Crops CRUD -> Vercel -> user_crops`, `Khata -> Vercel -> khata_transactions`, `Disease history/images -> Vercel -> private Supabase Storage`, and `Chat -> Vercel -> Gemini + Supabase history`. The backend also exposes `GET /api/sync/bootstrap` for restoring profile, crops, khata and disease history after login.
