from urllib.parse import quote

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    POSTGRES_USER: str = Field(default=..., description="PostgresSQL username")
    POSTGRES_PASSWORD: str = Field(default=..., description="PostgresSQL password")
    POSTGRES_DB: str = Field(default=..., description="PostgresSQL database name")

    DATABASE_URL: str | None = Field(default=None, description="PostgresSQL connection URL")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=True,
    )

    @property
    def database_url(self) -> str:
        """
        Return `DATABASE_URL`: use override if set, else constructed from credentials in .env.
        """
        if self.DATABASE_URL:
            return self.DATABASE_URL
        password = quote(self.POSTGRES_PASSWORD)
        return f"postgresql://{self.POSTGRES_USER}:{password}@localhost:5433/{self.POSTGRES_DB}"


settings = Settings()
