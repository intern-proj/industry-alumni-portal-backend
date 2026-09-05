import os
import urllib.parse
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "Recruitment AI Microservice"
    ENVIRONMENT: str = "development"
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    BACKEND_API_BASE_URL: str = os.getenv(
        "BACKEND_API_BASE_URL",
        os.getenv(
            "API_GATEWAY_URL",
            "https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io/api/v1"
            if "azurecontainerapps.io" in os.getenv("EUREKA_SERVER_URL", "")
            else "http://localhost:8080/api/v1"
        )
    ).rstrip("/")
    VACANCY_SERVICE_BASE_URL: str = os.getenv(
        "VACANCY_SERVICE_BASE_URL",
        "https://vacancy-service.internal.happybush-76206934.centralindia.azurecontainerapps.io/api/v1/vacancies/partner"
        if "azurecontainerapps.io" in os.getenv("EUREKA_SERVER_URL", "")
        else "http://localhost:8087/api/v1/vacancies/partner"
    ).rstrip("/")
    APPLICATION_SERVICE_BASE_URL: str = os.getenv(
        "APPLICATION_SERVICE_BASE_URL",
        "https://application-service.internal.happybush-76206934.centralindia.azurecontainerapps.io/api/v1/applications"
        if "azurecontainerapps.io" in os.getenv("EUREKA_SERVER_URL", "")
        else "http://localhost:8084/api/v1/applications"
    ).rstrip("/")

    # PostgreSQL Database Settings
    PGUSER: str = "pguser"
    PGPASSWORD: str = "NicDB@123"
    PGHOST: str = "nicdbpgs.postgres.database.azure.com"
    PGPORT: int = 5432
    PGDATABASE: str = "ai_service_db"
    DATABASE_URL: str = ""

    # Eureka Service Discovery Settings
    EUREKA_SERVER_URL: str = "http://localhost:8761/eureka/"
    EUREKA_APP_NAME: str = "ai-service"
    EUREKA_INSTANCE_HOST: str = ""
    EUREKA_ENABLED: bool = True

    # RabbitMQ Settings
    RABBITMQ_HOST: str = "toucan.lmq.cloudamqp.com"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USERNAME: str = "nnmhsfhw"
    RABBITMQ_PASSWORD: str = "cIpl_gm0K2QxsPS-bp51PHpD2aojyuS7"
    RABBITMQ_VIRTUAL_HOST: str = "nnmhsfhw"
    RABBITMQ_EXCHANGE: str = "vacancy.exchange"
    RABBITMQ_QUEUE: str = "vacancy.ai.queue"
    RABBITMQ_ROUTING_KEY: str = "vacancy.flyer.process"
    RABBITMQ_ENABLED: bool = True

    # Gemini Cloud LLM API Settings (Preferred for serverless / no-GPU hosting)
    USE_GEMINI_API: bool = True
    GEMINI_API_KEY: str = "AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg"
    GEMINI_MODEL: str = "gemini-3.1-flash-lite"

    # Local LLM Settings (Disabled when USE_GEMINI_API is True)
    LLM_REPO_ID: str = "lmstudio-community/Qwen3-4B-Instruct-2507-GGUF"
    LLM_FILENAME: str = "Qwen3-4B-Instruct-2507-Q4_K_M.gguf"
    LLM_THREADS: int = 4
    LLM_CONTEXT_SIZE: int = 4096

    def get_database_url(self) -> str:
        if self.DATABASE_URL and not self.DATABASE_URL.startswith("sqlite"):
            return self.DATABASE_URL
        
        encoded_password = urllib.parse.quote_plus(self.PGPASSWORD)
        return f"postgresql+psycopg2://{self.PGUSER}:{encoded_password}@{self.PGHOST}:{self.PGPORT}/{self.PGDATABASE}?sslmode=require"

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()
