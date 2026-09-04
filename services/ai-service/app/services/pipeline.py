import time
import logging
from pathlib import Path
from sqlalchemy.orm import Session
from app.models import VacancyRecord
from app.schemas import InstitutionalFitAnalysis, JobVacancySchema, VacancyParseResponse
from app.services.downloader import download_image_to_tempfile
from app.services.institutional_checker import InstitutionalFitChecker
from app.services.llm_engine import LLMEngine
from app.services.ocr_engine import OCREngine

logger = logging.getLogger("ai_service.services.pipeline")


from typing import Callable, Optional

class VacancyPipelineService:
    def __init__(self):
        self.ocr = OCREngine()

    async def process_and_save(self, image_url: str, db: Session, partner_id: str = None, progress_callback: Optional[Callable[[str], None]] = None) -> VacancyParseResponse:
        start_time = time.perf_counter()
        temp_file_path: Path = await download_image_to_tempfile(image_url)

        try:
            # 1. OCR Extraction
            logger.info(f"Starting OCR Extraction for {temp_file_path}")
            if progress_callback: progress_callback("[bold yellow]Downloading and extracting text via OCR...[/bold yellow]")
            raw_text = self.ocr.extract_text(temp_file_path)
            if not raw_text.strip():
                logger.error("OCR extracted no text.")
                raise ValueError("Could not extract any readable text from the provided job flyer or document.")
            logger.info(f"OCR Extraction successful. Extracted {len(raw_text)} characters.")

            # 2. LLM Extraction into Structured Schema
            logger.info("Starting LLM parsing to structured JSON...")
            if progress_callback: progress_callback("[bold cyan]Extracting structures via LLM...[/bold cyan]")
            parsed_data: JobVacancySchema = LLMEngine.extract_structured_json(raw_text)
            logger.info(f"LLM parsed Job Title: {parsed_data.job_title} at {parsed_data.company_name}")

            # 3. Institutional Suitability & Missing Explicit Fields Check
            logger.info("Starting Institutional Fit Check...")
            if progress_callback: progress_callback("[bold magenta]Evaluating institutional fit & degree alignment...[/bold magenta]")
            institutional_analysis: InstitutionalFitAnalysis = InstitutionalFitChecker.evaluate(parsed_data)
            logger.info(f"Institutional Check complete. Score: {institutional_analysis.institutional_match_score}")

            # 4. Database Persistence in PostgreSQL
            logger.info("Saving extracted record to database...")
            if progress_callback: progress_callback("[bold green]Saving candidate profile to database...[/bold green]")
            record = VacancyRecord(
                job_title=parsed_data.job_title,
                company_name=parsed_data.company_name,
                workplace_type=parsed_data.workplace_type,
                min_experience_years=parsed_data.min_experience_years,
                salary_raw=parsed_data.salary_raw,
                locations=parsed_data.locations,
                required_skills=parsed_data.required_skills,
                preferred_skills=parsed_data.preferred_skills,
                responsibilities=parsed_data.responsibilities,
                education_requirements=parsed_data.education_requirements,
                contact_emails=parsed_data.contact_emails,
                raw_extracted_payload={
                    **parsed_data.model_dump(),
                    "institutional_analysis": institutional_analysis.model_dump(),
                    "partner_id": partner_id
                },
                source_image_url=image_url
            )
            db.add(record)
            db.commit()
            db.refresh(record)
            logger.info(f"Record successfully saved with ID: {record.id}")

            elapsed = round(time.perf_counter() - start_time, 2)
            logger.info(f"Pipeline completed in {elapsed} seconds")

            return VacancyParseResponse(
                vacancy_id=record.id,
                extracted_data=parsed_data,
                institutional_analysis=institutional_analysis,
                processing_time_seconds=elapsed
            )

        finally:
            if temp_file_path.exists():
                temp_file_path.unlink()
