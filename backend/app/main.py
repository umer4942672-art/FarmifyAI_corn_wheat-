from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.routers import auth, sync, chat, data

app = FastAPI(title="FarmifyAI Backend", version="3.0.0")

origins = [origin.strip() for origin in settings.allowed_origins.split(",") if origin.strip()]
if not origins:
    origins = ["*"]  # Configure ALLOWED_ORIGINS in production

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "Accept"],
)

app.include_router(auth.router)
app.include_router(sync.router)
app.include_router(chat.router)
app.include_router(data.router)


@app.get("/")
def root():
    return {"service": "FarmifyAI Backend", "status": "running"}


@app.get("/health")
def health():
    from app.services.gemini import gemini
    from app.services.supabase import supabase
    chatbot_ok = gemini.configured
    return {
        "status": "healthy" if supabase.configured and chatbot_ok else "degraded",
        "supabase_configured": supabase.configured,
        "gemini_chatbot_configured": chatbot_ok,
        "chat_model": settings.gemini_model,
    }
