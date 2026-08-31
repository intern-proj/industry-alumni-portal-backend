import re
from typing import Any, Dict, List, Optional
from app.schemas import SmartSearchParsedIntent, SmartSearchRequest, SmartSearchResponse, SmartSearchResultItem

SRI_LANKA_LOCATIONS = [
    "colombo", "kandy", "galle", "gampaha", "moratuwa", "jaffna", "kurunegala",
    "negombo", "ratnapura", "kalutara", "batticaloa", "matara", "remote", "hybrid", "on-site"
]

COMMON_TECH_SKILLS = [
    "react", "react.js", "spring", "spring boot", "springboot", "java", "python",
    "javascript", "typescript", "node", "nodejs", "express", "angular", "vue",
    "docker", "kubernetes", "aws", "azure", "gcp", "sql", "postgresql", "mysql",
    "mongodb", "redis", "graphql", "rest", "c#", ".net", "c++", "flutter", "dart",
    "swift", "kotlin", "html", "css", "tailwind", "figma", "machine learning", "ai",
    "nlp", "data science", "tableau", "power bi", "git", "ci/cd", "linux"
]

COMMON_BIZ_SKILLS = [
    "marketing", "digital marketing", "seo", "sem", "content creation", "sales",
    "b2b", "accounting", "auditing", "financial analysis", "excel", "human resources",
    "recruitment", "talent acquisition", "supply chain", "logistics", "business analysis",
    "project management", "scrum", "agile"
]


class SmartSearchService:
    """
    Parses complex natural language queries into structured intent and filters
    candidate or vacancy listings with match scoring and reason highlights.
    """

    @classmethod
    def parse_query_intent(cls, query: str) -> SmartSearchParsedIntent:
        lower_query = query.lower()

        # 1. Extract Locations
        detected_locations = [loc.capitalize() for loc in SRI_LANKA_LOCATIONS if re.search(r'\b' + re.escape(loc) + r'\b', lower_query)]

        # 2. Extract Salary Thresholds (e.g. "more than 100000LKR", "> 150k", "pays 80000")
        min_salary = None
        currency = "LKR"
        if "usd" in lower_query or "$" in lower_query:
            currency = "USD"

        salary_patterns = [
            r'(?:more than|above|over|paying|pays|min|minimum|greater than|\>)\s*(?:lkr|rs\.?|\$)?\s*(\d+(?:[,\.]\d+)?)\s*(k|lkr|usd)?',
            r'(\d+)\s*(?:k|thousand)\s*(?:lkr|usd)?',
            r'(\d{5,7})\s*(?:lkr|usd)?'
        ]

        for pat in salary_patterns:
            match = re.search(pat, lower_query)
            if match:
                val_str = match.group(1).replace(",", "")
                try:
                    val = float(val_str)
                    # Check if 'k' suffix was used
                    if "k" in lower_query[match.start():match.end() + 2] or (len(match.groups()) > 1 and match.group(2) == "k"):
                        val *= 1000
                    min_salary = val
                    break
                except ValueError:
                    pass

        # 3. Extract Skills
        detected_skills = []
        for skill in COMMON_TECH_SKILLS + COMMON_BIZ_SKILLS:
            if re.search(r'\b' + re.escape(skill) + r'\b', lower_query):
                detected_skills.append(skill.title())

        # 4. Extract Target Role
        target_role = None
        role_patterns = [
            r'(?:for|as|hiring|seeking|want)\s+([a-zA-Z\s]+?)(?:graduate|intern|developer|engineer|specialist|manager|analyst)',
            r'([a-zA-Z\s]+?(?:developer|engineer|specialist|manager|analyst|associate|intern|lead))'
        ]
        for rp in role_patterns:
            match = re.search(rp, lower_query)
            if match:
                candidate_role = match.group(0).strip().title()
                if len(candidate_role) > 3 and not any(w in candidate_role.lower() for w in ["find", "search", "looking", "candidates", "vacancies"]):
                    target_role = candidate_role
                    break

        # 5. Extract Experience / Level
        experience_level = None
        if any(term in lower_query for term in ["intern", "internship"]):
            experience_level = "Internship"
        elif any(term in lower_query for term in ["graduate", "fresher", "entry level", "junior"]):
            experience_level = "Junior/Graduate"
        elif any(term in lower_query for term in ["senior", "lead", "architect"]):
            experience_level = "Senior"

        # 6. Workplace Type
        workplace_type = None
        if "remote" in lower_query:
            workplace_type = "REMOTE"
        elif "hybrid" in lower_query:
            workplace_type = "HYBRID"
        elif "on-site" in lower_query or "onsite" in lower_query:
            workplace_type = "ON_SITE"

        # Key tokens
        tokens = [w for w in re.findall(r'\w+', lower_query) if len(w) > 3 and w not in ["find", "with", "that", "than", "more", "from", "show", "give"]]

        return SmartSearchParsedIntent(
            raw_query=query,
            target_role=target_role,
            locations=detected_locations,
            min_salary=min_salary,
            currency=currency,
            required_skills=detected_skills,
            experience_level=experience_level,
            workplace_type=workplace_type,
            keywords=tokens
        )

    @classmethod
    def execute_smart_search(cls, request: SmartSearchRequest) -> SmartSearchResponse:
        intent = cls.parse_query_intent(request.query)
        items = request.items_to_rank or []

        scored_results: List[SmartSearchResultItem] = []

        for idx, item in enumerate(items):
            item_text = " ".join([str(v) for v in item.values()]).lower()
            score = 50  # Base match
            reasons = []

            # Check skills
            matched_skills = [sk for sk in intent.required_skills if sk.lower() in item_text]
            if matched_skills:
                score += min(30, len(matched_skills) * 15)
                reasons.append(f"Matches skills: {', '.join(matched_skills)}")

            # Check location
            matched_locs = [loc for loc in intent.locations if loc.lower() in item_text]
            if matched_locs:
                score += 15
                reasons.append(f"Located in: {', '.join(matched_locs)}")

            # Check workplace type
            if intent.workplace_type and intent.workplace_type.lower() in item_text:
                score += 10
                reasons.append(f"Offers {intent.workplace_type} work mode")

            # Check salary if vacancy item has salary data
            if intent.min_salary:
                salary_str = str(item.get("salaryRange", item.get("salary_raw", "")))
                num_match = re.search(r'(\d[\d,]*)', salary_str)
                if num_match:
                    try:
                        extracted_val = float(num_match.group(1).replace(",", ""))
                        if extracted_val >= intent.min_salary:
                            score += 15
                            reasons.append(f"Compensation meets requirement ({salary_str})")
                    except ValueError:
                        pass

            # Keyword match
            matched_kw = [kw for kw in intent.keywords if kw in item_text]
            if matched_kw:
                score += min(10, len(matched_kw) * 3)

            final_score = max(10, min(100, score))
            item_id = item.get("id", item.get("vacancy_id", item.get("userId", idx)))

            if not reasons:
                reasons.append("Relevant to search terms")

            scored_results.append(SmartSearchResultItem(
                id=item_id,
                item=item,
                match_score=final_score,
                highlight_reasons=reasons
            ))

        # Sort by score descending
        scored_results.sort(key=lambda x: x.match_score, reverse=True)
        top_results = scored_results[:request.limit]

        return SmartSearchResponse(
            status="success",
            search_type=request.search_type,
            parsed_intent=intent,
            results=top_results,
            total_found=len(scored_results)
        )
