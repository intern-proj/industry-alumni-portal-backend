import json
import logging
import re
import threading
import urllib.request
import urllib.error
from typing import Optional, List, Dict, Any

from app.config import settings
from app.schemas import JobVacancySchema, CoverLetterRequest

logger = logging.getLogger("ai_service.llm_engine")
_llm_lock = threading.Lock()

GEMINI_MODELS = [
    "gemini-3.1-flash-lite",
    "gemini-3.5-flash-lite",
    "gemini-3-flash-preview",
    "gemini-flash-lite-latest",
]


class LLMEngine:
    """
    High-performance LLM Engine powered by Google Gemini API.
    Replaces heavy local GGUF models on serverless / cloud deployments without GPU.
    Provides full backwards-compatible callable interface: llm(prompt, ...) -> {"choices": [{"text": ...}]}
    """
    _instance = None
    _active_config = None

    def __init__(self):
        self.api_key = getattr(settings, "GEMINI_API_KEY", "AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg")
        self.default_model = getattr(settings, "GEMINI_MODEL", "gemini-3.1-flash-lite")
        logger.info(f"[LLM Service] Initialized Gemini Cloud LLM Engine with default model '{self.default_model}'.")

    def reset(self):
        """No-op for compatibility with previous llama_cpp reset calls."""
        pass

    @classmethod
    def reload_instance(cls):
        with _llm_lock:
            cls._instance = None
            cls._active_config = None
            logger.info("[LLM Service] Engine instance reloaded.")

    @classmethod
    def get_instance(cls):
        """Singleton pattern for the LLM Engine."""
        if cls._instance is None:
            with _llm_lock:
                if cls._instance is None:
                    cls._instance = LLMEngine()
        return cls._instance

    @staticmethod
    def _clean_prompt(prompt: str) -> str:
        """Strips ChatML tokens (<|im_start|>, <|im_end|>) from prompts for clean Gemini processing."""
        text = re.sub(r"<\|im_start\|>system\s*", "SYSTEM INSTRUCTION:\n", prompt)
        text = re.sub(r"<\|im_start\|>user\s*", "\nUSER REQUEST:\n", text)
        text = re.sub(r"<\|im_start\|>assistant\s*", "\nASSISTANT RESPONSE:\n", text)
        text = text.replace("<|im_end|>", "\n")
        return text.strip()

    def _call_gemini_api(
        self,
        prompt: str,
        json_mode: bool = False,
        max_tokens: int = 3500,
        temperature: float = 0.2,
        stop_sequences: Optional[List[str]] = None
    ) -> str:
        """Dispatches prompt to Gemini API with automatic model failover and zero-thinking budget for fast JSON."""
        clean_text = self._clean_prompt(prompt)
        effective_max_tokens = max(max_tokens, 3500)

        payload = {
            "contents": [{"parts": [{"text": clean_text}]}],
            "generationConfig": {
                "temperature": temperature,
                "maxOutputTokens": effective_max_tokens,
                "thinkingConfig": {
                    "thinkingBudget": 0
                }
            }
        }

        if json_mode:
            payload["generationConfig"]["responseMimeType"] = "application/json"

        if stop_sequences:
            # Filter out ChatML markers or backticks if json mode is active
            valid_stops = [s for s in stop_sequences if s and "<|" not in s and "```" not in s]
            if valid_stops:
                payload["generationConfig"]["stopSequences"] = valid_stops[:5]

        models_to_try = [self.default_model] + [m for m in GEMINI_MODELS if m != self.default_model]

        last_error = None
        for model_name in models_to_try:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={self.api_key}"
            
            # First try with thinkingBudget: 0
            body = json.dumps(payload).encode("utf-8")
            req = urllib.request.Request(
                url,
                data=body,
                headers={"Content-Type": "application/json"}
            )
            try:
                with urllib.request.urlopen(req, timeout=12) as resp:
                    resp_data = json.loads(resp.read().decode("utf-8"))
                    candidates = resp_data.get("candidates", [])
                    if candidates:
                        parts = candidates[0].get("content", {}).get("parts", [])
                        if parts:
                            return parts[0].get("text", "").strip()
            except urllib.error.HTTPError as http_err:
                err_body = http_err.read().decode("utf-8", errors="ignore")
                # If thinkingConfig was rejected with 400 Bad Request, retry without it
                if http_err.code == 400 and "thinkingConfig" in payload["generationConfig"]:
                    try:
                        fallback_payload = dict(payload)
                        fallback_gen = dict(payload["generationConfig"])
                        fallback_gen.pop("thinkingConfig", None)
                        fallback_payload["generationConfig"] = fallback_gen
                        fallback_body = json.dumps(fallback_payload).encode("utf-8")
                        fallback_req = urllib.request.Request(
                            url,
                            data=fallback_body,
                            headers={"Content-Type": "application/json"}
                        )
                        with urllib.request.urlopen(fallback_req, timeout=12) as fb_resp:
                            resp_data = json.loads(fb_resp.read().decode("utf-8"))
                            candidates = resp_data.get("candidates", [])
                            if candidates:
                                parts = candidates[0].get("content", {}).get("parts", [])
                                if parts:
                                    return parts[0].get("text", "").strip()
                    except Exception as fb_exc:
                        logger.warning(f"[LLM Service] Retry without thinkingConfig on '{model_name}' failed: {fb_exc}")

                logger.warning(f"[LLM Service] Gemini model '{model_name}' HTTP {http_err.code}: {err_body[:180]}")
                last_error = http_err
                continue
            except Exception as exc:
                logger.warning(f"[LLM Service] Gemini model '{model_name}' failed: {exc}")
                last_error = exc
                continue

        raise RuntimeError(f"All Gemini endpoints failed. Last error: {last_error}")

    def __call__(
        self,
        prompt: str,
        max_tokens: int = 3500,
        temperature: float = 0.2,
        stop: Optional[List[str]] = None,
        **kwargs
    ) -> Dict[str, Any]:
        """Callable interface matching llama_cpp output schema."""
        is_json = "json" in prompt.lower() or "```json" in prompt.lower()
        result_text = self._call_gemini_api(
            prompt=prompt,
            json_mode=is_json,
            max_tokens=max_tokens,
            temperature=temperature,
            stop_sequences=stop
        )
        return {"choices": [{"text": result_text}]}

    @staticmethod
    def extract_structured_json(raw_text: str) -> JobVacancySchema:
        """Extracts job vacancy details into strict JobVacancySchema JSON using advanced Gemini reasoning."""
        schema_json_str = json.dumps(JobVacancySchema.model_json_schema(), separators=(",", ":"))

        prompt = (
            "SYSTEM INSTRUCTION:\n"
            "You are a principal HR intelligence engine that extracts comprehensive, structured information from job flyer and recruitment document text.\n"
            "Extract ALL details into a strict JSON object adhering to this schema:\n"
            f"{schema_json_str}\n\n"
            "EXTRACTION & REASONING GUIDELINES:\n"
            "1. `company_name`: Identify the primary hiring company or organization name. Deduce the company name from top header branding, logo text, company mission statements, or recruitment greetings.\n"
            "   CRITICAL RULES FOR RECRUITMENT CONTACTS:\n"
            "   - NEVER use raw email usernames or addresses (such as 'hrdcareers942', 'hrdcareers942@gmail.com', 'careers', 'info', 'jobs') as the company name!\n"
            "   - If the flyer does not state the corporate name and only provides an anonymous email like 'hrdcareers942@gmail.com', look at how the organization describes itself (e.g., 'leading jewellery manufacturing company based in Colombo') and formulate a clean professional title like 'Leading Jewellery Manufacturing Company' or 'Jewellery Design Studio'.\n"
            "   - If the email domain is corporate (e.g., '@synnext.com' or '@voguejewellers.com'), extract the brand name ('Synnext' or 'Vogue Jewellers').\n"
            "   - NEVER output an ugly email username like 'hrdcareers942'.\n"
            "2. `job_title`: Extract the exact official job title cleanly (e.g., 'Executive Jewellery Designer – CAD', 'Associate Software Engineer', 'Digital Marketing Specialist'). Do not include noise words like 'We are hiring' or 'Urgent Vacancy'.\n"
            "3. `seniority_level`: Accurately infer seniority from the title and requirements: 'Intern', 'Trainee', 'Associate', 'Junior', 'Mid-Level', 'Senior', 'Lead', 'Executive', or 'Director'.\n"
            "4. `employment_type`: Extract or infer: 'Full-time', 'Part-time', 'Contract', or 'Internship'. If not mentioned, infer reasonably from context or return null.\n"
            "5. `workplace_type`: Extract or infer: 'ON_SITE', 'REMOTE', or 'HYBRID'. Look for office location mentions, remote flags, or work modes.\n"
            "6. `locations`: Extract physical workplace locations, cities, or addresses as separate strings (e.g., ['Colombo', 'Kandy']). Exclude email addresses, web URLs, and phone numbers from locations.\n"
            "7. `min_experience_years`: Minimum years of professional experience required as a float number. If the flyer says 'Freshers welcome', 'Intern', 'Trainee', or experience is not mentioned, set to 0.0.\n"
            "8. `education_requirements`: Academic degrees, diplomas, or qualifications requested (e.g., ['Degree or Diploma in Jewellery Design / CAD', 'BSc in Computer Science']).\n"
            "9. `required_skills`: Decompose all explicit technical competencies, tools, software packages, programming languages, and functional skills into individual strings (e.g., ['Rhino 3D', 'Matrix CAD', 'Jewellery Rendering', 'Photoshop'] or ['React', 'Spring Boot', 'PostgreSQL']).\n"
            "10. `preferred_skills`: Bonus, nice-to-have skills or secondary proficiencies.\n"
            "11. `responsibilities`: Extract each duty, task, and responsibility as an individual string.\n"
            "12. `eligibility_criteria`: Specific qualification prerequisites, year of study, portfolio requirements, or certifications.\n"
            "13. `salary_raw`: Any remuneration terms, salary figures, stipends, or allowances mentioned (e.g., 'Rs. 75,000 - 100,000 / month', 'Attractive Allowance', 'Negotiable').\n"
            "14. `application_deadline`: Closing date for applications if mentioned. Standardize to YYYY-MM-DD if possible or extract text.\n"
            "15. `contact_emails`: Extract all valid email addresses found.\n"
            "16. `contact_phones`: Extract all phone numbers found.\n"
            "17. `application_urls`: Extract all websites, application portal links, or registration URLs found.\n\n"
            "USER REQUEST:\n"
            f"Job Vacancy Flyer Text:\n{raw_text}\n"
        )

        try:
            engine = LLMEngine.get_instance()
            output_text = engine._call_gemini_api(
                prompt=prompt,
                json_mode=True,
                max_tokens=3500,
                temperature=0.0
            )
            output_text = output_text.strip()
            if output_text.startswith("```json"):
                output_text = output_text[7:]
            if output_text.endswith("```"):
                output_text = output_text[:-3]

            parsed = json.loads(output_text.strip())

            # Post-processing: Guarantee company_name is never left as literal 'None', empty, or raw email username
            comp = parsed.get("company_name")
            bad_comp = False
            if not comp or str(comp).strip().lower() in ["none", "null", "not specified", "n/a", "unknown"]:
                bad_comp = True
            elif "@" in str(comp) or any(term in str(comp).lower() for term in ["hrdcareers", "careers9", "jobs1", "recruitment@"]):
                bad_comp = True
            elif re.search(r'^(hr|hrd|careers|jobs|recruitment|admin|info)\d*$', str(comp).strip().lower()):
                bad_comp = True

            if bad_comp:
                inferred_comp = None
                emails = parsed.get("contact_emails") or []
                for email in emails:
                    if "@" in email:
                        domain = email.split("@")[-1].lower()
                        if not any(pub in domain for pub in ["gmail.", "yahoo.", "hotmail.", "outlook.", "live.", "icloud.", "mail."]):
                            domain_name = domain.split(".")[0]
                            if len(domain_name) > 2:
                                inferred_comp = domain_name.title()
                                break
                if not inferred_comp:
                    urls = parsed.get("application_urls") or []
                    for u in urls:
                        clean_u = re.sub(r'https?://(www\.)?', '', u.lower())
                        domain_name = clean_u.split("/")[0].split(".")[0]
                        if len(domain_name) > 2 and domain_name not in ["google", "forms", "linkedin", "facebook", "bit", "tinyurl"]:
                            inferred_comp = domain_name.title()
                            break
                if not inferred_comp:
                    # Synthesize from job title context
                    title = parsed.get("job_title", "")
                    if "jewel" in title.lower() or "cad" in title.lower():
                        inferred_comp = "Leading Jewellery Manufacturing Company"
                    elif "software" in title.lower() or "developer" in title.lower():
                        inferred_comp = "Technology Partner Organization"
                    else:
                        inferred_comp = "Industry Placement Partner"
                parsed["company_name"] = inferred_comp

            # Normalize salary_raw
            if parsed.get("salary_raw") and str(parsed.get("salary_raw")).strip().lower() in ["none", "null"]:
                parsed["salary_raw"] = None

            return JobVacancySchema.model_validate(parsed)
        except Exception as e:
            logger.error(f"[LLM Service] Structured extraction error: {e}")
            raise e

    @staticmethod
    def generate_cover_letter(request: CoverLetterRequest) -> str:
        """Generates a high-impact 3-paragraph executive cover letter in clean HTML."""
        skills_str = ", ".join(request.candidate_skills) if request.candidate_skills else "software engineering, modern technical frameworks, and agile problem solving"
        prompt = (
            "SYSTEM INSTRUCTION:\n"
            "You are an executive career strategist and placement director at NSBM Green University.\n"
            "Compose a high-impact, professional, 3-paragraph executive cover letter in clean HTML using only <p> and <strong> tags.\n"
            "Requirements:\n"
            "- Paragraph 1: Express compelling enthusiasm for the specific vacancy at the target company, citing the candidate's rigorous academic training at NSBM Green University.\n"
            "- Paragraph 2: Articulate verified technical competencies, hands-on software/system implementations, and direct alignment with the company's domain challenges.\n"
            "- Paragraph 3: Reiterate value proposition, agile problem-solving readiness, commitment to excellence, and express eagerness for an interview.\n"
            "- Tone: Confident, articulate, professional, and impact-driven. Maximum 130 words.\n\n"
            "USER REQUEST:\n"
            f"Candidate Name: {request.candidate_name or 'Undergraduate Applicant'}\n"
            f"Candidate Verified Skills: {skills_str}\n"
            f"Target Vacancy: {request.vacancy_title}\n"
            f"Target Company: {request.company_name}\n"
            f"Vacancy Requirements: {request.vacancy_requirements[:500] if request.vacancy_requirements else 'Industry technical standards'}\n"
        )

        try:
            engine = LLMEngine.get_instance()
            output_text = engine._call_gemini_api(
                prompt=prompt,
                json_mode=False,
                max_tokens=350,
                temperature=0.2
            )
            cleaned = output_text.strip()
            if cleaned.startswith("```html"):
                cleaned = cleaned[7:]
            if cleaned.startswith("```"):
                cleaned = cleaned[3:]
            if cleaned.endswith("```"):
                cleaned = cleaned[:-3]
            cleaned = cleaned.strip()

            if not cleaned.startswith("<p>"):
                cleaned = f"<p>Dear Hiring Team at <strong>{request.company_name}</strong>,</p>\n" + cleaned

            cleaned = cleaned.replace("’", "'").replace("‘", "'").replace("“", '"').replace("”", '"')
            if len(cleaned) > 50:
                return cleaned
        except Exception as e:
            logger.warning(f"[LLM Service] Cover letter generation error: {e}. Using synthesis fallback.")

        # Fallback template
        name = request.candidate_name or "Applicant"
        comp = request.company_name or "your team"
        role = request.vacancy_title or "Open Position"
        return (
            f"<p>Dear Hiring Team at <strong>{comp}</strong>,</p>\n"
            f"<p>I am writing to express my strong enthusiasm for the <strong>{role}</strong> opportunity. "
            f"As an undergraduate student at NSBM Green University with proven technical capability in "
            f"<strong>{skills_str}</strong>, I am eager to apply my practical skills, agile problem solving, and dedication to your organization.</p>\n"
            f"<p>Throughout my academic studies and hands-on project engineering, I have developed a solid foundation in scalable software architectures, "
            f"collaborative workflows, and continuous improvement. The competencies sought for the {role} role closely match my experience and career aspirations.</p>\n"
            f"<p>Thank you for considering my application. I welcome the opportunity to discuss how my competencies can deliver immediate impact to {comp}.</p>\n"
            f"<p>Sincerely,<br><strong>{name}</strong><br>NSBM Green University</p>"
        )

    @staticmethod
    def generate_applicant_insights(
        candidate_name: str,
        candidate_skills: list,
        resume_text: str,
        cover_letter: str,
        vacancy_title: str,
        vacancy_requirements: str,
        vacancy_description: str
    ) -> dict:
        """Evaluates applicant profile, resume, and cover letter against vacancy requirements using Gemini intelligence."""
        skills_str = ", ".join(candidate_skills) if candidate_skills else "Not specified"
        clean_cover_letter = re.sub(r'<[^>]+>', ' ', cover_letter or '')
        clean_cover_letter = ' '.join(clean_cover_letter.split())[:3500]
        clean_resume_text = ' '.join((resume_text or "").split())[:6000]
        clean_requirements = ' '.join((vacancy_requirements or "").split())[:2500]
        clean_description = ' '.join((vacancy_description or "").split())[:2500]

        prompt = (
            "SYSTEM INSTRUCTION:\n"
            "You are the Principal Talent Assessor and AI Recruitment Intelligence Engine for university-corporate partnerships.\n"
            "Perform a rigorous multi-dimensional technical evaluation of the applicant against the target vacancy requirements.\n\n"
            "EVALUATION CRITERIA:\n"
            "1. `summary`: A 2 to 4 sentence executive technical evaluation of the candidate. Detail their degree background at NSBM Green University, proven project engineering implementations, verified stack mastery, and readiness to deliver immediate value in this role.\n"
            "2. `strong_fortes`: A list of 3 to 5 comprehensive, highly specific bullet points detailing the candidate's strongest competitive advantages. Cite actual technologies mastered, specific projects completed, and direct alignment with the vacancy's core responsibilities.\n"
            "3. `match_percentage`: A calibrated, mathematically sound integer percentage (0 to 100) reflecting core competency alignment, project evidence, and role readiness.\n\n"
            "Return ONLY a strict JSON object:\n"
            "{\n"
            '  "summary": "Executive technical summary...",\n'
            '  "strong_fortes": ["Forte 1", "Forte 2", "Forte 3"],\n'
            '  "match_percentage": 85\n'
            "}\n\n"
            "USER REQUEST:\n"
            f"[TARGET VACANCY]\n"
            f"Title: {vacancy_title}\n"
            f"Requirements: {clean_requirements if clean_requirements else 'Industry standard technical requirements.'}\n"
            f"Description: {clean_description if clean_description else 'Standard engineering responsibilities.'}\n\n"
            f"[APPLICANT PROFILE]\n"
            f"Candidate Name: {candidate_name or 'Applicant'}\n"
            f"Verified Skills: {skills_str}\n\n"
            f"[SUBMITTED COVER LETTER / PITCH]\n"
            f"{clean_cover_letter if clean_cover_letter else 'No cover letter provided.'}\n\n"
            f"[EXTRACTED RESUME DOSSIER]\n"
            f"{clean_resume_text if clean_resume_text else 'No resume text available.'}\n"
        )

        try:
            engine = LLMEngine.get_instance()
            output_text = engine._call_gemini_api(
                prompt=prompt,
                json_mode=True,
                max_tokens=1200,
                temperature=0.1
            )
            output_text = output_text.strip()
            if output_text.startswith("```json"):
                output_text = output_text[7:]
            if output_text.endswith("```"):
                output_text = output_text[:-3]

            parsed = json.loads(output_text.strip())
            summary = parsed.get("summary", "").strip()
            strong_fortes = parsed.get("strong_fortes", [])
            match_pct = int(parsed.get("match_percentage", 75))

            if summary and strong_fortes:
                return {
                    "summary": summary,
                    "strong_fortes": strong_fortes,
                    "match_percentage": match_pct
                }
        except Exception as e:
            logger.warning(f"[LLM Service] Applicant insights generation error: {e}. Using synthesis fallback.")

        # Fallback synthesis
        name = candidate_name or 'The applicant'
        skills_sample = candidate_skills[:4] if candidate_skills else ['software engineering', 'modern development workflows']
        summary = (
            f"{name} is an undergraduate student with verified academic preparation and practical competencies in {', '.join(skills_sample)}. "
            f"Their portfolio demonstrates focused alignment with the core responsibilities of {vacancy_title}."
        )
        if clean_cover_letter and len(clean_cover_letter) > 40:
            summary += " Their submitted application cover letter articulates high initiative, agile readiness, and dedication to immediate contribution."

        fortes = [
            f"Demonstrated technical proficiency in {', '.join(candidate_skills[:3]) if candidate_skills else 'core software engineering'}.",
            f"Strong academic foundation from NSBM Green University matching the degree profile of {vacancy_title}.",
            "Demonstrated foundational problem solving and agile workflow readiness."
        ]
        if clean_cover_letter and len(clean_cover_letter) > 40:
            fortes.append("High motivation and role-specific career vision clearly articulated in application pitch.")

        return {
            "summary": summary,
            "strong_fortes": fortes,
            "match_percentage": 75
        }

    @staticmethod
    def generate_candidate_profile_summary(
        candidate_name: str,
        degree_program: str = None,
        faculty: str = None,
        gpa: float = None,
        skills: list = None,
        bio: str = None,
        resume_text: str = None,
        projects: list = None
    ) -> str:
        """Synthesizes candidate academic record, skills, and projects into a 2-3 sentence executive profile."""
        skills_str = ", ".join(skills[:10]) if skills else "software engineering, modern technical workflows"
        gpa_str = f"with a cumulative GPA of {gpa}" if gpa else ""
        deg_str = f"pursuing {degree_program}" if degree_program else "undergraduate student"
        fac_str = f"at {faculty}" if faculty else "at NSBM Green University"
        bio_clean = bio[:350].strip() if bio else ""

        proj_str = ""
        if projects and isinstance(projects, list):
            proj_items = []
            for p in projects[:3]:
                p_title = p.get("title", "")
                p_tech = ", ".join(p.get("tech_stack", []) or p.get("techStack", []))
                if p_title:
                    proj_items.append(f"'{p_title}' ({p_tech})" if p_tech else f"'{p_title}'")
            if proj_items:
                proj_str = f"Verified Completed Projects: {'; '.join(proj_items)}\n"

        prompt = (
            "SYSTEM INSTRUCTION:\n"
            "You are the Senior Talent Evaluation Intelligence Engine for NSBM Green University.\n"
            "Synthesize a compelling 2 to 3 sentence executive profile summary of the undergraduate candidate for industry recruiters and enterprise partners.\n"
            "Requirements:\n"
            "- Emphasize verified technical capabilities, specific real-world projects engineered, and academic excellence.\n"
            "- Strict adherence to provided data; do not invent credentials. Write in third person with executive punch.\n\n"
            "USER REQUEST:\n"
            f"Candidate Name: {candidate_name}\n"
            f"Academic Program: {deg_str} {fac_str} {gpa_str}\n"
            f"Verified Competencies: {skills_str}\n"
            f"{proj_str}"
            f"Statement / Bio: {bio_clean}\n"
        )

        try:
            engine = LLMEngine.get_instance()
            output_text = engine._call_gemini_api(
                prompt=prompt,
                json_mode=False,
                max_tokens=300,
                temperature=0.2
            )
            summary = output_text.strip()
            if summary and len(summary) > 25:
                return summary
        except Exception as e:
            logger.warning(f"[LLM Service] Candidate summary generation error: {e}. Using fallback.")

        return (
            f"{candidate_name} is an undergraduate student {deg_str} {fac_str} {gpa_str}. "
            f"Demonstrates verified competencies in {skills_str}. "
            f"Equipped for technical placement contributions and collaborative software engineering roles."
        )
