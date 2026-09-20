from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
    )

    db_url: str

    openrouter_api_key: str
    openrouter_model: str = "openai/gpt-oss-20b"

    embed_model: str = "nomic-embed-text"

    minio_url: str = "http://localhost:9000"
    minio_access_key: str
    minio_secret_key: str
    minio_bucket: str = "smartedu-materials"


settings = Settings()