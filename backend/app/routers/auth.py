from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.supabase import supabase
router=APIRouter(prefix="/api/auth",tags=["Authentication"])
class Signup(BaseModel): email: str; password: str; metadata: dict={}
class Login(BaseModel): email: str; password: str
class Recover(BaseModel): email: str
@router.post('/signup')
async def signup(x:Signup):
    code,data=await supabase.signup({"email":x.email,"password":x.password,"data":x.metadata})
    if code>=400: raise HTTPException(code,data.get('msg') or data.get('message') or 'Signup failed')
    return {"success":True,"user":data.get('user'),"access_token":data.get('access_token')}
@router.post('/guest')
async def guest():
    # Requires Supabase Anonymous Sign-Ins to be enabled. Every guest gets a unique auth user_id.
    code,data=await supabase.anonymous_signup()
    if code>=400: raise HTTPException(code,data.get('msg') or data.get('message') or 'Guest sign-in failed')
    return {"success":True,"user":data.get('user'),"access_token":data.get('access_token'),"refresh_token":data.get('refresh_token')}

@router.post('/login')
async def login(x:Login):
    code,data=await supabase.login(x.email,x.password)
    if code>=400: raise HTTPException(code,data.get('msg') or data.get('message') or 'Login failed')
    return {"success":True,"user":data.get('user'),"access_token":data.get('access_token'),"refresh_token":data.get('refresh_token')}
@router.post('/forgot-password')
async def recover(x:Recover):
    code,data=await supabase.recover(x.email)
    if code>=400: raise HTTPException(code,data.get('msg') or data.get('message') or 'Recovery failed')
    return {"success":True,"message":"Password recovery request accepted"}
