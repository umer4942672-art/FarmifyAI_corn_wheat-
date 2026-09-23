import asyncio
from contextlib import asynccontextmanager

import httpx

from app.config import settings

# One client per event loop, reused across calls.
#
# Two problems are being solved here.
#
# Creating a fresh AsyncClient for every request opens and tears down a
# connection pool each time. On Vercel's Python runtime that exhausts the
# sandbox's socket handles and the next connect fails before it reaches
# Supabase at all, with "[Errno 16] Device or resource busy".
#
# A single module-level client is not safe either: a serverless invocation may
# run on a different event loop from the one the client was created on, and a
# client bound to a finished loop raises on use. Keying by loop avoids that
# while still reusing connections within an invocation.
#
# The transport retries connection failures, so a transient socket error
# recovers instead of surfacing as a 500.
_clients: dict[int, httpx.AsyncClient] = {}


def _build_client(timeout: float) -> httpx.AsyncClient:
    return httpx.AsyncClient(
        timeout=timeout,
        transport=httpx.AsyncHTTPTransport(retries=3),
        limits=httpx.Limits(max_connections=10, max_keepalive_connections=5),
    )


@asynccontextmanager
async def http_client(timeout: float = 30):
    """Yield a client bound to the running loop. Deliberately not closed here."""
    try:
        key = id(asyncio.get_running_loop())
    except RuntimeError:
        key = 0

    client = _clients.get(key)
    if client is None or client.is_closed:
        client = _build_client(timeout)
        _clients[key] = client

    try:
        yield client
    except httpx.ConnectError:
        # The pooled connection is unusable; drop it so the next call starts clean.
        _clients.pop(key, None)
        await client.aclose()
        raise


class SupabaseService:
    def __init__(self):
        self.base = settings.supabase_url.rstrip("/")

    @property
    def configured(self) -> bool:
        return bool(
            self.base
            and settings.supabase_anon_key
            and settings.supabase_service_role_key
            and self.base.startswith(("http://", "https://"))
        )

    def configuration_error(self):
        return 503, {"message": "Supabase backend is not configured. Set SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY."}

    def headers(self, service: bool = False, access_token: str | None = None):
        key = settings.supabase_service_role_key if service else settings.supabase_anon_key
        headers = {
            "apikey": key,
            "Content-Type": "application/json",
        }
        headers["Authorization"] = f"Bearer {access_token or key}"
        return headers

    async def signup(self, payload):
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/auth/v1/signup",
                headers=self.headers(),
                json=payload,
            )
            return response.status_code, response.json()

    async def anonymous_signup(self):
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.post(f"{self.base}/auth/v1/signup", headers=self.headers(), json={"data": {"guest": True}})
            return response.status_code, response.json() if response.content else {}

    async def login(self, identifier, password, is_phone: bool = False):
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/auth/v1/token?grant_type=password",
                headers=self.headers(),
                json=({"phone": identifier, "password": password} if is_phone else {"email": identifier, "password": password}),
            )
            return response.status_code, response.json()

    async def refresh(self, refresh_token: str):
        """Exchange a refresh token for a new short-lived access token."""
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/auth/v1/token?grant_type=refresh_token",
                headers=self.headers(),
                json={"refresh_token": refresh_token},
            )
            return response.status_code, response.json() if response.content else {}

    async def sign_out(self, access_token: str):
        """Revoke the Supabase session (and its refresh token) for this access token."""
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/auth/v1/logout",
                headers=self.headers(access_token=access_token),
            )
            return response.status_code, response.text

    async def recover(self, email):
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/auth/v1/recover",
                headers=self.headers(),
                json={"email": email},
            )
            return response.status_code, response.json() if response.content else {}

    async def get_user(self, access_token: str):
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.get(
                f"{self.base}/auth/v1/user",
                headers=self.headers(access_token=access_token),
            )
            if response.status_code >= 400:
                return response.status_code, {}
            return response.status_code, response.json()

    async def select(self, table, params=None):
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.get(
                f"{self.base}/rest/v1/{table}",
                headers=self.headers(service=True),
                params=params or {},
            )
            return response.status_code, response.json() if response.content else []

    async def insert(self, table, payload):
        if not self.configured:
            return self.configuration_error()
        headers = self.headers(service=True)
        headers["Prefer"] = "return=minimal"
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/rest/v1/{table}",
                headers=headers,
                json=payload,
            )
            return response.status_code, response.text

    async def insert_returning(self, table, payload):
        """INSERT that returns the created row, for ids the database generates."""
        if not self.configured:
            return self.configuration_error()
        headers = self.headers(service=True)
        headers["Prefer"] = "return=representation"
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/rest/v1/{table}",
                headers=headers,
                json=payload,
            )
            data = []
            if response.content:
                try:
                    data = response.json()
                except ValueError:
                    data = []
            return response.status_code, data

    async def rpc(self, function_name, payload):
        if not self.configured:
            return self.configuration_error()
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/rest/v1/rpc/{function_name}",
                headers=self.headers(service=True),
                json=payload,
            )
            return response.status_code, response.json() if response.content else []

    async def upsert(self, table, payload):
        if not self.configured:
            return self.configuration_error()
        headers = self.headers(service=True)
        headers["Prefer"] = "resolution=merge-duplicates,return=minimal"
        async with http_client(30) as client:
            response = await client.post(
                f"{self.base}/rest/v1/{table}",
                headers=headers,
                json=payload,
            )
            return response.status_code, response.text


    async def update(self, table, params, payload):
        """Ownership-safe UPDATE. `params` must always scope the row to its owner."""
        if not self.configured:
            return self.configuration_error()
        headers = self.headers(service=True)
        headers["Prefer"] = "return=representation"
        async with http_client(30) as client:
            response = await client.patch(
                f"{self.base}/rest/v1/{table}",
                headers=headers,
                params=params,
                json=payload,
            )
            data = []
            if response.content:
                try:
                    data = response.json()
                except ValueError:
                    data = []
            return response.status_code, data

    async def delete(self, table, params):
        if not self.configured: return self.configuration_error()
        async with http_client(30) as client:
            r = await client.delete(f"{self.base}/rest/v1/{table}", headers=self.headers(service=True), params=params)
            return r.status_code, r.text

    async def upload_storage(self, bucket: str, path: str, content: bytes, content_type: str):
        if not self.configured: return self.configuration_error()
        headers = self.headers(service=True)
        headers["Content-Type"] = content_type
        headers["x-upsert"] = "true"
        async with http_client(60) as client:
            r = await client.post(f"{self.base}/storage/v1/object/{bucket}/{path}", headers=headers, content=content)
            return r.status_code, r.json() if r.content else {}

    async def delete_storage(self, bucket: str, path: str):
        if not self.configured: return self.configuration_error()
        async with http_client(30) as client:
            r = await client.delete(f"{self.base}/storage/v1/object/{bucket}/{path}", headers=self.headers(service=True))
            return r.status_code, r.text

    async def signed_url(self, bucket: str, path: str, expires_in: int = 3600):
        if not self.configured: return self.configuration_error()
        async with http_client(30) as client:
            r = await client.post(f"{self.base}/storage/v1/object/sign/{bucket}/{path}", headers=self.headers(service=True), json={"expiresIn": expires_in})
            data = r.json() if r.content else {}
            if r.status_code < 400 and data.get("signedURL"):
                data["url"] = f"{self.base}/storage/v1{data['signedURL']}"
            return r.status_code, data


supabase = SupabaseService()
