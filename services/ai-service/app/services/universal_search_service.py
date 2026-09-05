import logging
import httpx
from typing import Any, Dict, List, Optional
import re
from app.config import settings
from app.schemas import (
    DomainIntentEnum,
    FrontendActionDirective,
    UniversalSearchRequest,
    UniversalSearchResultItem,
    UniversalSearchResponse,
)
from app.services.llm_engine import LLMEngine
from app.services.smart_search_service import SmartSearchService

logger = logging.getLogger("ai_service.universal_search")


class UniversalSearchService:
    @staticmethod
    def _classify_intent_regex(query: str) -> DomainIntentEnum:
        lower_query = query.lower()
        if any(w in lower_query for w in ["company", "companies", "partner", "partners", "firm", "firms", "organization", "organizations"]):
            return DomainIntentEnum.COMPANIES
        if any(w in lower_query for w in ["student", "students", "candidate", "candidates", "graduate", "graduates", "alumni"]):
            return DomainIntentEnum.STUDENTS
        return DomainIntentEnum.VACANCIES

    @staticmethod
    def classify_domain_intent(query: str) -> DomainIntentEnum:
        """Use Gemini LLM for domain intent classification with regex fallback."""
        try:
            llm = LLMEngine.get_instance()
            prompt = (
                "SYSTEM INSTRUCTION:\n"
                "You are an intelligent search domain classifier for the NSBM Green University Industry & Alumni Portal.\n"
                "Classify the user's search query into exactly one domain category:\n"
                "- 'vacancies' — Job postings, internships, placements, employment roles, salary queries, career openings\n"
                "- 'companies' — Partner organizations, hiring employers, corporate collaborators, industries, firms\n"
                "- 'students' — Candidates, undergraduates, fresh graduates, alumni, candidate profiles, talent with skills\n\n"
                "Respond with ONLY the category name ('vacancies', 'companies', or 'students'), nothing else.\n\n"
                "USER REQUEST:\n"
                f"{query}\n"
            )
            response = llm(prompt, max_tokens=60, temperature=0.0)
            output = response["choices"][0]["text"].strip().lower()

            if "vacanc" in output:
                return DomainIntentEnum.VACANCIES
            elif "compan" in output or "partner" in output:
                return DomainIntentEnum.COMPANIES
            elif "student" in output or "candidate" in output:
                return DomainIntentEnum.STUDENTS
            
            # fallback if LLM gave unexpected output
            return UniversalSearchService._classify_intent_regex(query)
            
        except Exception as e:
            logger.warning(f"LLM classification failed: {e}. Falling back to regex.")
            return UniversalSearchService._classify_intent_regex(query)

    @staticmethod
    async def fetch_domain_data(domain: DomainIntentEnum) -> List[Dict[str, Any]]:
        base_url = settings.BACKEND_API_BASE_URL
        url = ""
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                if domain == DomainIntentEnum.VACANCIES:
                    url = f"{base_url}/vacancies/public?page=0&size=50"
                elif domain == DomainIntentEnum.COMPANIES:
                    url = f"{base_url}/admin/partner-verifications?status=APPROVED"
                elif domain == DomainIntentEnum.STUDENTS:
                    url = f"{base_url}/admin/users?role=STUDENT&page=0&size=50"
                else:
                    return []

                # Attempt fetch
                logger.info(f"Fetching backend data for {domain} from {url}")
                response = await client.get(url)
                if response.status_code == 200:
                    response_json = response.json()
                    if not response_json:
                        return []
                    
                    raw = response_json.get("data") if (isinstance(response_json, dict) and "data" in response_json and response_json["data"] is not None) else response_json
                    
                    if isinstance(raw, list):
                        return raw
                    if isinstance(raw, dict) and "content" in raw and isinstance(raw["content"], list):
                        return raw["content"]
                    if isinstance(response_json, dict) and "content" in response_json and isinstance(response_json["content"], list):
                        return response_json["content"]
                    
                    return []
                else:
                    logger.error(f"Backend API returned {response.status_code} for {url}: {response.text}")
                    return []
        except Exception as e:
            logger.error(f"Failed to fetch {domain} data: {e}")
            return []

    @staticmethod
    def generate_directive(domain: DomainIntentEnum, current_route: Optional[str], total_results: int) -> FrontendActionDirective:
        is_same_domain = False
        if current_route:
            if domain == DomainIntentEnum.VACANCIES and "vacancies" in current_route:
                is_same_domain = True
            elif domain == DomainIntentEnum.COMPANIES and ("companies" in current_route or "collaborators" in current_route):
                is_same_domain = True
            elif domain == DomainIntentEnum.STUDENTS and "students" in current_route:
                is_same_domain = True

        action = "DISPLAY_RESULTS" if is_same_domain else "NAVIGATE_AND_FILTER"
        
        target_route = "/student/vacancies"
        domain_name = "vacancies"
        if domain == DomainIntentEnum.COMPANIES:
            target_route = "/student/companies"
            domain_name = "companies"
        elif domain == DomainIntentEnum.STUDENTS:
            target_route = "/staff/students"
            domain_name = "students"

        headline = f"Found {total_results} matching {domain_name}."
        if total_results == 0:
            headline = f"No matching {domain_name} found."

        explanation = f"Your query targets {domain_name}. "
        if not is_same_domain:
            explanation += f"We suggest switching to the {domain_name.capitalize()} section to view these results."
        else:
            explanation += f"Showing the top results based on your search criteria."

        return FrontendActionDirective(
            action=action,
            target_domain=domain.value,
            suggested_route=target_route,
            headline=headline,
            explanation=explanation,
            badge_label=domain_name.capitalize()
        )

    @staticmethod
    async def execute_universal_search(request: UniversalSearchRequest) -> UniversalSearchResponse:
        domain = UniversalSearchService.classify_domain_intent(request.query)
        logger.info(f"Detected intent domain: {domain.value}")

        parsed_intent = SmartSearchService.parse_query_intent(request.query)
        raw_items = await UniversalSearchService.fetch_domain_data(domain)

        if not raw_items:
            directive = UniversalSearchService.generate_directive(domain, request.current_route, 0)
            directive.explanation = "Data could not be fetched or no data exists at the moment."
            return UniversalSearchResponse(
                detected_domain=domain.value,
                parsed_intent=parsed_intent,
                directive=directive,
                results=[],
                total_found=0
            )

        from app.schemas import SmartSearchRequest as OldSmartSearchRequest
        search_type = "vacancies"
        if domain == DomainIntentEnum.STUDENTS:
            search_type = "candidates"
            
        old_request = OldSmartSearchRequest(
            query=request.query,
            search_type=search_type,
            limit=request.limit,
            items_to_rank=raw_items
        )
        
        ranked_response = SmartSearchService.execute_smart_search(old_request)
        
        universal_results = []
        for r in ranked_response.results:
            universal_results.append(UniversalSearchResultItem(
                id=r.id,
                domain=domain.value,
                item=r.item,
                match_score=r.match_score,
                highlight_reasons=r.highlight_reasons
            ))
            
        directive = UniversalSearchService.generate_directive(domain, request.current_route, len(universal_results))

        return UniversalSearchResponse(
            detected_domain=domain.value,
            parsed_intent=ranked_response.parsed_intent,
            directive=directive,
            results=universal_results,
            total_found=len(universal_results)
        )
