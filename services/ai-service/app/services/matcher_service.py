from typing import Any, Dict, List
from app.schemas import (
    CandidateVacancyMatchRequest,
    CandidateVacancyMatchResponse,
    MatchedCandidateItem,
    MatchedVacancyItem,
    RecruiterCandidateMatchRequest,
    RecruiterCandidateMatchResponse,
)


class MatcherService:
    """
    Computes semantic and skill-overlap matching percentages between candidates and vacancies.
    Enforces active job seeker status filtering for recruiters.
    """

    @classmethod
    def match_vacancies_for_candidate(cls, request: CandidateVacancyMatchRequest) -> CandidateVacancyMatchResponse:
        cand_skills = {s.lower().strip() for s in request.candidate_skills if s}
        matched_items: List[MatchedVacancyItem] = []

        for v in request.vacancies:
            v_id = v.get("id", v.get("vacancy_id"))
            title = v.get("title", v.get("job_title", "Untitled Role"))
            company = v.get("companyName", v.get("company_name", "Partner Company"))

            # Extract vacancy skills
            raw_req = v.get("requirements", "") or ""
            raw_tags = v.get("tags", "") or ""
            req_skills = v.get("required_skills", []) or []

            if isinstance(req_skills, str):
                req_skills = [s.strip() for s in req_skills.split(",") if s.strip()]

            combined_v_text = f"{title} {raw_req} {raw_tags} {' '.join(req_skills)}".lower()

            # Skill overlap
            matched_skills = []
            missing_skills = []

            for sk in cand_skills:
                if sk in combined_v_text:
                    matched_skills.append(sk.title())

            for req in req_skills:
                if req.lower() not in cand_skills:
                    missing_skills.append(req.title())

            # Calculate match percentage
            score = 40  # baseline
            if req_skills:
                match_ratio = len(matched_skills) / max(1, len(req_skills))
                score += int(match_ratio * 45)
            elif matched_skills:
                score += min(45, len(matched_skills) * 15)

            # Location match bonus
            v_loc = str(v.get("location", "")).lower()
            if any(ploc.lower() in v_loc for ploc in request.preferred_locations):
                score += 10

            final_pct = max(25, min(98, score))

            if final_pct >= 85:
                fit_summary = "High Match - Your skill profile aligns strongly with key requirements."
            elif final_pct >= 65:
                fit_summary = "Good Match - Meets core competencies with a few growth areas."
            else:
                fit_summary = "Moderate Match - Potential stretch opportunity for your profile."

            matched_items.append(MatchedVacancyItem(
                vacancy_id=v_id,
                title=title,
                company_name=company,
                match_percentage=final_pct,
                matched_skills=matched_skills,
                missing_skills=missing_skills[:4],
                fit_summary=fit_summary
            ))

        # Sort by match percentage descending
        matched_items.sort(key=lambda x: x.match_percentage, reverse=True)

        return CandidateVacancyMatchResponse(
            status="success",
            matched_vacancies=matched_items,
            total_evaluated=len(request.vacancies)
        )

    @classmethod
    def match_candidates_for_vacancy(cls, request: RecruiterCandidateMatchRequest) -> RecruiterCandidateMatchResponse:
        v_skills = {s.lower().strip() for s in request.required_skills + request.preferred_skills if s}
        matched_candidates: List[MatchedCandidateItem] = []

        for c in request.candidates:
            # 1. Active Job Search Filter Check (STRICT REQUIREMENT)
            search_status = str(c.get("job_search_status", c.get("jobSearchStatus", c.get("status", "ACTIVELY_LOOKING")))).upper()
            is_actively_seeking = c.get("actively_searching", c.get("actively_seeking_job", c.get("isActivelySeeking", True)))

            # If candidate explicitly marked as not seeking, skip
            if not is_actively_seeking or search_status in ["NOT_LOOKING", "EMPLOYED_INACTIVE", "INACTIVE"]:
                continue

            c_id = c.get("id", c.get("userId", c.get("candidate_id")))
            name = c.get("fullName", c.get("full_name", c.get("name", "NSBM Graduate")))
            email = c.get("email", c.get("contact_email"))

            cand_skills_raw = c.get("skills", c.get("technical_skills", []))
            if isinstance(cand_skills_raw, str):
                cand_skills = [s.strip().lower() for s in cand_skills_raw.split(",") if s.strip()]
            elif isinstance(cand_skills_raw, list):
                cand_skills = [str(s).strip().lower() for s in cand_skills_raw if s]
            else:
                cand_skills = []

            # Match skills
            matched_skills = []
            missing_skills = []

            for sk in cand_skills:
                if sk in v_skills or any(vsk in sk for vsk in v_skills):
                    matched_skills.append(sk.title())

            for vsk in v_skills:
                if vsk not in [s.lower() for s in cand_skills]:
                    missing_skills.append(vsk.title())

            # Calculate match percentage
            score = 45
            if v_skills:
                overlap_ratio = len(matched_skills) / max(1, len(v_skills))
                score += int(overlap_ratio * 45)
            elif matched_skills:
                score += min(45, len(matched_skills) * 12)

            final_pct = max(30, min(99, score))

            rec_note = (
                f"Candidate actively seeking opportunities with {len(matched_skills)} matching competencies: "
                f"{', '.join(matched_skills[:3]) if matched_skills else 'Strong relevant academic background'}."
            )

            matched_candidates.append(MatchedCandidateItem(
                candidate_id=c_id,
                full_name=name,
                email=email,
                job_search_status=search_status,
                actively_searching=True,
                match_percentage=final_pct,
                matched_skills=matched_skills,
                missing_skills=missing_skills[:4],
                recommendation_note=rec_note
            ))

        # Sort by match percentage descending
        matched_candidates.sort(key=lambda x: x.match_percentage, reverse=True)

        return RecruiterCandidateMatchResponse(
            status="success",
            vacancy_title=request.vacancy_title,
            matched_candidates=matched_candidates,
            total_active_candidates_evaluated=len(matched_candidates)
        )
