import json
import logging
import re
from typing import List, Optional
from app.schemas import CareerAdviceResponse, ResumeAnalysisRequest
from app.services.downloader import download_image_to_tempfile
from app.services.ocr_engine import OCREngine
from app.services.llm_engine import LLMEngine

logger = logging.getLogger("ai_service.resume_advisor")


class ResumeCareerAdvisorService:
    """
    Analyzes student/alumni resumes using Gemini intelligence, identifies role-specific market gaps,
    and provides tailored, actionable career guidance and readiness evaluation for their target role.
    """

    def __init__(self):
        self.ocr = OCREngine()

    async def analyze_and_advise(self, request: ResumeAnalysisRequest) -> CareerAdviceResponse:
        resume_content = request.resume_text or ""

        # If resume_url provided, fetch and extract text
        if not resume_content and request.resume_url:
            temp_path = await download_image_to_tempfile(str(request.resume_url))
            try:
                resume_content = self.ocr.extract_text(temp_path)
            finally:
                if temp_path.exists():
                    temp_path.unlink()

        target_role = request.target_job_position or "Software Engineer"
        clean_resume = " ".join(resume_content.split())[:12000]

        prompt = (
            "SYSTEM INSTRUCTION:\n"
            "You are a Senior University Career Advisor and Industry Readiness Strategist at NSBM Green University.\n"
            "Analyze the candidate's resume dossier against their target career position.\n\n"
            "EVALUATION REQUIREMENTS:\n"
            "- candidate_name: Candidate's full name from resume (or null if not found)\n"
            "- market_competitiveness_score: Calibrated integer (0-100) reflecting genuine competitiveness in the current industry job market for this target role.\n"
            "- match_summary: A 2-3 sentence executive summary of candidate readiness, highlighting primary alignment and core strategic steps.\n"
            "- strength_areas: 3 to 5 specific, evidence-backed strength areas based on actual projects, tools, coursework, or experiences.\n"
            "- improvement_areas: 3 to 4 actionable, role-tailored improvement recommendations (e.g., industry certifications, modern tool adoption, project metrics, architectural depth).\n"
            "- recommended_skills_to_focus: 4 to 6 specific, modern technologies or functional competencies crucial for the target role.\n"
            "- suggested_certifications: 2 to 3 recognized industry certifications directly relevant to the target role.\n"
            "- extracted_skills: Comprehensive list of technical and domain skills found on the resume.\n\n"
            "Return ONLY a strict JSON object:\n"
            "{\n"
            '  "candidate_name": "Full Name",\n'
            '  "market_competitiveness_score": 82,\n'
            '  "match_summary": "Summary...",\n'
            '  "strength_areas": ["Strength 1", "Strength 2"],\n'
            '  "improvement_areas": ["Improvement 1", "Improvement 2"],\n'
            '  "recommended_skills_to_focus": ["Skill 1", "Skill 2"],\n'
            '  "suggested_certifications": ["Cert 1", "Cert 2"],\n'
            '  "extracted_skills": ["Skill A", "Skill B"]\n'
            "}\n\n"
            "USER REQUEST:\n"
            f"Target Career Position: {target_role}\n"
            f"Resume Dossier:\n{clean_resume}\n"
        )

        try:
            llm = LLMEngine.get_instance()
            response = llm(
                prompt,
                max_tokens=1500,
                temperature=0.1
            )
            raw_text = response["choices"][0]["text"].strip()
            if raw_text.startswith("```json"):
                raw_text = raw_text[7:]
            if raw_text.endswith("```"):
                raw_text = raw_text[:-3]

            parsed = json.loads(raw_text.strip())

            score = int(parsed.get("market_competitiveness_score", 75))
            score = max(25, min(98, score))

            return CareerAdviceResponse(
                target_role=target_role,
                candidate_name=parsed.get("candidate_name"),
                market_competitiveness_score=score,
                match_summary=parsed.get("match_summary", f"Profile evaluated for {target_role}."),
                strength_areas=parsed.get("strength_areas", [])[:5],
                improvement_areas=parsed.get("improvement_areas", [])[:5],
                recommended_skills_to_focus=parsed.get("recommended_skills_to_focus", [])[:6],
                suggested_certifications=parsed.get("suggested_certifications", [])[:3],
                extracted_skills=parsed.get("extracted_skills", [])
            )
        except Exception as e:
            logger.warning(f"[CareerAdvisor] Gemini analysis failed, using dynamic fallback: {e}")

        # Baseline fallback
        lower_resume = resume_content.lower()
        extracted = [w.title() for w in ["Python", "Java", "React", "Spring Boot", "SQL", "Docker", "Git"] if w.lower() in lower_resume]
        return CareerAdviceResponse(
            target_role=target_role,
            market_competitiveness_score=72,
            match_summary=f"Your profile demonstrates solid foundational preparation for {target_role} roles. Strengthening hands-on project evidence and industry certifications will enhance your competitiveness.",
            strength_areas=["Demonstrated foundational domain coursework and academic preparation at NSBM Green University", "Practical programming and problem-solving capability"],
            improvement_areas=["Quantify accomplishments and user impact metrics across project descriptions", "Target specialized industry certifications to validate production readiness"],
            recommended_skills_to_focus=["Cloud Platforms (AWS/Azure)", "CI/CD & DevOps Automation", "Automated Testing"],
            suggested_certifications=["AWS Certified Cloud Practitioner", "Professional Role-Specific Certification"],
            extracted_skills=extracted or ["Software Engineering"]
        )
