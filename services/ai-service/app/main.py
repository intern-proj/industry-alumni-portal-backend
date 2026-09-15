import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.database import Base, engine
from app.eureka_client import EurekaManager
from app.rabbitmq_consumer import consumer_instance
from app.routers.career_advisor import router as career_advisor_router
from app.routers.resume_matching import router as resume_matching_router
from app.routers.smart_search import router as smart_search_router
from app.routers.vacancies import router as vacancy_router
from app.routers.ai_model_router import router as ai_model_router
from app.services.embedding_engine import EmbeddingEngine
from app.services.llm_engine import LLMEngine

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai_service")
logging.getLogger("httpx").setLevel(logging.WARNING)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. Initialize DB tables
    logger.info("Initializing database tables...")
    Base.metadata.create_all(bind=engine)

    # 2. Register with Eureka Server (for microservice discovery)
    await EurekaManager.start()

    # 3. Warm up LLM and Embedding models on startup
    try:
        logger.info("Checking LLM Engine singleton...")
        LLMEngine.get_instance()
    except Exception as e:
        logger.warning(f"Note: LLM engine initialization deferred or skipped: {e}")

    try:
        logger.info("Checking Bi-Encoder & Cross-Encoder models...")
        EmbeddingEngine.get_bi_encoder()
        EmbeddingEngine.get_cross_encoder()
    except Exception as e:
        logger.warning(f"Note: EmbeddingEngine initialization deferred: {e}")

    # 4. Start RabbitMQ consumer for asynchronous flyer processing (after models are ready)
    try:
        logger.info("Starting RabbitMQ consumer...")
        consumer_instance.start()
    except Exception as e:
        logger.warning(f"Note: RabbitMQ consumer startup deferred or failed: {e}")

    yield

    # Shutdown: Stop consumer & Deregister from Eureka
    consumer_instance.stop()
    await EurekaManager.stop()


app = FastAPI(
    title=settings.APP_NAME,
    description="NSBM Industry Collaboration Unit - AI Vacancy Extraction, Smart NLP Search & Career Advisory Microservice",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware for cross-origin frontend requests
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register All API Routers
app.include_router(vacancy_router)
app.include_router(smart_search_router)
app.include_router(career_advisor_router)
app.include_router(resume_matching_router)
app.include_router(ai_model_router, prefix="/api/v1")


@app.get("/", tags=["System"])
def root():
    return {
        "service": settings.APP_NAME,
        "version": "1.0.0",
        "status": "online",
        "endpoints": [
            "/api/v1/vacancies/parse-and-save",
            "/api/v1/vacancies/institutional-check",
            "/api/v1/ai/smart-search/universal",
            "/api/v1/ai/smart-search/vacancies",
            "/api/v1/ai/smart-search/candidates",
            "/api/v1/ai/resume/analyze-and-advise",
            "/api/v1/ai/vacancies/recommend-for-candidate",
            "/api/v1/ai/candidates/recommend-for-vacancy",
            "/api/v1/ai/resume/match-vacancies",
            "/api/v1/ai/resume/match-single-applicant",
            "/api/v1/ai/resume/match-applicants-bulk"
        ],
        "docs_url": "/docs"
    }


@app.get("/health", tags=["System"])
@app.get("/actuator/health", tags=["System"])
@app.get("/api/v1/ai/health", tags=["System"])
def health_check():
    return {
        "status": "UP",
        "model": settings.LLM_FILENAME,
        "environment": settings.ENVIRONMENT
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
