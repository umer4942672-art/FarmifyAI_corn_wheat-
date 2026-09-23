"""
Vercel entry point.

The project is a normal FastAPI application under `app/`; this module only
exposes it where Vercel's Python runtime expects to find it. Keeping the entry
point separate means the application can still be run locally with
`uvicorn app.main:app` without any Vercel-specific wiring.
"""
import os
import sys

# The function is executed from the `api/` directory, so the project root has to
# be importable for `app.*` to resolve.
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.main import app  # noqa: E402

# Vercel's Python runtime looks for a module-level ASGI callable named `app`.
__all__ = ["app"]
