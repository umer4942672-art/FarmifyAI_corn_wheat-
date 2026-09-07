from fastapi import Header, HTTPException
from app.services.supabase import supabase


async def require_user(authorization: str | None = Header(default=None)) -> dict:
    """Validate the Supabase access token and return the authenticated user."""
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Authentication required")

    token = authorization.split(" ", 1)[1].strip()
    if not token:
        raise HTTPException(status_code=401, detail="Authentication required")

    code, user = await supabase.get_user(token)
    if code >= 400 or not user.get("id"):
        raise HTTPException(status_code=401, detail="Invalid or expired access token")

    return user
