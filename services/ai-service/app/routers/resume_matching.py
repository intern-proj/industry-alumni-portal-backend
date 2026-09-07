import logging
from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks
from typing import List, Optional, Any, Dict
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas import (
    BulkApplicantMatchRequest,
    BulkApplicantMatchResponse,
    ResumeVacancyMatchRequest,
    ResumeVacancyMatchResponse,
    SingleApplicantMatchRequest,
    SingleApplicantMatchResponse,
    ProfileRebuildRequest,
    EnhanceProfileRequest,
    EnhanceProfileResponse
)
from app.services.resume_matcher_service import ResumeVacancyMatcherService
from app.services.profile_builder import CandidateProfileBuilder
from app.services.llm_engine import LLMEngine
from app.services.resume_extractor import ResumeExtractor
from app.services.downloader import download_image_to_tempfile
from app.services.ocr_engine import OCREngine

logger = logging.getLogger("ai_service.routers.resume_matching")

router = APIRouter(prefix="/api/v1/ai/resume", tags=["Resume & Vacancy AI Matching"])
matcher_service = ResumeVacancyMatcherService()


@router.post("/process-upload")
async def process_resume_upload_immediate(
    request: ResumeVacancyMatchRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db)
):
    """
    Immediate upload trigger: called as soon as a student uploads a CV.
    Downloads the CV, runs OCR, extracts all technical skills, calculates 
    dense semantic embeddings, and stores in resume_embedding_cache.
    Also triggers a background rebuild of the unified candidate profile.
    """
    print("\n" + "=" * 80)
    print(f"[AI SERVICE] IMMEDIATE RESUME UPLOAD PROCESSING STARTED")
    print(f"   User ID:     {request.user_id}")
    print(f"   Resume URL:  {request.resume_url}")
    print("=" * 80 + "\n")
    try:
        profile = await matcher_service.get_or_create_resume_profile(
            resume_url=request.resume_url,
            resume_text=request.resume_text,
            user_id=request.user_id,
            fallback_skills=request.candidate_skills,
            db=db
        )
        
        # Trigger background profile rebuild
        background_tasks.add_task(
            CandidateProfileBuilder.build_unified_profile,
            user_id=request.user_id,
            resume_urls=[request.resume_url], # Ideally fetch all from user service, but for now we'll seed with this one + what's in cache later if needed. The frontend can pass all URLs if we update it.
            profile_skills=request.candidate_skills or [],
            db=db
        )

        print("\n" + "=" * 80)
        print(f"[+] [AI SERVICE] RESUME PROCESSED & EMBEDDED SUCCESSFULLY")
        print(f"   Extracted Skills ({len(profile['skills'])}): {', '.join(profile['skills']) if profile['skills'] else 'None'}")
        print(f"   Target Role:         {profile['target_role']}")
        print(f"   Dense Vector Dim:    {len(profile['embedding'])}")
        print("=" * 80 + "\n")
        return {
            "status": "success",
            "message": "Resume analyzed and embedded successfully. Unified profile rebuilding in background.",
            "extracted_skills": profile["skills"],
            "target_role": profile["target_role"]
        }
    except Exception as e:
        logger.error(f"Error processing resume upload: {e}", exc_info=True)
        print(f"[-] [AI SERVICE] Resume processing failed: {e}\n")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process uploaded resume: {str(e)}"
        )


@router.post("/profile/rebuild")
async def rebuild_unified_profile(
    request: ProfileRebuildRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db)
):
    """
    Explicit trigger to rebuild the unified candidate profile.
    """
    print("\n" + "=" * 80)
    print(f"[*] [AI SERVICE] REBUILDING UNIFIED PROFILE")
    print(f"   User ID:     {request.user_id}")
    print(f"   Resumes:     {len(request.resume_urls)}")
    print("=" * 80 + "\n")
    
    background_tasks.add_task(
        CandidateProfileBuilder.build_unified_profile,
        user_id=request.user_id,
        resume_urls=request.resume_urls,
        profile_skills=request.profile_skills,
        db=db
    )
    
    return {"status": "success", "message": "Profile rebuild started in background"}



@router.post("/match-vacancies", response_model=ResumeVacancyMatchResponse)
async def match_vacancies_for_resume(
    request: ResumeVacancyMatchRequest,
    db: Session = Depends(get_db)
):
    """
    Student flow: Evaluates open job vacancies against candidate's uploaded resume.
    Returns ranked vacancies with AI match percentages and explainable fit badges.
    """
    print("\n" + "=" * 80)
    print(f"[*] [AI SERVICE] MATCHING RESUME TO {len(request.vacancies)} VACANCIES")
    print(f"   User ID:     {request.user_id}")
    print(f"   Resume URL:  {request.resume_url}")
    print("=" * 80 + "\n")
    try:
        res = await matcher_service.match_resume_to_vacancies(request, db)
        print("\n" + "=" * 80)
        print(f"[+] [AI SERVICE] VACANCY MATCHING COMPLETED")
        print(f"   Evaluated:   {res.total_evaluated} vacancies")
        if res.matched_vacancies:
            print("   Match Breakdown:")
            for v in res.matched_vacancies:
                print(f"   - {v.title} at {v.company_name} ({v.match_percentage}%)")
        print("=" * 80 + "\n")
        return res
    except Exception as e:
        logger.error(f"Error matching resume to vacancies: {e}", exc_info=True)
        print(f"[-] [AI SERVICE] Matching failed: {e}\n")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to calculate resume-vacancy matches: {str(e)}"
        )


@router.post("/match-single-applicant", response_model=SingleApplicantMatchResponse)
async def match_single_applicant(
    request: SingleApplicantMatchRequest,
    db: Session = Depends(get_db)
):
    """
    Application Intake (Option B): Evaluates one candidate application at submission time.
    Calculates 4-pillar ATS match percentage and breakdown for persistence in database.
    """
    print("\n" + "=" * 80)
    print(f"[*] [AI SERVICE] SINGLE APPLICANT ATS EVALUATION")
    print(f"   Candidate:   {request.candidate_name or 'Applicant'} ({request.candidate_email or 'N/A'})")
    print(f"   Vacancy:     {request.vacancy_title} (ID: {request.vacancy_id})")
    print("=" * 80 + "\n")
    try:
        res = await matcher_service.match_single_applicant(request, db)
        print("\n" + "=" * 80)
        print(f"[+] [AI SERVICE] APPLICANT ATS MATCH COMPUTED: {res.match_percentage}% ({res.match_tier})")
        print(f"   Matched:     {', '.join(res.matched_skills) if res.matched_skills else 'None'}")
        print(f"   Missing:     {', '.join(res.missing_skills) if res.missing_skills else 'None'}")
        print(f"   Summary:     {res.fit_summary}")
        print("=" * 80 + "\n")
        return res
    except Exception as e:
        logger.error(f"Error matching single applicant: {e}", exc_info=True)
        print(f"[-] [AI SERVICE] Single applicant evaluation failed: {e}\n")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to calculate applicant match score: {str(e)}"
        )


@router.post("/match-applicants-bulk", response_model=BulkApplicantMatchResponse)
async def match_applicants_bulk(
    request: BulkApplicantMatchRequest,
    db: Session = Depends(get_db)
):
    """
    Partner flow: Evaluates and ranks all candidates who applied to a specific vacancy.
    Used for on-demand recalculation or when vacancy requirements are updated.
    """
    print("\n" + "=" * 80)
    print(f"[*] [AI SERVICE] BULK MATCHING {len(request.applicants)} APPLICANTS FOR VACANCY #{request.vacancy_id}")
    print(f"   Role: {request.vacancy_title}")
    print("=" * 80 + "\n")
    try:
        res = await matcher_service.match_applicants_bulk(request, db)
        print(f"[+] [AI SERVICE] Bulk matching complete for {res.total_evaluated} applicants.")
        return res
    except Exception as e:
        logger.error(f"Error in bulk applicant match: {e}", exc_info=True)
        print(f"[-] [AI SERVICE] Bulk matching failed: {e}\n")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to compute bulk applicant matches: {str(e)}"
        )


class CandidateSummaryRequest(BaseModel):
    user_id: Optional[str] = None
    candidate_name: Optional[str] = "Candidate"
    degree_program: Optional[str] = None
    faculty: Optional[str] = None
    gpa: Optional[float] = None
    skills: List[str] = []
    bio: Optional[str] = None
    projects: Optional[List[Any]] = []


class CandidateSummaryResponse(BaseModel):
    status: str = "success"
    summary: str


@router.post("/candidate-summary", response_model=CandidateSummaryResponse)
def get_candidate_ai_summary(
    request: CandidateSummaryRequest,
    db: Session = Depends(get_db)
):
    """
    Generates a real, authentic LLM executive profile summary for a student citing real projects.
    """
    print(f"\n[AI SERVICE] Generating Real LLM Candidate Summary for: {request.candidate_name} ({request.user_id})")

    summary = LLMEngine.generate_candidate_profile_summary(
        candidate_name=request.candidate_name,
        degree_program=request.degree_program,
        faculty=request.faculty,
        gpa=request.gpa,
        skills=request.skills,
        bio=request.bio,
        projects=request.projects
    )
    print(f"[AI SERVICE] LLM Output: {summary}\n")
    return CandidateSummaryResponse(status="success", summary=summary)


@router.post("/enhance-profile", response_model=EnhanceProfileResponse)
async def enhance_candidate_profile_from_resume(
    request: EnhanceProfileRequest,
    db: Session = Depends(get_db)
):
    """
    Parses candidate's primary resume using MuPDF/OCR and Qwen LLM.
    Extracts verified technical skills, completed projects with tech stacks and descriptions,
    and a synthesized executive bio.
    """
    print("\n" + "=" * 80)
    print(f"[AI SERVICE] ENHANCING CANDIDATE PROFILE FROM PRIMARY RESUME")
    print(f"   User ID:     {request.user_id}")
    print(f"   Resume URL:  {request.resume_url}")
    print("=" * 80 + "\n")

    clean_url = (request.resume_url or "").strip()
    extracted_text = ""

    if clean_url and (clean_url.startswith("http://") or clean_url.startswith("https://")):
        try:
            temp_path = await download_image_to_tempfile(clean_url)
            try:
                ocr = OCREngine()
                extracted_text = ocr.extract_text(temp_path)
            finally:
                if temp_path.exists():
                    temp_path.unlink()
        except Exception as dl_err:
            logger.warning(f"Could not download resume directly from URL {clean_url}: {dl_err}")

    # Fallback to cached profile text or fallback skills if download was blocked
    if not extracted_text.strip():
        from app.models import ResumeEmbeddingCache
        cached = db.query(ResumeEmbeddingCache).filter(ResumeEmbeddingCache.user_id == request.user_id).first()
        if cached and cached.semantic_profile_text:
            extracted_text = cached.semantic_profile_text

    if not extracted_text.strip() and request.fallback_skills:
        extracted_text = f"Candidate Profile. Technical Skills: {', '.join(request.fallback_skills)}."

    # Parse with Qwen LLM in threadpool to avoid blocking event loop
    from starlette.concurrency import run_in_threadpool
    structured = await run_in_threadpool(ResumeExtractor.extract_structured_resume, extracted_text)

    # Merge fallback skills
    merged_skills = list(dict.fromkeys(structured.skills + (request.fallback_skills or [])))

    default_bio = (
        f"Dedicated {structured.target_roles[0] if structured.target_roles else 'Software Engineer'} with hands-on experience in modern application development and software engineering best practices."
        if merged_skills else ""
    )

    return EnhanceProfileResponse(
        status="success",
        user_id=request.user_id,
        candidate_name=structured.candidate_name,
        bio=structured.bio or default_bio,
        target_roles=structured.target_roles,
        skills=merged_skills,
        projects=structured.projects,
        education=structured.education,
        faculty=structured.faculty,
        experience_years=structured.experience_years
    )

