import logging
import traceback

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.routers import auth, sync, chat, data, work

app = FastAPI(title="FarmifyAI Backend", version="3.0.0")

origins = [origin.strip() for origin in settings.allowed_origins.split(",") if origin.strip()]
if not origins:
    origins = ["*"]  # Configure ALLOWED_ORIGINS in production

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=False,
    # The API uses PUT and DELETE as well; without them a browser client is
    # blocked at the preflight check.
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "Accept"],
)

app.include_router(auth.router)
app.include_router(sync.router)
app.include_router(chat.router)
app.include_router(data.router)
app.include_router(work.router)

logger = logging.getLogger("farmify")


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    """Return the failure instead of an empty 500.

    An unhandled exception otherwise reaches the client as a bare
    "Internal Server Error" with no body, which says nothing about whether the
    cause was configuration, connectivity or a genuine bug.
    """
    logger.error("Unhandled error on %s: %s", request.url.path, traceback.format_exc())
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error": type(exc).__name__,
            "detail": str(exc) or "Unhandled server error",
            "path": request.url.path,
        },
    )


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
