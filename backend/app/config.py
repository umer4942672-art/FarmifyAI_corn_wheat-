from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    supabase_url: str = ""
    supabase_anon_key: str = ""
    supabase_service_role_key: str = ""

    # Google Gemini (server-side only)
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"
    gemini_timeout_seconds: float = 60.0
    gemini_max_output_tokens: int = 1200

    chat_temperature: float = 0.2
    rag_top_k: int = 5
    allowed_origins: str = ""
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
