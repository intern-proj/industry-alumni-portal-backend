import logging
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas import (
    InstitutionalFitAnalysis,
    JobVacancySchema,
    VacancyParseRequest,
    VacancyParseResponse,
)
from app.services.institutional_checker import InstitutionalFitChecker
from app.services.pipeline import VacancyPipelineService

logger = logging.getLogger("ai_service.routers.vacancies")

router = APIRouter(prefix="/api/v1/vacancies", tags=["Vacancies & Flyers"])
pipeline_service = VacancyPipelineService()


@router.post("/parse-and-save", response_model=VacancyParseResponse, status_code=status.HTTP_201_CREATED)
async def parse_and_save_vacancy(
    request: VacancyParseRequest,
    db: Session = Depends(get_db)
):
    """
    Parses a job flyer image or PDF document using OCR + LLM, checks for NSBM
    institutional fit and missing mandatory explicit fields (salary, deadline, email),
    and persists the record to PostgreSQL.
    """
    try:
        logger.info(f"Received parse-and-save request for partner_id: {request.partner_id} with image_url: {request.image_url}")
        response = await pipeline_service.process_and_save(
            str(request.image_url),
            db,
            partner_id=request.partner_id
        )
        logger.info(f"Successfully processed and saved vacancy for partner_id: {request.partner_id}")
        return response
    except Exception as e:
        logger.error(f"Error processing parse-and-save request: {str(e)}", exc_info=True)
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process job flyer: {str(e)}"
        )


@router.post("/institutional-check", response_model=InstitutionalFitAnalysis)
def check_institutional_fit(vacancy: JobVacancySchema):
    """
    Evaluates institutional suitability for NSBM faculties and flags missing fields.
    """
    try:
        return InstitutionalFitChecker.evaluate(vacancy)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Failed to evaluate institutional fit: {str(e)}"
        )
