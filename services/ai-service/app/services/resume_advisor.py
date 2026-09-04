import re
from typing import List, Optional
from app.schemas import CareerAdviceResponse, ResumeAnalysisRequest
from app.services.downloader import download_image_to_tempfile
from app.services.ocr_engine import OCREngine


class ResumeCareerAdvisorService:
    """
    Analyzes student/alumni resumes, identifies market gaps, and provides
    actionable career guidance tailored to their targeted job position.
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
        lower_resume = resume_content.lower()

        # Skill extraction
        extracted_skills = []
        known_skills = [
            "python", "java", "c++", "c#", "javascript", "typescript", "react", "angular",
            "vue", "node", "spring", "spring boot", "django", "fastapi", "sql", "postgresql",
            "mongodb", "docker", "kubernetes", "aws", "azure", "git", "ci/cd", "rest api",
            "graphql", "microservices", "unit testing", "agile", "scrum", "html", "css",
            "tailwind", "machine learning", "data structures", "algorithms"
        ]
        for sk in known_skills:
            if re.search(r'\b' + re.escape(sk) + r'\b', lower_resume):
                extracted_skills.append(sk.title())

        # Strength Areas
        strengths: List[str] = []
        if len(extracted_skills) >= 5:
            strengths.append(f"Strong diverse technical skillset across {len(extracted_skills)} tools ({', '.join(extracted_skills[:4])})")
        if any(term in lower_resume for term in ["project", "developed", "built", "implemented"]):
            strengths.append("Demonstrated hands-on project implementations and software development experience")
        if any(term in lower_resume for term in ["bsc", "degree", "nsbm", "university", "gpa"]):
            strengths.append("Solid academic background with accredited degree coursework")
        if any(term in lower_resume for term in ["intern", "trainee", "experience"]):
            strengths.append("Prior industry or internship exposure demonstrating practical workplace readiness")

        if not strengths:
            strengths.append("Foundational knowledge in computing and software engineering fundamentals")

        # Improvement Areas based on Modern Market Standards
        improvements: List[str] = []
        recommended_skills: List[str] = []
        certifications: List[str] = []

        # Check DevOps & Cloud
        if not any(sk in lower_resume for sk in ["docker", "kubernetes", "aws", "azure", "gcp", "ci/cd"]):
            improvements.append("Add containerization (Docker) and Cloud deployment (AWS/Azure) to showcase production-readiness.")
            recommended_skills.extend(["Docker", "AWS Essentials", "CI/CD Pipelines (GitHub Actions)"])
            certifications.append("AWS Certified Cloud Practitioner")

        # Check Automated Testing & Quality
        if not any(sk in lower_resume for sk in ["junit", "jest", "unit test", "mock", "cypress", "selenium"]):
            improvements.append("Highlight automated unit and integration testing frameworks (JUnit/Jest/Mockito) to prove code quality.")
            recommended_skills.append("Unit Testing & Test-Driven Development (TDD)")

        # Check System Design & Architecture
        if not any(sk in lower_resume for sk in ["microservices", "system design", "redis", "kafka", "rabbitmq"]):
            improvements.append("Include message queues (RabbitMQ/Kafka) or distributed caching (Redis) in your portfolio projects.")
            recommended_skills.extend(["Redis Caching", "RabbitMQ Message Broker"])

        # Check Quantifiable Metrics
        if not re.search(r'\b\d+%\b|\b\d+x\b|\bincreased\b|\breduced\b|\bimproved\b', lower_resume):
            improvements.append("Quantify accomplishments in project bullet points (e.g. 'Optimized query latency by 35%', 'Supported 500+ active users').")

        if not certifications:
            certifications.extend(["Oracle Certified Professional: Java SE", "Meta Front-End Developer Certificate"])

        # Market Readiness Score
        base_score = 60
        base_score += min(20, len(extracted_skills) * 3)
        if len(strengths) >= 3:
            base_score += 10
        if len(improvements) <= 2:
            base_score += 10

        competitiveness_score = max(35, min(95, base_score))

        summary = (
            f"Your profile shows solid foundational capabilities for {target_role} positions. "
            f"Targeting key industry differentiators like cloud containerization and automated testing will significantly elevate your recruitment conversion."
        )

        return CareerAdviceResponse(
            target_role=target_role,
            market_competitiveness_score=competitiveness_score,
            match_summary=summary,
            strength_areas=strengths,
            improvement_areas=improvements,
            recommended_skills_to_focus=list(set(recommended_skills))[:5],
            suggested_certifications=certifications,
            extracted_skills=extracted_skills
        )
