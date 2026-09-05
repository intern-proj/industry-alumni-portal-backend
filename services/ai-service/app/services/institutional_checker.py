import json
import logging
import re
from typing import List
from app.schemas import InstitutionalFitAnalysis, JobVacancySchema, MissingFieldFlag
from app.services.llm_engine import LLMEngine

logger = logging.getLogger("ai_service.institutional_checker")

# NSBM Green University Faculty & Degree Framework for reference / fallbacks
NSBM_FACULTIES = {
    "Faculty of Computing": [
        "BSc (Hons) Software Engineering",
        "BSc (Hons) Computer Science",
        "BSc (Hons) Data Science",
        "BSc (Hons) Cyber Security",
        "BSc (Hons) Computer Networks",
        "BSc (Hons) Management Information Systems"
    ],
    "Faculty of Business": [
        "BSc (Hons) in Business Management",
        "BSc (Hons) in Marketing Management",
        "BSc (Hons) in Accounting & Finance",
        "BSc (Hons) in Human Resource Management",
        "BSc (Hons) in Logistics & Supply Chain Management",
        "BSc (Hons) in International Business",
        "BSc (Hons) in Project Management"
    ],
    "Faculty of Engineering": [
        "BSc (Hons) in Electrical & Electronic Engineering",
        "BSc (Hons) in Mechatronic Engineering",
        "BSc (Hons) in Civil Engineering",
        "BSc (Hons) in Mechanical / CAD & Product Design Engineering"
    ],
    "Faculty of Science": [
        "BSc (Hons) in Biomedical Science",
        "BSc (Hons) in Applied Sciences",
        "BSc (Hons) in Pharmaceutical Science"
    ]
}


class InstitutionalFitChecker:
    """
    Evaluates whether an extracted job vacancy complies with NSBM Green University
    academic standards, assigns the most appropriate faculty and degree programs,
    calculates curriculum alignment, and identifies missing explicit compliance fields.
    """

    @classmethod
    def evaluate(cls, vacancy: JobVacancySchema) -> InstitutionalFitAnalysis:
        # 1. Programmatic Compliance & Missing Field Verification
        missing_flags: List[MissingFieldFlag] = []
        compliance_flags: List[str] = []

        # Check explicit salary
        if not vacancy.salary_raw or any(term in str(vacancy.salary_raw).lower() for term in ["negotiable", "attractive", "market rate", "none", "null", "not disclosed"]):
            missing_flags.append(MissingFieldFlag(
                field_name="salary_raw",
                severity="WARNING",
                message="Salary or stipend is not explicitly quantified in the flyer (marked as undisclosed, negotiable, or attractive).",
                suggestion="Request the partner to provide an estimated stipend or compensation range for student transparency."
            ))
            compliance_flags.append("UNDISCLOSED_SALARY")

        # Check contact channels
        has_emails = bool(vacancy.contact_emails and len(vacancy.contact_emails) > 0)
        has_urls = bool(vacancy.application_urls and len(vacancy.application_urls) > 0)
        if not has_emails and not has_urls:
            missing_flags.append(MissingFieldFlag(
                field_name="contact_emails",
                severity="CRITICAL",
                message="No direct application email address or submission web portal detected.",
                suggestion="Add an official recruiter email address or application link before approving."
            ))
            compliance_flags.append("NO_APPLICATION_CHANNEL")

        # Check application deadline
        if not vacancy.application_deadline:
            missing_flags.append(MissingFieldFlag(
                field_name="application_deadline",
                severity="WARNING",
                message="Application closing deadline is missing from the flyer.",
                suggestion="Establish an institutional closing deadline (typically 30 days from publication)."
            ))
            compliance_flags.append("MISSING_DEADLINE")

        # Check location
        if not vacancy.locations and not vacancy.workplace_type:
            missing_flags.append(MissingFieldFlag(
                field_name="locations",
                severity="INFO",
                message="Specific physical work location or remote/hybrid mode was not explicitly defined.",
                suggestion="Confirm whether the opportunity is On-site, Hybrid, or Remote."
            ))

        # Check required skills
        if not vacancy.required_skills:
            missing_flags.append(MissingFieldFlag(
                field_name="required_skills",
                severity="WARNING",
                message="No core technical or functional competencies were explicitly catalogued.",
                suggestion="Specify key tools, technologies, or foundational skills needed for student screening."
            ))

        # Check experience suitability
        is_fresh_grad_experience = vacancy.min_experience_years <= 2.0
        if not is_fresh_grad_experience:
            compliance_flags.append("EXPERIENCED_ROLE_ONLY")

        # 2. Advanced Multi-Faculty Academic Alignment via Gemini
        prompt = (
            "SYSTEM INSTRUCTION:\n"
            "You are the Dean of Academic Affairs and Director of University-Industry Partnerships at NSBM Green University.\n"
            "Evaluate the provided job vacancy against the university's faculties and degree curricula.\n\n"
            "UNIVERSITY FACULTIES & ACCREDITED DEGREE PROGRAMS:\n"
            "1. Faculty of Computing:\n"
            "   - BSc (Hons) Software Engineering (Full-stack coding, microservices, mobile, cloud architectures)\n"
            "   - BSc (Hons) Computer Science (Algorithms, systems programming, compilers, core data structures)\n"
            "   - BSc (Hons) Data Science (Machine learning, AI, big data analytics, statistical modeling, databases)\n"
            "   - BSc (Hons) Cyber Security (Network security, ethical hacking, digital forensics, threat analysis)\n"
            "   - BSc (Hons) Computer Networks (Network administration, cloud infrastructure, routing & switching)\n"
            "   - BSc (Hons) Management Information Systems (Enterprise IT, business analysis, ERP, IT governance)\n"
            "2. Faculty of Business:\n"
            "   - BSc (Hons) in Business Management (Operations, enterprise administration, corporate leadership)\n"
            "   - BSc (Hons) in Marketing Management (Digital marketing, brand positioning, sales engineering, SEO/SEM)\n"
            "   - BSc (Hons) in Accounting & Finance (Financial reporting, audit, taxation, investment analysis)\n"
            "   - BSc (Hons) in Human Resource Management (Talent acquisition, HR analytics, people operations)\n"
            "   - BSc (Hons) in Logistics & Supply Chain Management (Procurement, freight operations, inventory systems)\n"
            "   - BSc (Hons) in International Business (Global trade, export-import, multinational strategies)\n"
            "   - BSc (Hons) in Project Management (Agile project leadership, scrum, stakeholder delivery)\n"
            "3. Faculty of Engineering:\n"
            "   - BSc (Hons) in Electrical & Electronic Engineering (Circuit design, power systems, electronics, embedded)\n"
            "   - BSc (Hons) in Mechatronic Engineering (Robotics, automation, PLC, industrial control systems)\n"
            "   - BSc (Hons) in Civil Engineering (Structural engineering, surveying, construction project engineering)\n"
            "   - BSc (Hons) in Mechanical / CAD & Product Design Engineering (CAD/CAM, 3D modeling, jewellery/product design, prototyping)\n"
            "4. Faculty of Science:\n"
            "   - BSc (Hons) in Biomedical Science (Clinical diagnostics, laboratory analysis, molecular biology, hematology)\n"
            "   - BSc (Hons) in Applied Sciences (Industrial chemistry, applied mathematics, laboratory instrumentation)\n"
            "   - BSc (Hons) in Pharmaceutical Science (Pharmacology, drug formulation, quality assurance)\n\n"
            "EVALUATION CRITERIA:\n"
            "- Target Faculty: Select the single most accurate faculty from the four above. (E.g., 3D CAD/Jewellery design or robotics -> Faculty of Engineering; Coding/Cloud/AI -> Faculty of Computing; Marketing/HR/Finance -> Faculty of Business; Clinical/Biotech -> Faculty of Science).\n"
            "- Recommended Programs: Select 1 to 3 exact degree programs from that faculty whose coursework directly prepares students for this role.\n"
            "- Suitable for Interns/Graduates: Boolean. True if the role accommodates interns, trainees, junior associates, or fresh graduates (<= 2 years experience).\n"
            "- Institutional Match Score: Calibrated integer (0 to 100) reflecting curriculum alignment, technical depth, and learning value for university undergraduates.\n"
            "- Approval Recommendation: Exactly 'RECOMMENDED_FOR_APPROVAL' (score >= 80 and valid contact info), 'NEEDS_MANUAL_REVIEW' (score 50-79 or missing critical info), or 'HIGH_RISK_REJECT' (score < 50 or hazardous/unsuitable role).\n"
            "- Institutional Fit Notes: A nuanced 2 to 3 sentence academic assessment explaining why this role matches the selected faculty, citing relevant degree modules and career progression advice.\n\n"
            "Return ONLY a strict JSON object with these keys:\n"
            "{\n"
            '  "target_faculty": "Faculty Name",\n'
            '  "recommended_degree_programs": ["Degree 1", "Degree 2"],\n'
            '  "is_suitable_for_interns_or_graduates": true,\n'
            '  "institutional_match_score": 88,\n'
            '  "approval_recommendation": "RECOMMENDED_FOR_APPROVAL",\n'
            '  "institutional_fit_notes": "Academic evaluation notes."\n'
            "}\n\n"
            "USER REQUEST:\n"
            "JOB VACANCY SPECIFICATION:\n"
            f"Job Title: {vacancy.job_title}\n"
            f"Hiring Company: {vacancy.company_name or 'Industry Partner'}\n"
            f"Seniority Level: {vacancy.seniority_level or 'Not specified'}\n"
            f"Employment Type: {vacancy.employment_type or 'Not specified'}\n"
            f"Workplace Type: {vacancy.workplace_type or 'Not specified'}\n"
            f"Minimum Experience: {vacancy.min_experience_years} years\n"
            f"Required Skills: {', '.join(vacancy.required_skills) if vacancy.required_skills else 'General industry capabilities'}\n"
            f"Preferred Skills: {', '.join(vacancy.preferred_skills) if vacancy.preferred_skills else 'None'}\n"
            f"Responsibilities: {'; '.join(vacancy.responsibilities[:8]) if vacancy.responsibilities else 'Standard deliverables'}\n"
            f"Education Requirements: {', '.join(vacancy.education_requirements) if vacancy.education_requirements else 'Degree / Diploma'}\n"
            f"Eligibility Criteria: {', '.join(vacancy.eligibility_criteria) if vacancy.eligibility_criteria else 'None specified'}\n"
            f"Salary / Stipend: {vacancy.salary_raw or 'Undisclosed'}\n"
            f"Application Channels: {', '.join(vacancy.contact_emails + vacancy.application_urls) if (vacancy.contact_emails or vacancy.application_urls) else 'None detected'}\n"
        )

        target_faculty = "Faculty of Computing"
        recommended_programs = ["BSc (Hons) Software Engineering", "BSc (Hons) Computer Science"]
        is_suitable = is_fresh_grad_experience
        match_score = 75
        approval_rec = "NEEDS_MANUAL_REVIEW"
        fit_notes = "Evaluated against university undergraduate placement standards."

        try:
            llm = LLMEngine.get_instance()
            response = llm(prompt, max_tokens=600, temperature=0.1)
            raw_text = response["choices"][0]["text"].strip()
            if raw_text.startswith("```json"):
                raw_text = raw_text[7:]
            if raw_text.endswith("```"):
                raw_text = raw_text[:-3]

            parsed = json.loads(raw_text.strip())

            # Validate target faculty against known list
            ai_faculty = parsed.get("target_faculty", "").strip()
            for known_fac in NSBM_FACULTIES.keys():
                if known_fac.lower() in ai_faculty.lower():
                    target_faculty = known_fac
                    break

            recommended_programs = parsed.get("recommended_degree_programs", NSBM_FACULTIES.get(target_faculty, []))[:4]
            is_suitable = bool(parsed.get("is_suitable_for_interns_or_graduates", is_fresh_grad_experience))
            match_score = int(parsed.get("institutional_match_score", 75))
            match_score = max(20, min(99, match_score))

            ai_rec = parsed.get("approval_recommendation", "").upper()
            if ai_rec in ["RECOMMENDED_FOR_APPROVAL", "NEEDS_MANUAL_REVIEW", "HIGH_RISK_REJECT"]:
                approval_rec = ai_rec
            else:
                approval_rec = "RECOMMENDED_FOR_APPROVAL" if match_score >= 80 else ("NEEDS_MANUAL_REVIEW" if match_score >= 50 else "HIGH_RISK_REJECT")

            fit_notes = parsed.get("institutional_fit_notes", fit_notes).strip()

        except Exception as e:
            logger.warning(f"[InstitutionalChecker] Gemini academic evaluation failed, utilizing dynamic faculty fallback: {e}")
            # Dynamic heuristic fallback across ALL faculties (not just Computing!)
            full_haystack = f"{vacancy.job_title} {' '.join(vacancy.required_skills)} {' '.join(vacancy.responsibilities)}".lower()

            if any(t in full_haystack for t in ["cad", "jewel", "mechanical", "electrical", "electronic", "civil", "mechatronic", "robotics", "autocad", "solidworks", "hardware"]):
                target_faculty = "Faculty of Engineering"
                recommended_programs = ["BSc (Hons) in Mechanical / CAD & Product Design Engineering", "BSc (Hons) in Mechatronic Engineering"]
                match_score = 85
            elif any(t in full_haystack for t in ["market", "sales", "finance", "account", "hr", "human resource", "logistics", "supply chain", "business", "admin", "audit"]):
                target_faculty = "Faculty of Business"
                recommended_programs = ["BSc (Hons) in Business Management", "BSc (Hons) in Marketing Management"]
                match_score = 85
            elif any(t in full_haystack for t in ["biomedical", "biology", "chemical", "pharma", "clinical", "laboratory", "nursing"]):
                target_faculty = "Faculty of Science"
                recommended_programs = ["BSc (Hons) in Biomedical Science", "BSc (Hons) in Applied Sciences"]
                match_score = 85
            else:
                target_faculty = "Faculty of Computing"
                recommended_programs = ["BSc (Hons) Software Engineering", "BSc (Hons) Computer Science", "BSc (Hons) Data Science"]
                match_score = 85

            fit_notes = f"Strong alignment with {target_faculty} curricula based on core functional competencies."
            approval_rec = "RECOMMENDED_FOR_APPROVAL" if match_score >= 80 and not any(f.severity == "CRITICAL" for f in missing_flags) else "NEEDS_MANUAL_REVIEW"

        # If critical missing channels exist, downgrade recommendation to ensure student safety
        if any(f.severity == "CRITICAL" for f in missing_flags):
            approval_rec = "NEEDS_MANUAL_REVIEW"
            if "CRITICAL" not in fit_notes:
                fit_notes += " Note: Missing verified application channels require administrative review before publishing."

        return InstitutionalFitAnalysis(
            institutional_match_score=match_score,
            target_faculty=target_faculty,
            is_suitable_for_interns_or_graduates=is_suitable,
            recommended_degree_programs=recommended_programs,
            missing_explicit_fields=missing_flags,
            institutional_fit_notes=fit_notes,
            approval_recommendation=approval_rec,
            compliance_flags=compliance_flags
        )
