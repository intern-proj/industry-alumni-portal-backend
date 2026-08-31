import re
from typing import List, Tuple
from app.schemas import InstitutionalFitAnalysis, JobVacancySchema, MissingFieldFlag

FACULTY_COMPUTING_KEYWORDS = [
    "software", "developer", "engineer", "react", "spring", "java", "python",
    "javascript", "typescript", "fullstack", "backend", "frontend", "devops",
    "cloud", "aws", "azure", "docker", "kubernetes", "sql", "nosql", "database",
    "data science", "ai", "machine learning", "cyber", "security", "network",
    "ui", "ux", "mobile", "flutter", "react native", "android", "ios", "qa", "tester"
]

FACULTY_BUSINESS_KEYWORDS = [
    "business", "marketing", "digital marketing", "sales", "finance", "accounting",
    "audit", "hr", "human resource", "talent", "recruiter", "logistics", "supply chain",
    "management", "analyst", "operations", "customer relations", "client executive",
    "project management", "scrum master"
]

FACULTY_ENGINEERING_KEYWORDS = [
    "mechanical", "electrical", "electronic", "civil", "mechatronics", "hardware",
    "robotics", "embedded", "autocad", "solidworks", "construction", "site engineer",
    "automation", "plc", "scada"
]

FACULTY_SCIENCE_KEYWORDS = [
    "biomedical", "nursing", "laboratory", "clinical", "biotech", "microbiology",
    "chemist", "pharmacist", "data analyst science", "researcher"
]


class InstitutionalFitChecker:
    """
    Evaluates whether an extracted vacancy complies with NSBM Green University
    academic alignment and flags missing or ambiguous explicit fields.
    """

    @classmethod
    def evaluate(cls, vacancy: JobVacancySchema) -> InstitutionalFitAnalysis:
        full_text = " ".join([
            vacancy.job_title,
            " ".join(vacancy.required_skills),
            " ".join(vacancy.preferred_skills),
            " ".join(vacancy.responsibilities),
            " ".join(vacancy.education_requirements)
        ]).lower()

        # 1. Faculty Alignment Scoring
        comp_score = sum(1 for kw in FACULTY_COMPUTING_KEYWORDS if kw in full_text)
        biz_score = sum(1 for kw in FACULTY_BUSINESS_KEYWORDS if kw in full_text)
        eng_score = sum(1 for kw in FACULTY_ENGINEERING_KEYWORDS if kw in full_text)
        sci_score = sum(1 for kw in FACULTY_SCIENCE_KEYWORDS if kw in full_text)

        target_faculty = "Faculty of Computing"
        max_score = comp_score
        recommended_programs = ["BSc (Hons) Software Engineering", "BSc (Hons) Computer Science", "BSc (Hons) Data Science"]

        if biz_score > max_score:
            max_score = biz_score
            target_faculty = "Faculty of Business"
            recommended_programs = ["BSc (Hons) in Business Management", "BSc (Hons) in Marketing Management", "BSc (Hons) in Accounting & Finance"]
        elif eng_score > max_score:
            max_score = eng_score
            target_faculty = "Faculty of Engineering"
            recommended_programs = ["BSc (Hons) in Electrical & Electronic Engineering", "BSc (Hons) in Mechatronics", "BSc (Hons) in Civil Engineering"]
        elif sci_score > max_score:
            max_score = sci_score
            target_faculty = "Faculty of Science"
            recommended_programs = ["BSc (Hons) in Biomedical Science", "BSc (Hons) in Applied Sciences"]

        # 2. Experience Level Check (Internship / Fresh Grad Suitability)
        is_graduate_friendly = vacancy.min_experience_years <= 2.0 or any(
            term in full_text for term in ["intern", "trainee", "associate", "junior", "graduate", "entry", "freshers"]
        )

        # 3. Missing Explicit Field Checks
        missing_flags: List[MissingFieldFlag] = []
        compliance_flags: List[str] = []

        # Check explicit salary
        if not vacancy.salary_raw or any(term in str(vacancy.salary_raw).lower() for term in ["negotiable", "attractive", "market rate", "none", "not disclosed"]):
            missing_flags.append(MissingFieldFlag(
                field_name="salary_raw",
                severity="WARNING",
                message="Salary is not explicitly stated in the flyer (marked as undisclosed or negotiable).",
                suggestion="Request partner to specify estimated stipend/salary range for student transparency."
            ))
            compliance_flags.append("UNDISCLOSED_SALARY")

        # Check contact email
        if not vacancy.contact_emails and not vacancy.application_urls:
            missing_flags.append(MissingFieldFlag(
                field_name="contact_emails",
                severity="CRITICAL",
                message="No official email or application portal link detected on the flyer.",
                suggestion="Add explicit recruitment email or URL before publishing."
            ))
            compliance_flags.append("NO_APPLICATION_CHANNEL")

        # Check application deadline
        if not vacancy.application_deadline:
            missing_flags.append(MissingFieldFlag(
                field_name="application_deadline",
                severity="WARNING",
                message="Application deadline date is missing from flyer.",
                suggestion="Set a default 30-day closing deadline."
            ))
            compliance_flags.append("MISSING_DEADLINE")

        # Check location
        if not vacancy.locations and not vacancy.workplace_type:
            missing_flags.append(MissingFieldFlag(
                field_name="locations",
                severity="INFO",
                message="Specific work location or remote/on-site type was not explicitly defined.",
                suggestion="Confirm whether the position is On-site, Hybrid, or Remote."
            ))

        # Check required skills
        if not vacancy.required_skills:
            missing_flags.append(MissingFieldFlag(
                field_name="required_skills",
                severity="WARNING",
                message="No structured skills were explicitly identified.",
                suggestion="Ensure key technical or functional skills are listed."
            ))

        # 4. Calculate Overall Institutional Score
        base_score = 65
        if max_score >= 3:
            base_score += 20
        elif max_score >= 1:
            base_score += 10

        if is_graduate_friendly:
            base_score += 15
        else:
            base_score -= 10

        if compliance_flags:
            base_score -= (len(compliance_flags) * 5)

        institutional_match_score = max(20, min(98, base_score))

        # Approval recommendation
        if institutional_match_score >= 80 and not any(f.severity == "CRITICAL" for f in missing_flags):
            approval_recommendation = "RECOMMENDED_FOR_APPROVAL"
            fit_notes = f"High alignment with {target_faculty} curricula. Ideal for student interns and fresh graduates."
        elif institutional_match_score >= 50:
            approval_recommendation = "NEEDS_MANUAL_REVIEW"
            fit_notes = f"Moderate fit with {target_faculty}. Please review missing fields before making live."
        else:
            approval_recommendation = "HIGH_RISK_REJECT"
            fit_notes = "Low alignment with university faculties or missing critical contact information."

        return InstitutionalFitAnalysis(
            institutional_match_score=institutional_match_score,
            target_faculty=target_faculty,
            is_suitable_for_interns_or_graduates=is_graduate_friendly,
            recommended_degree_programs=recommended_programs,
            missing_explicit_fields=missing_flags,
            institutional_fit_notes=fit_notes,
            approval_recommendation=approval_recommendation,
            compliance_flags=compliance_flags
        )
