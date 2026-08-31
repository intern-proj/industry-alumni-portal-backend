from fastapi import APIRouter, HTTPException, status
from app.schemas import SmartSearchParsedIntent, SmartSearchRequest, SmartSearchResponse
from app.services.smart_search_service import SmartSearchService

router = APIRouter(prefix="/api/v1/ai/smart-search", tags=["Smart AI Search"])


@router.post("/vacancies", response_model=SmartSearchResponse)
def smart_search_vacancies(request: SmartSearchRequest):
    """
    Executes natural language smart search for job vacancies, e.g.:
    'find me vacancy within colombo that pays more than 100000LKR for software engineering graduate'
    """
    try:
        request.search_type = "vacancies"
        return SmartSearchService.execute_smart_search(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Smart search processing failed: {str(e)}"
        )


@router.post("/candidates", response_model=SmartSearchResponse)
def smart_search_candidates(request: SmartSearchRequest):
    """
    Executes natural language smart search for candidates, e.g.:
    'find me candidates that excel in react and springboot'
    """
    try:
        request.search_type = "candidates"
        return SmartSearchService.execute_smart_search(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Smart candidate search processing failed: {str(e)}"
        )


@router.get("/parse-intent", response_model=SmartSearchParsedIntent)
def parse_search_intent(q: str):
    """
    Returns structured entity decomposition of a natural language search query.
    """
    try:
        return SmartSearchService.parse_query_intent(q)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Failed to parse query intent: {str(e)}"
        )
