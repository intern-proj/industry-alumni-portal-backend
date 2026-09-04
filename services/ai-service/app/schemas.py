from enum import Enum
from typing import Any, Dict, List, Literal, Optional
from pydantic import BaseModel, Field, HttpUrl


class MissingFieldFlag(BaseModel):
    field_name: str
    severity: Literal["WARNING", "CRITICAL", "INFO"]
    message: str
    suggestion: str


class InstitutionalFitAnalysis(BaseModel):
    institutional_match_score: int = Field(..., ge=0, le=100, description="Match score with NSBM faculties and undergraduate profile")
    target_faculty: str = Field(..., description="e.g., Faculty of Computing, Faculty of Business, Faculty of Engineering, Faculty of Science")
    is_suitable_for_interns_or_graduates: bool
    recommended_degree_programs: List[str] = Field(default_factory=list)
    missing_explicit_fields: List[MissingFieldFlag] = Field(default_factory=list)
    institutional_fit_notes: str
    approval_recommendation: Literal["RECOMMENDED_FOR_APPROVAL", "NEEDS_MANUAL_REVIEW", "HIGH_RISK_REJECT"]
    compliance_flags: List[str] = Field(default_factory=list)


class JobVacancySchema(BaseModel):
    job_title: str
    seniority_level: Optional[str] = None
    employment_type: Optional[str] = None
    company_name: Optional[str] = None
    workplace_type: Optional[str] = None
    locations: List[str] = Field(default_factory=list)
    min_experience_years: float = 0.0
    education_requirements: List[str] = Field(default_factory=list)
    required_skills: List[str] = Field(default_factory=list)
    preferred_skills: List[str] = Field(default_factory=list)
    responsibilities: List[str] = Field(default_factory=list)
    eligibility_criteria: List[str] = Field(default_factory=list)
    salary_raw: Optional[str] = None
    application_deadline: Optional[str] = None
    contact_emails: List[str] = Field(default_factory=list)
    contact_phones: List[str] = Field(default_factory=list)
    application_urls: List[str] = Field(default_factory=list)


class VacancyParseRequest(BaseModel):
    image_url: HttpUrl = Field(..., description="Public or presigned URL of the job flyer image or PDF")
    partner_id: Optional[str] = Field(None, description="ID of partner posting the vacancy")
    callback_url: Optional[HttpUrl] = Field(None, description="Optional webhook URL to notify upon completion")


class VacancyParseResponse(BaseModel):
    status: str = "success"
    vacancy_id: str
    extracted_data: JobVacancySchema
    institutional_analysis: InstitutionalFitAnalysis
    processing_time_seconds: float


# ==========================================
# SMART SEARCH SCHEMAS
# ==========================================

class SmartSearchRequest(BaseModel):
    query: str = Field(..., min_length=2, description="Natural language search prompt, e.g., 'find me vacancy within colombo that pays more than 100000LKR for software engineering graduate'")
    search_type: Literal["vacancies", "candidates"] = "vacancies"
    limit: int = Field(20, ge=1, le=100)
    items_to_rank: Optional[List[Dict[str, Any]]] = Field(default=None, description="Optional list of items to filter & rank")


class SmartSearchParsedIntent(BaseModel):
    raw_query: str
    target_role: Optional[str] = None
    locations: List[str] = Field(default_factory=list)
    min_salary: Optional[float] = None
    currency: Optional[str] = None
    required_skills: List[str] = Field(default_factory=list)
    experience_level: Optional[str] = None
    workplace_type: Optional[str] = None
    faculty: Optional[str] = None
    keywords: List[str] = Field(default_factory=list)


class SmartSearchResultItem(BaseModel):
    id: Any
    item: Dict[str, Any]
    match_score: int = Field(..., ge=0, le=100)
    highlight_reasons: List[str] = Field(default_factory=list)


class SmartSearchResponse(BaseModel):
    status: str = "success"
    search_type: str
    parsed_intent: SmartSearchParsedIntent
    results: List[SmartSearchResultItem]
    total_found: int


# ==========================================
# UNIVERSAL MULTI-DOMAIN AI SEARCH SCHEMAS
# ==========================================

class DomainIntentEnum(str, Enum):
    VACANCIES = "vacancies"
    COMPANIES = "companies"
    STUDENTS = "students"
    UNKNOWN = "unknown"


class FrontendActionDirective(BaseModel):
    action: str = Field(default="DISPLAY_RESULTS", description="DISPLAY_RESULTS, NAVIGATE_AND_FILTER, or SHOW_OVERLAY")
    target_domain: str = Field(default="vacancies", description="vacancies, companies, or students")
    suggested_route: str = Field(default="/student/vacancies")
    headline: str = Field(default="")
    explanation: str = Field(default="")
    badge_label: Optional[str] = None


class UniversalSearchRequest(BaseModel):
    query: str = Field(..., min_length=2, description="Natural language search prompt across portal domains")
    current_route: Optional[str] = Field(default=None, description="Optional current frontend route to inform context")
    user_role: Optional[str] = Field(default="STUDENT", description="Role of the querying user (STUDENT, INDUSTRY_PARTNER, STAFF, GUEST)")
    limit: int = Field(20, ge=1, le=100)


class UniversalSearchResultItem(BaseModel):
    id: Any
    domain: str
    item: Dict[str, Any]
    match_score: int = Field(..., ge=0, le=100)
    highlight_reasons: List[str] = Field(default_factory=list)


class UniversalSearchResponse(BaseModel):
    status: str = "success"
    detected_domain: str
    parsed_intent: SmartSearchParsedIntent
    directive: FrontendActionDirective
    results: List[UniversalSearchResultItem]
    total_found: int


# ==========================================
# RESUME & CAREER ADVISORY SCHEMAS
# ==========================================

class ResumeAnalysisRequest(BaseModel):
    resume_text: Optional[str] = Field(None, description="Raw text of the candidate's resume")
    resume_url: Optional[HttpUrl] = Field(None, description="Downloadable URL for resume file")
    candidate_id: Optional[str] = None
    target_job_position: Optional[str] = Field(None, description="Desired role, e.g. 'Software Engineer', 'Data Analyst'")


class CareerAdviceResponse(BaseModel):
    target_role: str
    candidate_name: Optional[str] = None
    market_competitiveness_score: int = Field(..., ge=0, le=100, description="Overall career readiness score in Sri Lankan & global tech market")
    match_summary: str
    strength_areas: List[str] = Field(default_factory=list)
    improvement_areas: List[str] = Field(default_factory=list)
    recommended_skills_to_focus: List[str] = Field(default_factory=list)
    suggested_certifications: List[str] = Field(default_factory=list)
    extracted_skills: List[str] = Field(default_factory=list)


# ==========================================
# MATCH PERCENTAGE & RECOMMENDATION SCHEMAS
# ==========================================

class CandidateVacancyMatchRequest(BaseModel):
    candidate_skills: List[str] = Field(default_factory=list)
    candidate_experience_years: float = 0.0
    candidate_education: Optional[str] = None
    preferred_locations: List[str] = Field(default_factory=list)
    vacancies: List[Dict[str, Any]] = Field(..., description="List of available vacancies to evaluate")


class MatchedVacancyItem(BaseModel):
    vacancy_id: Any
    title: str
    company_name: str
    match_percentage: int = Field(..., ge=0, le=100)
    matched_skills: List[str] = Field(default_factory=list)
    missing_skills: List[str] = Field(default_factory=list)
    fit_summary: str


class CandidateVacancyMatchResponse(BaseModel):
    status: str = "success"
    matched_vacancies: List[MatchedVacancyItem]
    total_evaluated: int


class RecruiterCandidateMatchRequest(BaseModel):
    vacancy_title: str
    required_skills: List[str] = Field(default_factory=list)
    preferred_skills: List[str] = Field(default_factory=list)
    min_experience_years: float = 0.0
    location: Optional[str] = None
    candidates: List[Dict[str, Any]] = Field(..., description="List of candidates to evaluate for this vacancy")


class MatchedCandidateItem(BaseModel):
    candidate_id: Any
    full_name: str
    email: Optional[str] = None
    job_search_status: str
    actively_searching: bool
    match_percentage: int = Field(..., ge=0, le=100)
    matched_skills: List[str] = Field(default_factory=list)
    missing_skills: List[str] = Field(default_factory=list)
    recommendation_note: str


class RecruiterCandidateMatchResponse(BaseModel):
    status: str = "success"
    vacancy_title: str
    matched_candidates: List[MatchedCandidateItem]
    total_active_candidates_evaluated: int


# ==========================================
# ADVANCED ATS 4-PILLAR RESUME MATCHING SCHEMAS
# ==========================================

class ATSScoreBreakdown(BaseModel):
    skills_coverage: int = Field(..., ge=0, le=100)
    semantic_alignment: int = Field(..., ge=0, le=100)
    cross_encoder_score: int = Field(..., ge=0, le=100)
    institutional_fit: int = Field(..., ge=0, le=100)


class ResumeVacancyMatchRequest(BaseModel):
    resume_url: Optional[str] = Field(None, description="Direct URL of the student's resume PDF/file")
    resume_text: Optional[str] = Field(None, description="Optional raw text if already extracted")
    user_id: Optional[str] = None
    candidate_skills: List[str] = Field(default_factory=list, description="Verified profile skills to use as fallback/enrichment")
    preferred_locations: List[str] = Field(default_factory=list)
    vacancies: List[Dict[str, Any]] = Field(..., description="List of available vacancies to evaluate")
    force_refresh: bool = False


class ResumeVacancyMatchResponse(BaseModel):
    status: str = "success"
    extracted_skills: List[str] = Field(default_factory=list)
    matched_vacancies: List[MatchedVacancyItem] = Field(default_factory=list)
    total_evaluated: int = 0


class SingleApplicantMatchRequest(BaseModel):
    resume_url: Optional[str] = None
    resume_text: Optional[str] = None
    cover_letter: Optional[str] = None
    candidate_skills: List[str] = Field(default_factory=list, description="Verified profile skills")
    vacancy_id: Optional[Any] = None
    vacancy_title: str
    vacancy_description: Optional[str] = None
    vacancy_requirements: Optional[str] = None
    vacancy_tags: Optional[str] = None
    candidate_name: Optional[str] = None
    candidate_email: Optional[str] = None
    candidate_faculty: Optional[str] = None


class SingleApplicantMatchResponse(BaseModel):
    match_percentage: int = Field(..., ge=0, le=100)
    matched_skills: List[str] = Field(default_factory=list)
    missing_skills: List[str] = Field(default_factory=list)
    fit_summary: str
    strong_fortes: List[str] = Field(default_factory=list)
    score_breakdown: Optional[ATSScoreBreakdown] = None
    match_tier: str = "STRONG_MATCH"


class ApplicantMatchItem(BaseModel):
    applicant_id: Any
    student_name: Optional[str] = None
    student_email: Optional[str] = None
    resume_url: Optional[str] = None
    match_percentage: int = Field(..., ge=0, le=100)
    matched_skills: List[str] = Field(default_factory=list)
    missing_skills: List[str] = Field(default_factory=list)
    fit_summary: str
    score_breakdown: ATSScoreBreakdown


class BulkApplicantMatchRequest(BaseModel):
    vacancy_id: Optional[Any] = None
    vacancy_title: str
    vacancy_description: Optional[str] = None
    vacancy_requirements: Optional[str] = None
    vacancy_tags: Optional[str] = None
    applicants: List[Dict[str, Any]] = Field(..., description="List of applicants with resume_url/id")


class BulkApplicantMatchResponse(BaseModel):
    status: str = "success"
    vacancy_title: str
    ranked_applicants: List[ApplicantMatchItem] = Field(default_factory=list)
    total_evaluated: int = 0


class ProfileRebuildRequest(BaseModel):
    user_id: str
    resume_urls: List[str] = Field(default_factory=list)
    profile_skills: List[str] = Field(default_factory=list)


class CandidateProjectSchema(BaseModel):
    title: str = Field(..., description="Project name")
    tech_stack: List[str] = Field(default_factory=list, description="Technologies and frameworks utilized")
    description: str = Field(default="", description="Key implementation features, responsibilities, or impact")


class StructuredResumeSchema(BaseModel):
    candidate_name: Optional[str] = None
    target_roles: List[str] = Field(default_factory=list)
    seniority_level: Optional[str] = None
    experience_years: float = 0.0
    education: Optional[str] = None
    faculty: Optional[str] = None
    skills: List[str] = Field(default_factory=list)
    projects: List[CandidateProjectSchema] = Field(default_factory=list)
    bio: Optional[str] = None
    certifications: List[str] = Field(default_factory=list)
    languages: List[str] = Field(default_factory=list)


class EnhanceProfileRequest(BaseModel):
    user_id: str
    resume_url: Optional[str] = None
    fallback_skills: Optional[List[str]] = Field(default_factory=list)


class EnhanceProfileResponse(BaseModel):
    status: str = "success"
    user_id: str
    candidate_name: Optional[str] = None
    bio: Optional[str] = None
    target_roles: List[str] = Field(default_factory=list)
    skills: List[str] = Field(default_factory=list)
    projects: List[CandidateProjectSchema] = Field(default_factory=list)
    education: Optional[str] = None
    faculty: Optional[str] = None
    experience_years: float = 0.0


class CoverLetterRequest(BaseModel):
    candidate_name: str
    candidate_skills: List[str] = Field(default_factory=list)
    vacancy_title: str
    company_name: str
    vacancy_requirements: str


class CoverLetterResponse(BaseModel):
    cover_letter_html: str
