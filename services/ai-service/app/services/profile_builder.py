import logging
import asyncio
from typing import List
from sqlalchemy.orm import Session
from app.models import CandidateProfileCache
from app.services.downloader import download_image_to_tempfile
from app.services.ocr_engine import OCREngine
from app.services.resume_extractor import ResumeExtractor
from app.services.embedding_engine import EmbeddingEngine
from app.services.ats_scorer import ATSScorer

logger = logging.getLogger("ai_service.profile_builder")

class CandidateProfileBuilder:
    @staticmethod
    async def build_unified_profile(user_id: str, resume_urls: List[str], profile_skills: List[str], db: Session):
        """
        Orchestrates the background profile building process.
        """
        # Get or create cache entry
        profile = db.query(CandidateProfileCache).filter_by(user_id=user_id).first()
        if not profile:
            profile = CandidateProfileCache(user_id=user_id)
            db.add(profile)
            
        profile.status = "PROCESSING"
        profile.resume_urls = resume_urls
        profile.profile_skills = profile_skills
        db.commit()

        try:
            ocr_engine = OCREngine()
            all_skills = set(profile_skills)
            max_experience = 0.0
            highest_seniority = "Intern"
            target_roles = set()
            best_education = ""
            best_faculty = ""
            all_certifications = set()
            all_languages = set()
            
            seniority_ranks = {"Intern": 0, "Junior": 1, "Mid": 2, "Senior": 3, "Lead": 4, "Manager": 5}

            for url in resume_urls:
                if not url.startswith("http"):
                    continue
                
                try:
                    # 1. Download & OCR
                    file_path = await download_image_to_tempfile(url)
                    raw_text = ocr_engine.extract_text(file_path)
                    file_path.unlink() # Cleanup

                    if not raw_text.strip():
                        continue

                    # 2. Extract structured data
                    extracted = ResumeExtractor.extract_structured_resume(raw_text)

                    # 3. Merge
                    all_skills.update(extracted.skills)
                    target_roles.update(extracted.target_roles)
                    all_certifications.update(extracted.certifications)
                    all_languages.update(extracted.languages)
                    
                    if extracted.experience_years > max_experience:
                        max_experience = extracted.experience_years
                        
                    if extracted.seniority_level:
                        current_rank = seniority_ranks.get(highest_seniority, 0)
                        new_rank = seniority_ranks.get(extracted.seniority_level, 0)
                        if new_rank > current_rank:
                            highest_seniority = extracted.seniority_level
                            
                    if extracted.education and len(extracted.education) > len(best_education):
                        best_education = extracted.education
                    if extracted.faculty and len(extracted.faculty) > len(best_faculty):
                        best_faculty = extracted.faculty

                except Exception as ex:
                    logger.error(f"Error processing resume {url}: {ex}")

            # 4. Generate Semantic Profile & Embedding
            # We use ATSScorer's build_dense_resume_profile logic but with merged data
            skills_list = list(all_skills)
            roles_str = ", ".join(list(target_roles)[:3]) if target_roles else "Candidate"
            semantic_text = ATSScorer.build_dense_resume_profile(
                target_role=roles_str,
                skills=skills_list,
                raw_snippet=f"Experience: {max_experience} years. {best_education}",
                faculty=best_faculty
            )
            
            embedding = EmbeddingEngine.encode_text(semantic_text)

            # 5. Save to DB
            profile.extracted_skills = skills_list
            profile.seniority_level = highest_seniority
            profile.experience_years = max_experience
            profile.target_roles = list(target_roles)
            profile.education = best_education
            profile.faculty = best_faculty
            profile.certifications = list(all_certifications)
            profile.semantic_profile_text = semantic_text
            profile.embedding_vector = embedding
            profile.status = "READY"
            
            db.commit()
            logger.info(f"Successfully built unified profile for {user_id}")

        except Exception as e:
            logger.error(f"Failed to build unified profile for {user_id}: {e}")
            profile.status = "FAILED"
            db.commit()
