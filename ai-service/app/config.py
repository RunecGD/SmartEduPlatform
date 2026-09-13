from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )

    db_url: str

    ollama_url: str = "http://localhost:11434"
    llm_model: str = "llama3.1:8b"
    embed_model: str = "nomic-embed-text"

    minio_url: str = "http://localhost:9000"
    minio_access_key: str
    minio_secret_key: str
    minio_bucket: str = "smartedu-materials"


settings = Settings()