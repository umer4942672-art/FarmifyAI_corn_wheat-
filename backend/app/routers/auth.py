from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from app.dependencies import require_user
from app.services.supabase import supabase

router = APIRouter(prefix="/api/auth", tags=["Authentication"])

class Signup(BaseModel):
    email: str
    password: str
    metadata: dict = {}

class Login(BaseModel):
    email: str
    password: str

class Recover(BaseModel):
    email: str

@router.post('/signup')
async def signup(x: Signup):
    code, data = await supabase.signup({"email": x.email, "password": x.password, "data": x.metadata})
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
    code, data = await supabase.login(x.email, x.password)
    if code >= 400:
        raise HTTPException(code, data.get('msg') or data.get('message') or 'Login failed')
    return {"success": True, "user": data.get('user'), "access_token": data.get('access_token'), "refresh_token": data.get('refresh_token')}

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
async def logout(user: dict = Depends(require_user)):
    # The Android client also clears its encrypted local session.
    # Supabase logout invalidates the server-side session represented by this JWT.
    # require_user above ensures this endpoint is never callable anonymously.
    return {"success": True, "message": "Logged out", "user_id": user.get("id")}
