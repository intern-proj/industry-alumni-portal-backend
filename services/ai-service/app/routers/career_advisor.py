from fastapi import APIRouter, HTTPException, status
from app.schemas import (
    CandidateVacancyMatchRequest,
    CandidateVacancyMatchResponse,
    CareerAdviceResponse,
    RecruiterCandidateMatchRequest,
    RecruiterCandidateMatchResponse,
    ResumeAnalysisRequest,
)
from app.services.matcher_service import MatcherService
from app.services.resume_advisor import ResumeCareerAdvisorService

router = APIRouter(prefix="/api/v1/ai", tags=["Career Advisor & Matching"])
advisor_service = ResumeCareerAdvisorService()


@router.post("/resume/analyze-and-advise", response_model=CareerAdviceResponse)
async def analyze_resume_for_market_guidance(request: ResumeAnalysisRequest):
    """
    Analyzes uploaded resume or resume text and generates market-driven improvements,
    target skill focus areas, and readiness score for the candidate's target job role.
    """
    try:
        return await advisor_service.analyze_and_advise(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to analyze resume: {str(e)}"
        )


@router.post("/vacancies/recommend-for-candidate", response_model=CandidateVacancyMatchResponse)
def recommend_vacancies_for_candidate(request: CandidateVacancyMatchRequest):
    """
    Evaluates available vacancies against candidate skills and returns ranked vacancies
    with calculated match percentages and fit summaries.
    """
    try:
        return MatcherService.match_vacancies_for_candidate(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to match candidate vacancies: {str(e)}"
        )


@router.post("/candidates/recommend-for-vacancy", response_model=RecruiterCandidateMatchResponse)
def recommend_candidates_for_vacancy(request: RecruiterCandidateMatchRequest):
    """
    Suggests candidate profiles matching a recruiter's job vacancy with match percentages.
    CRITICAL: Only includes candidates whose status is actively searching for jobs.
    """
    try:
        return MatcherService.match_candidates_for_vacancy(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to match candidates for vacancy: {str(e)}"
        )
