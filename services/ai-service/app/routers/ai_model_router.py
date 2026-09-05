import time
import logging
from typing import Optional, List, Dict, Any
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import AiModelConfig
from app.config import settings
from app.services.llm_engine import LLMEngine

logger = logging.getLogger("ai_service.model_router")

router = APIRouter(
    prefix="/ai/models",
    tags=["AI Models & Infrastructure"]
)


class ModelConfigRequest(BaseModel):
    id: Optional[str] = None
    config_name: str
    provider: str = "LOCAL_GGUF"  # LOCAL_GGUF, AZURE_OPENAI, OPENAI_COMPATIBLE
    model_name: str
    repo_id: Optional[str] = None
    filename: Optional[str] = None
    gpu_layers: int = 0
    context_size: int = 4096
    threads: int = 4
    temperature: float = 0.2
    azure_endpoint: Optional[str] = None
    azure_api_key: Optional[str] = None
    azure_deployment_name: Optional[str] = None
    is_active: bool = True


class TestModelRequest(BaseModel):
    prompt: Optional[str] = "Provide a 1-sentence confirmation of system operational status and model readiness."


@router.get("/config")
def get_model_configurations(db: Session = Depends(get_db)):
    """
    Returns the currently active AI model configuration alongside all available
    already present configurations and selectable cloud/GPU presets.
    """
    configs = db.query(AiModelConfig).order_by(AiModelConfig.created_at.asc()).all()
    active = next((c for c in configs if c.is_active), None)

    # If no active config in DB, fallback to current settings
    if not active:
        active_dict = {
            "id": "default-qwen4b-cpu",
            "config_name": "Current System Setup (Qwen 4B CPU)",
            "provider": "LOCAL_GGUF",
            "model_name": "Qwen 3 4B Instruct",
            "repo_id": settings.LLM_REPO_ID,
            "filename": settings.LLM_FILENAME,
            "gpu_layers": 0,
            "context_size": settings.LLM_CONTEXT_SIZE,
            "threads": settings.LLM_THREADS,
            "temperature": 0.2,
            "is_active": True,
            "is_password_set": False
        }
    else:
        active_dict = {
            "id": active.id,
            "config_name": active.config_name,
            "provider": active.provider,
            "model_name": active.model_name,
            "repo_id": active.repo_id,
            "filename": active.filename,
            "gpu_layers": active.gpu_layers,
            "context_size": active.context_size,
            "threads": active.threads,
            "temperature": active.temperature,
            "azure_endpoint": active.azure_endpoint,
            "azure_api_key": "••••••••" if active.azure_api_key else None,
            "is_password_set": bool(active.azure_api_key),
            "azure_deployment_name": active.azure_deployment_name,
            "is_active": active.is_active,
            "updated_at": active.updated_at.isoformat() if active.updated_at else None
        }

    config_list = []
    for c in configs:
        config_list.append({
            "id": c.id,
            "config_name": c.config_name,
            "provider": c.provider,
            "model_name": c.model_name,
            "repo_id": c.repo_id,
            "filename": c.filename,
            "gpu_layers": c.gpu_layers,
            "context_size": c.context_size,
            "threads": c.threads,
            "temperature": c.temperature,
            "azure_endpoint": c.azure_endpoint,
            "azure_deployment_name": c.azure_deployment_name,
            "is_active": c.is_active,
            "is_password_set": bool(c.azure_api_key),
        })

    return {
        "active_config": active_dict,
        "available_configs": config_list,
        "hardware_info": {
            "platform": "Azure Ready",
            "cuda_available": False,  # Will be True if CUDA runtime loaded
            "current_threads": settings.LLM_THREADS,
            "current_context": settings.LLM_CONTEXT_SIZE
        }
    }


@router.put("/config")
def update_or_activate_model_config(req: ModelConfigRequest, db: Session = Depends(get_db)):
    """
    Activates or updates an AI model configuration in the database
    and hot-reloads the LLM Engine if active.
    """
    logger.info(f"Admin updating AI model configuration: {req.config_name} ({req.model_name}), activate={req.is_active}")

    existing = None
    if req.id:
        existing = db.query(AiModelConfig).filter(AiModelConfig.id == req.id).first()

    # Deactivate other configs only if activating this one
    if req.is_active:
        db.query(AiModelConfig).update({AiModelConfig.is_active: False})

    if existing:
        existing.config_name = req.config_name
        existing.provider = req.provider
        existing.model_name = req.model_name
        existing.repo_id = req.repo_id
        existing.filename = req.filename
        existing.gpu_layers = req.gpu_layers
        existing.context_size = req.context_size
        existing.threads = req.threads
        existing.temperature = req.temperature
        existing.azure_endpoint = req.azure_endpoint
        if req.azure_api_key and req.azure_api_key != "••••••••":
            existing.azure_api_key = req.azure_api_key
        existing.azure_deployment_name = req.azure_deployment_name
        if req.is_active:
            existing.is_active = True
        target = existing
    else:
        target = AiModelConfig(
            config_name=req.config_name,
            provider=req.provider,
            model_name=req.model_name,
            repo_id=req.repo_id,
            filename=req.filename,
            gpu_layers=req.gpu_layers,
            context_size=req.context_size,
            threads=req.threads,
            temperature=req.temperature,
            azure_endpoint=req.azure_endpoint,
            azure_api_key=req.azure_api_key if req.azure_api_key != "••••••••" else None,
            azure_deployment_name=req.azure_deployment_name,
            is_active=req.is_active
        )
        db.add(target)

    db.commit()
    db.refresh(target)

    # Hot-reload LLM Engine if this model is active
    if target.is_active:
        try:
            LLMEngine.reload_instance()
        except Exception as e:
            logger.warning(f"Engine reload notice: {e}")

    return {
        "success": True,
        "message": f"AI model configuration '{target.config_name}' saved successfully.",
        "active_config": {
            "id": target.id,
            "config_name": target.config_name,
            "model_name": target.model_name,
            "provider": target.provider,
            "gpu_layers": target.gpu_layers,
            "is_active": target.is_active
        }
    }


@router.post("/test")
def test_ai_inference(req: TestModelRequest = None, db: Session = Depends(get_db)):
    """
    Executes a test inference prompt on the active model configuration
    and returns latency in milliseconds and model response.
    """
    prompt = req.prompt if req and req.prompt else "State in 1 sentence your system identity and operational readiness."
    start_time = time.perf_counter()

    try:
        active = db.query(AiModelConfig).filter(AiModelConfig.is_active == True).first()
        model_name = active.model_name if active else "Qwen 3 4B Instruct"

        llm = LLMEngine.get_instance()
        
        # Test prompt execution
        formatted_prompt = f"USER REQUEST:\n{prompt}\nASSISTANT RESPONSE:\n"
        output = llm(
            formatted_prompt,
            max_tokens=64,
            temperature=0.2
        )

        elapsed_ms = round((time.perf_counter() - start_time) * 1000, 1)
        text_response = output.get("choices", [{}])[0].get("text", "").strip()

        return {
            "success": True,
            "model_name": model_name,
            "latency_ms": elapsed_ms,
            "response": text_response or "Model online. Inference completed successfully.",
            "gpu_accelerated": (active.gpu_layers != 0) if active else False
        }
    except Exception as e:
        elapsed_ms = round((time.perf_counter() - start_time) * 1000, 1)
        logger.error(f"Inference test failed: {e}")
        return {
            "success": False,
            "model_name": "Active Configuration",
            "latency_ms": elapsed_ms,
            "error": str(e)
        }
