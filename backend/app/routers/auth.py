from fastapi import APIRouter, Depends, Header, HTTPException
from pydantic import BaseModel

from app.dependencies import require_user
from app.services.supabase import supabase

router = APIRouter(prefix="/api/auth", tags=["Authentication"])

class Signup(BaseModel):
    email: str | None = None
    phone: str | None = None
    password: str
    metadata: dict = {}

class Login(BaseModel):
    email: str | None = None
    phone: str | None = None
    password: str

class Recover(BaseModel):
    email: str

class Refresh(BaseModel):
    refresh_token: str

@router.post('/signup')
async def signup(x: Signup):
    if not x.email and not x.phone:
        raise HTTPException(422, "Provide either email or phone")
    payload = {"password": x.password, "data": x.metadata}
    if x.phone:
        payload["phone"] = x.phone
    else:
        payload["email"] = x.email
    code, data = await supabase.signup(payload)
    if code >= 400:
        raise HTTPException(code, data.get('msg') or data.get('message') or 'Signup failed')
    return {"success": True, "user": data.get('user'), "access_token": data.get('access_token'), "refresh_token": data.get('refresh_token')}

@router.post('/guest')
async def guest():
    code, data = await supabase.anonymous_signup()
    if code >= 400:
        raise HTTPException(code, data.get('msg') or data.get('message') or 'Guest sign-in failed')
    return {"success": True, "user": data.get('user'), "access_token": data.get('access_token'), "refresh_token": data.get('refresh_token')}

@router.post('/login')
async def login(x: Login):
    if not x.email and not x.phone:
        raise HTTPException(422, "Provide either email or phone")
    code, data = await supabase.login(x.phone or x.email, x.password, is_phone=bool(x.phone))
    if code >= 400:
        raise HTTPException(code, data.get('msg') or data.get('message') or 'Login failed')
    return {"success": True, "user": data.get('user'), "access_token": data.get('access_token'), "refresh_token": data.get('refresh_token')}

@router.post('/refresh')
async def refresh(x: Refresh):
    """Rotate an expiring Supabase access token. Called by the Android client on 401."""
    token = (x.refresh_token or "").strip()
    if not token:
        raise HTTPException(422, "refresh_token is required")
    code, data = await supabase.refresh(token)
    if code >= 400:
        raise HTTPException(401, data.get('msg') or data.get('message') or 'Refresh token is invalid or expired')
    return {
        "success": True,
        "user": data.get('user'),
        "access_token": data.get('access_token'),
        "refresh_token": data.get('refresh_token'),
        "expires_in": data.get('expires_in'),
    }

@router.post('/forgot-password')
async def recover(x: Recover):
    code, data = await supabase.recover(x.email)
    if code >= 400:
        raise HTTPException(code, data.get('msg') or data.get('message') or 'Recovery failed')
    return {"success": True, "message": "Password recovery request accepted"}

@router.get('/me')
async def me(user: dict = Depends(require_user)):
    return {"success": True, "user": user}

@router.post('/logout')
async def logout(authorization: str | None = Header(default=None), user: dict = Depends(require_user)):
    # require_user above ensures this endpoint is never callable anonymously and
    # guarantees the header is a well-formed bearer token.
    access_token = authorization.split(" ", 1)[1].strip()
    code, _ = await supabase.sign_out(access_token)
    # 401/403 here just means the session was already gone; the client clears
    # its encrypted local session either way.
    revoked = code < 400
    return {"success": True, "message": "Logged out", "session_revoked": revoked, "user_id": user.get("id")}
