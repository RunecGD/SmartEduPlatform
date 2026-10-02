from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    db_url: str
    ai_service_token: SecretStr = Field(min_length=32)
    openrouter_api_key: SecretStr
    openrouter_model: str = "openai/gpt-oss-20b"
    ollama_url: str = "http://ollama:11434"
    embed_model: str = "nomic-embed-text"
    embedding_dimensions: int = Field(default=768, ge=1, le=16384)
    minio_url: str = "http://minio:9000"
    minio_access_key: str
    minio_secret_key: SecretStr
    minio_bucket: str = "smartedu-materials"
    max_file_bytes: int = 25 * 1024 * 1024
    max_text_chars: int = 2_000_000
    max_pdf_pages: int = 1000

settings = Settings()
