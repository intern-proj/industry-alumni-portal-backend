import logging
import uuid
from datetime import datetime
from sqlalchemy.orm import Session
from app.models import AiModelConfig

logger = logging.getLogger("ai_service.seed_models")

DEFAULT_MODEL_PRESETS = [
    {
        "id": "preset-gemini-35-flash",
        "config_name": "Google Gemini 3.5 Flash (Cloud API - Active)",
        "provider": "GEMINI_API",
        "model_name": "gemini-3.5-flash",
        "gpu_layers": 0,
        "context_size": 8192,
        "threads": 4,
        "temperature": 0.2,
        "azure_endpoint": "https://generativelanguage.googleapis.com/v1beta",
        "is_active": True
    },
    {
        "id": "preset-llama3-8b-cpu",
        "config_name": "Meta Llama 3.1 8B Instruct (Quantized CPU)",
        "provider": "LOCAL_GGUF",
        "model_name": "Meta Llama 3.1 8B Instruct",
        "repo_id": "bartowski/Meta-Llama-3.1-8B-Instruct-GGUF",
        "filename": "Meta-Llama-3.1-8B-Instruct-Q3_K_M.gguf",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "is_active": False
    },
    {
        "id": "preset-qwen3-4b-cpu",
        "config_name": "Qwen 3 4B Instruct (Fast CPU)",
        "provider": "LOCAL_GGUF",
        "model_name": "Qwen 3 4B Instruct",
        "repo_id": "lmstudio-community/Qwen3-4B-Instruct-2507-GGUF",
        "filename": "Qwen3-4B-Instruct-2507-Q4_K_M.gguf",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "is_active": False
    },
    {
        "id": "preset-qwen25-7b-cpu",
        "config_name": "Qwen 2.5 7B Instruct (Balanced CPU)",
        "provider": "LOCAL_GGUF",
        "model_name": "Qwen 2.5 7B Instruct",
        "repo_id": "Qwen/Qwen2.5-7B-Instruct-GGUF",
        "filename": "qwen2.5-7b-instruct-q3_k_m.gguf",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "is_active": False
    },
    {
        "id": "preset-azure-openai-4o-mini",
        "config_name": "Azure OpenAI GPT-4o Mini (Enterprise Cloud)",
        "provider": "AZURE_OPENAI",
        "model_name": "gpt-4o-mini",
        "gpu_layers": 0,
        "context_size": 8192,
        "threads": 4,
        "temperature": 0.2,
        "azure_endpoint": "https://your-resource-name.openai.azure.com/",
        "azure_deployment_name": "gpt-4o-mini",
        "is_active": False
    },
    {
        "id": "preset-openai-compatible-vllm",
        "config_name": "Self-Hosted vLLM / Ollama Endpoint",
        "provider": "OPENAI_COMPATIBLE",
        "model_name": "llama-3.1-8b",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "azure_endpoint": "http://localhost:11434/v1",
        "is_active": False
    }
]


def seed_ai_model_presets(db: Session):
    """Initializes default AI model configurations if none exist in the database."""
    try:
        count = db.query(AiModelConfig).count()
        if count == 0:
            logger.info("[AI Model Seeder] No model configurations found in database. Seeding presets...")
            for preset in DEFAULT_MODEL_PRESETS:
                config = AiModelConfig(
                    id=preset.get("id", str(uuid.uuid4())),
                    config_name=preset["config_name"],
                    provider=preset["provider"],
                    model_name=preset["model_name"],
                    repo_id=preset.get("repo_id"),
                    filename=preset.get("filename"),
                    gpu_layers=preset.get("gpu_layers", 0),
                    context_size=preset.get("context_size", 4096),
                    threads=preset.get("threads", 4),
                    temperature=preset.get("temperature", 0.2),
                    azure_endpoint=preset.get("azure_endpoint"),
                    azure_deployment_name=preset.get("azure_deployment_name"),
                    is_active=preset.get("is_active", False),
                    created_at=datetime.utcnow(),
                    updated_at=datetime.utcnow()
                )
                db.add(config)
            db.commit()
            logger.info(f"[AI Model Seeder] Successfully seeded {len(DEFAULT_MODEL_PRESETS)} AI model presets.")
        else:
            logger.info(f"[AI Model Seeder] Found {count} existing model configurations. Skipping preset seeding.")
    except Exception as e:
        logger.warning(f"[AI Model Seeder] Could not seed AI model presets: {e}")
        db.rollback()
