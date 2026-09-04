import logging
import re
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from sqlalchemy.orm import Session

from app.models import ResumeEmbeddingCache, VacancyEmbeddingCache, CandidateProfileCache
from app.schemas import (
    ATSScoreBreakdown,
    ApplicantMatchItem,
    BulkApplicantMatchRequest,
    BulkApplicantMatchResponse,
    MatchedVacancyItem,
    ResumeVacancyMatchRequest,
    ResumeVacancyMatchResponse,
    SingleApplicantMatchRequest,
    SingleApplicantMatchResponse,
)
from app.services.ats_scorer import ATSScorer
from app.services.downloader import download_image_to_tempfile
from app.services.embedding_engine import EmbeddingEngine
from app.services.ocr_engine import OCREngine

logger = logging.getLogger("ai_service.resume_matcher_service")

# Regex pattern for extracting technical skills from raw resume
KNOWN_TECH_SKILLS = [
    "python", "java", "c++", "c#", ".net", "javascript", "typescript", "react", "react.js",
    "angular", "vue", "node", "node.js", "express", "spring", "spring boot", "django",
    "fastapi", "sql", "postgresql", "mysql", "mongodb", "redis", "docker", "kubernetes",
    "aws", "azure", "gcp", "git", "ci/cd", "rest api", "graphql", "microservices",
    "html", "css", "tailwind", "flutter", "dart", "swift", "kotlin", "figma", "scrum",
    "agile", "unit testing", "machine learning", "data science", "nlp"
]


class ResumeVacancyMatcherService:
    def __init__(self):
        self.ocr = OCREngine()

    def _extract_skills_from_text(self, text: str) -> List[str]:
        if not text:
            return []
        lower = text.lower()
        extracted = []
        for sk in KNOWN_TECH_SKILLS:
            # Word boundary check
            pattern = r'\b' + re.escape(sk) + r'\b'
            if re.search(pattern, lower):
                extracted.append(sk.title())
        return list(dict.fromkeys(extracted))

    async def get_or_create_resume_profile(
        self,
        resume_url: Optional[str],
        resume_text: Optional[str],
        user_id: Optional[str],
        fallback_skills: Optional[List[str]] = None,
        faculty: Optional[str] = None,
        db: Session = None,
        force_refresh: bool = False
    ) -> Dict[str, Any]:
        """
        Retrieves cached resume profile and embedding vector, or computes and caches it.
        Gracefully handles blob/mock URLs by utilizing fallback_skills.
        """
        clean_url = (resume_url or "").strip()
        is_valid_remote_url = clean_url.startswith("http://") or clean_url.startswith("https://")
        cache_key = clean_url if is_valid_remote_url else (f"user_{user_id}" if user_id else "")

        # 1. Check DB Cache
        if cache_key and db and not force_refresh:
            cached = db.query(ResumeEmbeddingCache).filter(ResumeEmbeddingCache.resume_url == cache_key).first()
            if cached and cached.embedding_vector and cached.extracted_skills:
                return {
                    "skills": cached.extracted_skills,
                    "profile_text": cached.semantic_profile_text or "",
                    "embedding": cached.embedding_vector,
                    "target_role": cached.target_role or "Software Engineer"
                }

        # 2. Extract Raw Text (via OCR or passed text)
        extracted_text = (resume_text or "").strip()
        if not extracted_text and is_valid_remote_url:
            try:
                temp_path = await download_image_to_tempfile(clean_url)
                try:
                    extracted_text = self.ocr.extract_text(temp_path)
                finally:
                    if temp_path.exists():
                        temp_path.unlink()
            except Exception as e:
                logger.warning(f"Could not OCR resume from URL {clean_url}: {e}")

        # 3. Extract skills and combine with fallback skills
        skills = self._extract_skills_from_text(extracted_text)
        if fallback_skills:
            for s in fallback_skills:
                if s and str(s).strip():
                    skills.append(str(s).strip().title())
        skills = list(dict.fromkeys(skills))

        target_role = "Software Engineer"
        for role_hint in ["Full Stack", "Frontend", "Backend", "Mobile Developer", "DevOps", "Data Scientist", "Software Engineer", "QA Engineer"]:
            if role_hint.lower() in extracted_text.lower():
                target_role = role_hint
                break

        dense_profile = ATSScorer.build_dense_resume_profile(
            target_role=target_role,
            skills=skills,
            raw_snippet=extracted_text[:250],
            faculty=faculty or "Faculty of Computing"
        )

        # 4. Generate dense embedding vector via bge-small-en-v1.5
        try:
            vector = EmbeddingEngine.encode_text(dense_profile)
        except Exception as e:
            logger.error(f"Failed to generate embedding vector: {e}")
            vector = EmbeddingEngine._compute_fallback_vector(dense_profile)

        # 5. Persist to Cache Table
        if cache_key and db:
            try:
                existing = db.query(ResumeEmbeddingCache).filter(ResumeEmbeddingCache.resume_url == cache_key).first()
                if existing:
                    existing.user_id = user_id
                    existing.extracted_skills = skills
                    existing.semantic_profile_text = dense_profile
                    existing.embedding_vector = vector
                    existing.target_role = target_role
                else:
                    new_cache = ResumeEmbeddingCache(
                        resume_url=cache_key,
                        user_id=user_id,
                        extracted_skills=skills,
                        semantic_profile_text=dense_profile,
                        embedding_vector=vector,
                        target_role=target_role
                    )
                    db.add(new_cache)
                db.commit()
            except Exception as e:
                logger.warning(f"Failed to persist resume embedding cache: {e}")
                db.rollback()

        return {
            "skills": skills,
            "profile_text": dense_profile,
            "embedding": vector,
            "target_role": target_role
        }

    def get_or_create_vacancy_profile(
        self,
        v_id: Any,
        title: str,
        company: str,
        requirements: str,
        tags: str,
        workplace_type: str,
        db: Session,
        force_refresh: bool = False
    ) -> Dict[str, Any]:
        """
        Retrieves cached vacancy profile and embedding vector, or computes and caches it.
        """
        cache_id = str(v_id)
        if cache_id and not force_refresh:
            cached = db.query(VacancyEmbeddingCache).filter(VacancyEmbeddingCache.vacancy_id == cache_id).first()
            if cached and cached.embedding_vector:
                return {
                    "structured_text": cached.structured_text,
                    "embedding": cached.embedding_vector
                }

        dense_vac = ATSScorer.build_dense_vacancy_profile(
            title=title,
            company=company,
            requirements=requirements,
            tags=tags,
            workplace_type=workplace_type
        )

        try:
            vector = EmbeddingEngine.encode_text(dense_vac)
        except Exception as e:
            logger.error(f"Failed to encode vacancy: {e}")
            vector = [0.0] * 384

        if cache_id:
            try:
                existing = db.query(VacancyEmbeddingCache).filter(VacancyEmbeddingCache.vacancy_id == cache_id).first()
                if existing:
                    existing.structured_text = dense_vac
                    existing.embedding_vector = vector
                else:
                    new_vac_cache = VacancyEmbeddingCache(
                        vacancy_id=cache_id,
                        structured_text=dense_vac,
                        embedding_vector=vector
                    )
                    db.add(new_vac_cache)
                db.commit()
            except Exception as e:
                logger.warning(f"Failed to persist vacancy embedding cache: {e}")
                db.rollback()

        return {
            "structured_text": dense_vac,
            "embedding": vector
        }

    async def match_resume_to_vacancies(
        self,
        request: ResumeVacancyMatchRequest,
        db: Session
    ) -> ResumeVacancyMatchResponse:
        """
        Ranks a list of vacancies against a student's cached unified profile using the 5-pillar ATS engine.
        """
        # 1. Fetch pre-built CandidateProfileCache for this user_id
        profile = None
        if request.user_id:
            profile = db.query(CandidateProfileCache).filter_by(
                user_id=request.user_id, status="READY"
            ).first()

        if profile:
            cand_skills = profile.extracted_skills or []
            cand_vector = profile.embedding_vector or [0.0] * 384
            cand_profile_text = profile.semantic_profile_text or ""
            cand_seniority = profile.seniority_level or "Intern"
            cand_exp = profile.experience_years or 0.0
        else:
            # Fallback to single resume analysis if unified profile not ready
            logger.warning(f"Unified profile not READY for user {request.user_id}, falling back to single resume.")
            resume_profile = await self.get_or_create_resume_profile(
                resume_url=request.resume_url,
                resume_text=request.resume_text,
                user_id=request.user_id,
                fallback_skills=request.candidate_skills,
                db=db,
                force_refresh=request.force_refresh
            )
            cand_skills = resume_profile["skills"]
            cand_vector = resume_profile["embedding"]
            cand_profile_text = resume_profile["profile_text"]
            cand_seniority = "Intern"
            cand_exp = 0.0

        prelim_matches: List[Dict[str, Any]] = []

        # 2. Stage 1: Vector similarity + Skill coverage across all vacancies
        for v in request.vacancies:
            v_id = v.get("id") or v.get("vacancy_id")
            title = v.get("title", "Untitled Position")
            company = v.get("companyName", v.get("company_name", "Partner Enterprise"))
            req_str = v.get("requirements", "") or ""
            tags_str = v.get("tags", "") or ""
            workplace = v.get("workplaceType", "") or ""

            # Extract required skills list
            req_skills = v.get("required_skills", []) or []
            if not req_skills and tags_str:
                req_skills = [s.strip() for s in tags_str.split(",") if s.strip()]
            
            extracted = self._extract_skills_from_text(f"{title} {req_str} {tags_str}")
            req_skills.extend(extracted)
            req_skills = list(dict.fromkeys(req_skills))

            vac_profile = self.get_or_create_vacancy_profile(
                v_id=v_id,
                title=title,
                company=company,
                requirements=req_str,
                tags=tags_str,
                workplace_type=workplace,
                db=db,
                force_refresh=request.force_refresh
            )

            matched_skills, missing_skills, skills_cov = ATSScorer.match_skill_lists(cand_skills, req_skills)
            semantic_sim = EmbeddingEngine.compute_cosine_similarity(cand_vector, vac_profile["embedding"])

            # Location bonus
            loc_fit = 80.0
            v_loc = str(v.get("location", "")).lower()
            if request.preferred_locations and any(ploc.lower() in v_loc for ploc in request.preferred_locations):
                loc_fit = 100.0

            # Stage 1 Rank Score
            # Need min experience for seniority penalty
            req_exp = v.get("min_experience_years", 0.0)
            vac_level = v.get("seniority_level", "Junior")
            
            seniority_penalty = ATSScorer.compute_seniority_penalty(
                cand_seniority=cand_seniority, 
                vac_seniority=vac_level, 
                req_experience=req_exp
            )

            prelim_matches.append({
                "vacancy_id": v_id,
                "title": title,
                "company_name": company,
                "req_skills": req_skills,
                "matched_skills": matched_skills,
                "missing_skills": missing_skills,
                "skills_cov": skills_cov,
                "semantic_sim": semantic_sim,
                "loc_fit": loc_fit,
                "vac_text": vac_profile["structured_text"],
                "vac_level": vac_level,
                "req_exp": req_exp,
                "seniority_penalty": seniority_penalty,
                "stage1_rank_score": ((0.6 * skills_cov) + (0.4 * semantic_sim * 100.0)) * seniority_penalty
            })

        # 3. Stage 2: Cross-Encoder Rerank top 15 candidates
        prelim_matches.sort(key=lambda x: x["stage1_rank_score"], reverse=True)
        top_candidates = prelim_matches[:15]

        rerank_pairs = [
            (f"{m['title']} at {m['company_name']}. Requirements: {', '.join(m['req_skills'])}", cand_profile_text)
            for m in top_candidates
        ]

        cross_scores = EmbeddingEngine.compute_cross_scores(rerank_pairs)

        # 4. Final 5-Pillar Score computation
        matched_items: List[MatchedVacancyItem] = []

        for idx, m in enumerate(top_candidates):
            cross_prob = cross_scores[idx] if idx < len(cross_scores) else m["semantic_sim"]
            final_pct, breakdown, fit_summary, tier = ATSScorer.compute_composite_ats_score(
                skills_coverage=m["skills_cov"],
                semantic_sim=m["semantic_sim"],
                cross_encoder_prob=cross_prob,
                institutional_fit=m["loc_fit"],
                seniority_fit=100.0 if m["seniority_penalty"] == 1.0 else (m["seniority_penalty"] * 100.0),
                seniority_penalty=m["seniority_penalty"]
            )

            matched_items.append(MatchedVacancyItem(
                vacancy_id=m["vacancy_id"],
                title=m["title"],
                company_name=m["company_name"],
                match_percentage=final_pct,
                matched_skills=m["matched_skills"],
                missing_skills=m["missing_skills"][:4],
                fit_summary=f"{tier.replace('_', ' ').title()}: {fit_summary}"
            ))

        # Add any remaining vacancies evaluated by stage 1
        for m in prelim_matches[15:]:
            final_pct, _, fit_summary, tier = ATSScorer.compute_composite_ats_score(
                skills_coverage=m["skills_cov"],
                semantic_sim=m["semantic_sim"],
                cross_encoder_prob=m["semantic_sim"],
                institutional_fit=m["loc_fit"],
                seniority_fit=100.0 if m["seniority_penalty"] == 1.0 else (m["seniority_penalty"] * 100.0),
                seniority_penalty=m["seniority_penalty"]
            )
            matched_items.append(MatchedVacancyItem(
                vacancy_id=m["vacancy_id"],
                title=m["title"],
                company_name=m["company_name"],
                match_percentage=final_pct,
                matched_skills=m["matched_skills"],
                missing_skills=m["missing_skills"][:4],
                fit_summary=fit_summary
            ))

        matched_items.sort(key=lambda x: x.match_percentage, reverse=True)

        return ResumeVacancyMatchResponse(
            status="success",
            extracted_skills=cand_skills,
            matched_vacancies=matched_items,
            total_evaluated=len(request.vacancies)
        )

    async def match_single_applicant(
        self,
        request: SingleApplicantMatchRequest,
        db: Session
    ) -> SingleApplicantMatchResponse:
        """
        Fast evaluation of a single application submission (Option B).
        Pre-computes and returns match percentage and ATS breakdown for instant persistence.
        """
        # 1. Resume profile
        resume_profile = await self.get_or_create_resume_profile(
            resume_url=request.resume_url,
            resume_text=request.resume_text,
            user_id=None,
            fallback_skills=request.candidate_skills,
            faculty=request.candidate_faculty,
            db=db
        )

        cand_skills = resume_profile["skills"]
        cand_vector = resume_profile["embedding"]
        cand_profile_text = resume_profile["profile_text"]

        # 2. Vacancy profile
        vac_profile = self.get_or_create_vacancy_profile(
            v_id=request.vacancy_id or "adhoc",
            title=request.vacancy_title,
            company="Partner Enterprise",
            requirements=request.vacancy_requirements or "",
            tags=request.vacancy_tags or "",
            workplace_type="Full-time",
            db=db
        )

        # 3. Pillar 1: Skills coverage
        req_skills = []
        if request.vacancy_requirements:
            req_skills = [s.strip() for s in request.vacancy_requirements.split(",") if s.strip()]
        if not req_skills:
            req_skills = self._extract_skills_from_text(f"{request.vacancy_title} {request.vacancy_description or ''} {request.vacancy_tags or ''}")

        matched_skills, missing_skills, skills_cov = ATSScorer.match_skill_lists(cand_skills, req_skills)

        # 4. Pillar 2: Semantic vector cosine similarity
        semantic_sim = EmbeddingEngine.compute_cosine_similarity(cand_vector, vac_profile["embedding"])

        # 5. Pillar 3: Cross-encoder rerank score
        pair = (f"{request.vacancy_title}. Requirements: {request.vacancy_requirements or ''}", cand_profile_text)
        cross_scores = EmbeddingEngine.compute_cross_scores([pair])
        cross_prob = cross_scores[0] if cross_scores else semantic_sim

        # 6. Pillar 4: Institutional alignment
        inst_fit = ATSScorer.evaluate_institutional_fit(cand_profile_text, f"{request.vacancy_title} {request.vacancy_requirements or ''}")

        # 7. Composite score calculation
        final_pct, breakdown, summary, tier = ATSScorer.compute_composite_ats_score(
            skills_coverage=skills_cov,
            semantic_sim=semantic_sim,
            cross_encoder_prob=cross_prob,
            institutional_fit=inst_fit,
            seniority_fit=100.0, # single applicant fallback logic doesn't strictly have seniority check unless extracted, assuming 100 for now.
            seniority_penalty=1.0
        )

        # 8. Generate LLM applicant summary and strong fortes taking into account the cover letter
        from app.services.llm_engine import LLMEngine
        llm_insights = LLMEngine.generate_applicant_insights(
            candidate_name=request.candidate_name,
            candidate_skills=cand_skills,
            resume_text=cand_profile_text,
            cover_letter=request.cover_letter,
            vacancy_title=request.vacancy_title,
            vacancy_requirements=request.vacancy_requirements or "",
            vacancy_description=request.vacancy_description or ""
        )

        llm_summary = llm_insights.get("summary") or summary
        strong_fortes = llm_insights.get("strong_fortes") or []
        if llm_insights.get("match_percentage"):
            # Blend 50% LLM holistic evaluation + 50% dense embedding score
            final_pct = int(round(0.5 * final_pct + 0.5 * llm_insights["match_percentage"]))

        return SingleApplicantMatchResponse(
            match_percentage=final_pct,
            matched_skills=matched_skills,
            missing_skills=missing_skills[:5],
            fit_summary=llm_summary,
            strong_fortes=strong_fortes,
            score_breakdown=ATSScoreBreakdown(
                skills_coverage=breakdown["skills_coverage"],
                semantic_alignment=breakdown["semantic_alignment"],
                cross_encoder_score=breakdown["cross_encoder_score"],
                institutional_fit=breakdown["institutional_fit"]
            ),
            match_tier="TOP_MATCH" if final_pct >= 85 else ("STRONG_MATCH" if final_pct >= 70 else "MODERATE_MATCH")
        )

    async def match_applicants_bulk(
        self,
        request: BulkApplicantMatchRequest,
        db: Session
    ) -> BulkApplicantMatchResponse:
        """
        Evaluates and ranks all candidates who applied to a specific vacancy.
        """
        ranked_items: List[ApplicantMatchItem] = []

        for app in request.applicants:
            res_url = app.get("resumeUrl") or app.get("resume_url")
            res_text = app.get("resumeText") or app.get("resume_text")
            c_name = app.get("studentName") or app.get("name") or "Candidate"
            c_email = app.get("studentEmail") or app.get("email")
            app_id = app.get("id") or app.get("applicationId") or app.get("applicant_id")

            single_req = SingleApplicantMatchRequest(
                resume_url=res_url,
                resume_text=res_text,
                vacancy_id=request.vacancy_id,
                vacancy_title=request.vacancy_title,
                vacancy_description=request.vacancy_description,
                vacancy_requirements=request.vacancy_requirements,
                vacancy_tags=request.vacancy_tags,
                candidate_name=c_name,
                candidate_email=c_email
            )

            res = await self.match_single_applicant(single_req, db)

            ranked_items.append(ApplicantMatchItem(
                applicant_id=app_id,
                student_name=c_name,
                student_email=c_email,
                resume_url=res_url,
                match_percentage=res.match_percentage,
                matched_skills=res.matched_skills,
                missing_skills=res.missing_skills,
                fit_summary=res.fit_summary,
                score_breakdown=res.score_breakdown
            ))

        ranked_items.sort(key=lambda x: x.match_percentage, reverse=True)

        return BulkApplicantMatchResponse(
            status="success",
            vacancy_title=request.vacancy_title,
            ranked_applicants=ranked_items,
            total_evaluated=len(request.applicants)
        )
