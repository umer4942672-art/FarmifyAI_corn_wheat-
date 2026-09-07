from datetime import date
from typing import Any
from uuid import NAMESPACE_URL, uuid5

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from pydantic import BaseModel

from app.dependencies import require_user
from app.services.supabase import supabase

router = APIRouter(prefix="/api", tags=["Cloud Data"])

def uid(user_id: str, kind: str, local_id: Any) -> str:
    return str(uuid5(NAMESPACE_URL, f"farmifyai:{user_id}:{kind}:{local_id}"))

class CropIn(BaseModel):
    local_id: str | None = None
    crop_name: str
    variety: str | None = None
    area: float | None = None
    area_unit: str = "Acres"
    sowing_date: str | None = None
    expected_harvest_date: str | None = None
    status: str = "Active"
    notes: str | None = None

@router.get('/crops')
async def list_crops(user: dict = Depends(require_user)):
    code, data = await supabase.select('user_crops', {'select':'*','user_id':f'eq.{user["id"]}','order':'created_at.desc'})
    if code >= 400: raise HTTPException(code, str(data))
    return {'success':True,'crops':data}

@router.post('/crops')
async def create_crop(x: CropIn, user: dict = Depends(require_user)):
    payload=x.model_dump()
    payload['id']=uid(user['id'],'crop',payload.pop('local_id') or payload['crop_name']+str(date.today()))
    payload['user_id']=user['id']
    code, text=await supabase.upsert('user_crops',payload)
    if code >= 400: raise HTTPException(code,text)
    return {'success':True,'id':payload['id']}

@router.put('/crops/{crop_id}')
async def update_crop(crop_id:str,x:CropIn,user:dict=Depends(require_user)):
    # Never upsert here. An upsert on a client-supplied id would let a caller
    # overwrite another user's row and reassign its user_id to themselves.
    # A PATCH scoped by BOTH id and user_id can only ever touch the caller's own row.
    payload=x.model_dump(exclude={'local_id'})
    payload.pop('id',None)
    payload.pop('user_id',None)
    code,rows=await supabase.update(
        'user_crops',
        {'id':f'eq.{crop_id}','user_id':f'eq.{user["id"]}'},
        payload,
    )
    if code>=400: raise HTTPException(code,str(rows))
    if not rows:
        # Either the crop does not exist or it belongs to somebody else.
        # Both cases return 404 so the endpoint cannot be used to probe for ids.
        raise HTTPException(404,'Crop not found')
    return {'success':True,'id':crop_id}

@router.delete('/crops/{crop_id}')
async def delete_crop(crop_id:str,user:dict=Depends(require_user)):
    code,text=await supabase.delete('user_crops',{'id':f'eq.{crop_id}','user_id':f'eq.{user["id"]}'})
    if code>=400: raise HTTPException(code,text)
    return {'success':True}

@router.post('/disease-images/upload')
async def upload_disease_image(
    file: UploadFile = File(...),
    local_id: str = Form(...),
    crop_name: str = Form('Unknown'),
    user: dict = Depends(require_user),
):
    if not (file.content_type or '').startswith('image/'):
        raise HTTPException(400,'Only image uploads are allowed')
    content=await file.read()
    if len(content)>10*1024*1024: raise HTTPException(413,'Image exceeds 10 MB')
    suffix=(file.filename or 'scan.jpg').split('.')[-1].lower()
    object_path=f'{user["id"]}/{uid(user["id"],"image",local_id)}.{suffix}'
    code,data=await supabase.upload_storage('disease-images',object_path,content,file.content_type or 'image/jpeg')
    if code>=400: raise HTTPException(code,str(data))
    scan_id=uid(user['id'],'disease',local_id)
    await supabase.upsert('disease_detections',{'id':scan_id,'user_id':user['id'],'crop_name':crop_name,'image_url':object_path})
    return {'success':True,'path':object_path,'detection_id':scan_id}

@router.get('/disease-images/signed-url')
async def disease_image_url(path:str,user:dict=Depends(require_user)):
    if not path.startswith(user['id']+'/'): raise HTTPException(403,'Image does not belong to current user')
    code,data=await supabase.signed_url('disease-images',path)
    if code>=400: raise HTTPException(code,str(data))
    return {'success':True,'url':data.get('url')}

@router.get('/sync/bootstrap')
async def bootstrap(user:dict=Depends(require_user)):
    user_id=user['id']
    async def get(table, order='created_at.desc'):
        code,data=await supabase.select(table,{'select':'*','user_id':f'eq.{user_id}','order':order})
        if code>=400: raise HTTPException(code,str(data))
        return data
    code, profile=await supabase.select('profiles',{'select':'*','id':f'eq.{user_id}','limit':'1'})
    if code>=400: raise HTTPException(code,str(profile))
    return {'success':True,'profile': profile[0] if profile else None,'crops':await get('user_crops'),'khata':await get('khata_transactions','transaction_date.desc'),'disease_history':await get('disease_detections')}
