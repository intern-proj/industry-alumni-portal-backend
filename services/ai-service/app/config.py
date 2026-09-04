import os
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "Recruitment AI Microservice"
    ENVIRONMENT: str = "development"
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    BACKEND_API_BASE_URL: str = "http://localhost:8080/api/v1"


    # PostgreSQL Database Settings
    DB_USER: str = "user"
    DB_PASSWORD: str = "root"
    DB_HOST: str = "localhost"
    DB_PORT: int = 5432
    DB_NAME: str = "ai_service_db"
    DATABASE_URL: str = "postgresql+psycopg2://user:root@localhost:5432/ai_service_db"

    # Eureka Service Discovery Settings
    EUREKA_SERVER_URL: str = "http://localhost:8761/eureka/"
    EUREKA_APP_NAME: str = "ai-service"
    EUREKA_INSTANCE_HOST: str = "localhost"
    EUREKA_ENABLED: bool = True

    # RabbitMQ Settings
    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USERNAME: str = "guest"
    RABBITMQ_PASSWORD: str = "guest"
    RABBITMQ_EXCHANGE: str = "vacancy.exchange"
    RABBITMQ_QUEUE: str = "vacancy.ai.queue"
    RABBITMQ_ROUTING_KEY: str = "vacancy.flyer.process"
    RABBITMQ_ENABLED: bool = True

    # LLM Settings
    LLM_REPO_ID: str = "lmstudio-community/Qwen3-4B-Instruct-2507-GGUF"
    LLM_FILENAME: str = "Qwen3-4B-Instruct-2507-Q4_K_M.gguf"
    LLM_THREADS: int = 4
    LLM_CONTEXT_SIZE: int = 4096

    def get_database_url(self) -> str:
        if self.DATABASE_URL and not self.DATABASE_URL.startswith("sqlite"):
            return self.DATABASE_URL
        return f"postgresql+psycopg2://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()
