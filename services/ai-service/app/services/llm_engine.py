import json
import logging
import threading
from huggingface_hub import hf_hub_download
from llama_cpp import Llama
from app.config import settings
from app.schemas import JobVacancySchema, CoverLetterRequest

logger = logging.getLogger("ai_service.llm_engine")
_llm_lock = threading.Lock()


class LLMEngine:
    _instance = None
    _active_config = None

    @classmethod
    def reload_instance(cls):
        with _llm_lock:
            cls._instance = None
            cls._active_config = None
            logger.info("[LLM Service] Engine instance cleared. Next call will reload active config.")

    @classmethod
    def get_instance(cls):
        """Singleton pattern to keep LLM warm in memory across HTTP requests."""
        if cls._instance is None:
            # Query active configuration from database if available
            repo_id = settings.LLM_REPO_ID
            filename = settings.LLM_FILENAME
            ctx_size = settings.LLM_CONTEXT_SIZE
            threads = settings.LLM_THREADS
            gpu_layers = 0

            try:
                from app.database import SessionLocal
                from app.models import AiModelConfig
                db = SessionLocal()
                active = db.query(AiModelConfig).filter(AiModelConfig.is_active == True).first()
                if active and active.provider == "LOCAL_GGUF":
                    if active.repo_id: repo_id = active.repo_id
                    if active.filename: filename = active.filename
                    if active.context_size: ctx_size = active.context_size
                    if active.threads: threads = active.threads
                    if active.gpu_layers is not None: gpu_layers = active.gpu_layers
                    logger.info(f"[LLM Service] Initializing model from active DB config: {active.config_name} (GPU layers: {gpu_layers})")
                db.close()
            except Exception as e:
                logger.warning(f"[LLM Service] Could not fetch DB model config, falling back to defaults: {e}")

            print(f"[LLM Service] Preloading model {repo_id}/{filename} (GPU layers: {gpu_layers})...")
            try:
                model_path = hf_hub_download(
                    repo_id=repo_id,
                    filename=filename,
                    local_files_only=True
                )
            except Exception:
                try:
                    model_path = hf_hub_download(
                        repo_id=repo_id,
                        filename=filename
                    )
                except Exception as dl_err:
                    logger.warning(f"[LLM Service] Could not download {repo_id}/{filename}: {dl_err}. Falling back to default system model.")
                    model_path = hf_hub_download(
                        repo_id=settings.LLM_REPO_ID,
                        filename=settings.LLM_FILENAME,
                        local_files_only=True
                    )

            kwargs = {
                "model_path": model_path,
                "n_ctx": ctx_size,
                "n_batch": 512,
                "n_threads": threads,
                "verbose": False
            }
            if gpu_layers != 0:
                kwargs["n_gpu_layers"] = gpu_layers

            try:
                cls._instance = Llama(**kwargs)
            except Exception as llama_err:
                if "n_gpu_layers" in kwargs:
                    logger.warning(f"[LLM Service] GPU initialization failed ({llama_err}). Falling back to CPU execution.")
                    kwargs.pop("n_gpu_layers", None)
                    cls._instance = Llama(**kwargs)
                else:
                    raise llama_err
        return cls._instance

    @staticmethod
    def extract_structured_json(raw_text: str) -> JobVacancySchema:
        llm = LLMEngine.get_instance()
        schema_json_str = json.dumps(JobVacancySchema.model_json_schema(), separators=(",", ":"))

        prompt = (
            f"<|im_start|>system\n"
            f"You are an expert HR data parsing engine that extracts structured information from job flyer text.\n"
            f"Extract ALL information into a strict JSON object adhering to this schema:\n"
            f"{schema_json_str}\n\n"
            f"EXTRACTION RULES (follow these precisely):\n"
            f"1. `company_name`: Extract the primary hiring company name only (not sub-brands or sister companies).\n"
            f"2. `job_title`: Extract the exact job role title (e.g., 'Junior Mobile App Developer').\n"
            f"3. `seniority_level`: Infer from title or description — e.g., 'Junior', 'Mid', 'Senior', 'Lead', 'Intern'.\n"
            f"4. `employment_type`: Extract ONLY if explicitly mentioned (e.g., 'Full-time', 'Part-time', 'Contract', 'Internship'). DO NOT guess or infer. Return null if not stated.\n"
            f"5. `workplace_type`: Extract ONLY if explicitly mentioned (e.g., 'ON_SITE', 'REMOTE', 'HYBRID'). DO NOT guess. Return null if not stated.\n"
            f"6. `locations`: Return as a list of SEPARATE address strings. Do NOT include phone numbers, emails, or website URLs as locations.\n"
            f"7. `required_skills`: Break each technology/tool into individual strings. Do NOT combine them. Example: ['Ionic', 'Capacitor', 'React Native', 'Firebase'].\n"
            f"8. `preferred_skills`: Skills that are nice-to-have but not mandatory.\n"
            f"9. `responsibilities`: Extract each duty/responsibility as a separate string.\n"
            f"10. `contact_emails`: Extract all email addresses found.\n"
            f"11. `contact_phones`: Extract all phone numbers found (include country code if present).\n"
            f"12. `application_urls`: Extract any website URLs for applications.\n"
            f"13. `salary_raw`: Keep as the original text (e.g., 'Negotiable based on experience & skills').\n"
            f"14. `min_experience_years`: Set to 0 if not mentioned or if role is junior/intern level.\n"
            f"15. Return ONLY the valid JSON object inside a ```json code block. No commentary.<|im_end|>\n"
            f"<|im_start|>user\n"
            f"Job Vacancy Content:\n{raw_text}<|im_end|>\n"
            f"<|im_start|>assistant\n```json\n"
        )

        with _llm_lock:
            try:
                llm.reset()
            except Exception:
                pass
            response = llm(
                prompt,
                max_tokens=1800,
                temperature=0.0,
                stop=["```", "<|im_end|>"]
            )
            try:
                llm.reset()
            except Exception:
                pass

        output_text = response["choices"][0]["text"].strip()
        if output_text.startswith("```json"):
            output_text = output_text[7:]
        if output_text.endswith("```"):
            output_text = output_text[:-3]

        parsed = json.loads(output_text.strip())
        return JobVacancySchema.model_validate(parsed)

    @staticmethod
    def generate_cover_letter(request: CoverLetterRequest) -> str:
        skills_str = ", ".join(request.candidate_skills) if request.candidate_skills else "software engineering and modern technology frameworks"
        prompt = (
            f"<|im_start|>system\n"
            f"You are a professional talent advocate and career counselor.\n"
            f"Write a concise, high-impact 3-paragraph executive cover letter in HTML using <p> and <strong> tags only. Keep each paragraph to 1-2 clear, punchy sentences. Maximum 110 words.\n"
            f"Paragraph 1: Express strong enthusiasm for the role and company, citing candidate background.\n"
            f"Paragraph 2: Align core technical skills directly with the job requirements and projects.\n"
            f"Paragraph 3: Reiterate value proposition, eagerness to contribute, and thank the hiring team.\n"
            f"<|im_end|>\n"
            f"<|im_start|>user\n"
            f"Candidate: {request.candidate_name}\n"
            f"Candidate Skills: {skills_str}\n"
            f"Target Role: {request.vacancy_title}\n"
            f"Target Company: {request.company_name}\n"
            f"Job Requirements: {request.vacancy_requirements[:400]}\n"
            f"<|im_end|>\n"
            f"<|im_start|>assistant\n<p>Dear Hiring Team at <strong>{request.company_name}</strong>,</p>\n"
        )
        
        try:
            llm = LLMEngine.get_instance()
            with _llm_lock:
                try:
                    llm.reset()
                except Exception:
                    pass
                response = llm(
                    prompt,
                    max_tokens=160,
                    temperature=0.1,
                    stop=["<|im_end|>", "```"]
                )
                try:
                    llm.reset()
                except Exception:
                    pass
            
            output_text = response["choices"][0]["text"].strip()
            # If the model didn't start with the opening <p>, prepend it
            if not output_text.startswith("<p>Dear"):
                output_text = f"<p>Dear Hiring Team at <strong>{request.company_name}</strong>,</p>\n" + output_text
                
            if output_text.startswith("```html"):
                output_text = output_text[7:]
            if output_text.startswith("```"):
                output_text = output_text[3:]
            if output_text.endswith("```"):
                output_text = output_text[:-3]
                
            cleaned = output_text.strip()
            # Normalize typographic characters for clean HTML rendering
            cleaned = cleaned.replace("’", "'").replace("‘", "'").replace("“", '"').replace("”", '"').replace("—", " - ").replace("–", " - ")
            if len(cleaned) > 50:
                return cleaned
        except Exception as e:
            import logging
            logging.getLogger("ai_service.llm_engine").warning(f"LLM cover letter inference fallback: {e}")

        # High-quality fallback
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
        """
        Dynamically evaluates the candidate's profile, full resume, and cover letter
        against the job vacancy requirements. Context and output token limits are dynamically 
        calibrated according to the job complexity and seniority to maximize accuracy and analytical depth:
        - Specialized/Senior/Lead/DevOps/Cloud roles: Deep architectural, toolchain, and leadership assessment (up to 650 tokens).
        - Mid/Junior/Intern roles: Academic foundation, technical stack proficiency, and growth trajectory assessment (up to 500-550 tokens).
        Full cover letter and comprehensive resume text are retained for maximum evaluation accuracy.
        """
        import re
        import logging
        logger = logging.getLogger("ai_service.llm_engine")

        skills_str = ", ".join(candidate_skills) if candidate_skills else "Not specified"
        clean_cover_letter = cover_letter or ""
        clean_cover_letter = re.sub(r'<[^>]+>', ' ', clean_cover_letter)
        clean_cover_letter = ' '.join(clean_cover_letter.split())[:3500]
        clean_resume_text = ' '.join((resume_text or "").split())[:4500]
        clean_requirements = ' '.join((vacancy_requirements or "").split())[:2500]
        clean_description = ' '.join((vacancy_description or "").split())[:2500]

        # Analyze job complexity and seniority level to tailor prompt depth and token budget
        role_corpus = f"{vacancy_title} {clean_requirements}".lower()
        is_senior_or_lead = any(term in role_corpus for term in ["senior", "lead", "principal", "architect", "manager", "head", "director"])
        is_devops_or_infra = any(term in role_corpus for term in ["devops", "cloud", "sre", "infrastructure", "platform", "kubernetes", "security", "sysadmin"])
        is_data_or_ai = any(term in role_corpus for term in ["data engineer", "data scientist", "machine learning", "ai engineer", "deep learning"])
        is_intern_or_trainee = any(term in role_corpus for term in ["intern", "trainee", "associate", "junior", "entry", "fresh"])

        if is_senior_or_lead or is_devops_or_infra or is_data_or_ai:
            role_type = "Specialized Technical / Senior Role"
            output_token_budget = 650
            depth_instruction = (
                "Conduct an in-depth technical analysis. Focus specifically on architectural competency, production toolchain mastery, "
                "problem-solving capabilities, and readiness to handle complex workflows based on the resume and cover letter."
            )
        elif is_intern_or_trainee:
            role_type = "Internship / Graduate Trainee Role"
            output_token_budget = 500
            depth_instruction = (
                "Focus on academic grounding, core coursework mastery, speed of adaptation, practical project deliverables, "
                "and proactive motivation articulated in the cover letter."
            )
        else:
            role_type = "Standard Professional Role"
            output_token_budget = 550
            depth_instruction = (
                "Focus on direct technical fit, hands-on framework competencies, project contributions, "
                "and communication clarity evidenced in the cover letter."
            )

        prompt = (
            f"<|im_start|>system\n"
            f"You are a principal talent assessor and technical evaluation intelligence engine for university recruitment partnerships.\n"
            f"Evaluation Category: {role_type}.\n"
            f"{depth_instruction}\n\n"
            f"Output requirements:\n"
            f"1. `summary`: A detailed, professional 2 to 4 sentence executive summary of the applicant. Accurately capture their degree background, technical specializations, key practical projects, and how their background qualifies them for this vacancy.\n"
            f"2. `strong_fortes`: A list of 3 to 5 comprehensive, highly specific bullet points detailing the candidate's strongest advantages for this application. Directly cite technologies, project achievements, and motivations evidenced in their resume and cover letter.\n"
            f"3. `match_percentage`: An accurate, calibrated integer percentage (0 to 100) reflecting overall competency alignment against the vacancy's mandatory and preferred requirements.\n\n"
            f"Format response strictly as valid JSON within a ```json code block.\n"
            f"<|im_end|>\n"
            f"<|im_start|>user\n"
            f"[TARGET VACANCY]\n"
            f"Title: {vacancy_title}\n"
            f"Requirements:\n{clean_requirements if clean_requirements else 'Industry standard technical requirements.'}\n\n"
            f"Description & Responsibilities:\n{clean_description if clean_description else 'Standard engineering responsibilities.'}\n\n"
            f"[APPLICANT PROFILE]\n"
            f"Name: {candidate_name or 'Applicant'}\n"
            f"Verified Skills: {skills_str}\n\n"
            f"[SUBMITTED COVER LETTER]\n"
            f"{clean_cover_letter if clean_cover_letter else 'No cover letter provided.'}\n\n"
            f"[EXTRACTED RESUME DOSSIER]\n"
            f"{clean_resume_text if clean_resume_text else 'No resume text available.'}\n"
            f"<|im_end|>\n"
            f"<|im_start|>assistant\n```json\n"
        )

        try:
            llm = LLMEngine.get_instance()
            with _llm_lock:
                try:
                    llm.reset()
                except Exception:
                    pass
                response = llm(
                    prompt,
                    max_tokens=output_token_budget,
                    temperature=0.1,
                    stop=["```", "<|im_end|>"]
                )
                try:
                    llm.reset()
                except Exception:
                    pass
            output_text = response["choices"][0]["text"].strip()
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
            logger.warning(f"LLM applicant insights inference failed or skipped: {e}. Utilizing smart synthesis fallback.")

        # Context-aware fallback if LLM is offline or model fails
        name = candidate_name or 'The applicant'
        skills_sample = candidate_skills[:4] if candidate_skills else ['software engineering', 'modern development workflows']
        summary = (
            f"{name} is an undergraduate student with verified academic preparation and practical competencies in {', '.join(skills_sample)}. "
            f"Their portfolio demonstrates focused alignment with the core responsibilities of {vacancy_title}."
        )
        if clean_cover_letter and len(clean_cover_letter) > 40:
            summary += " Their submitted application cover letter articulates high initiative, agile readiness, and dedication to immediate contribution."

        fortes = [
            f"Demonstrated technical proficiency in {', '.join(candidate_skills[:3]) if candidate_skills else 'core software engineering'}."
        ]
        if is_senior_or_lead or is_devops_or_infra:
            fortes.append(f"Demonstrated capability in systems architecture, scripting, and infrastructure automation relevant to {vacancy_title}.")
        else:
            fortes.append(f"Strong academic foundation from NSBM Green University matching the degree profile of {vacancy_title}.")
            
        if clean_cover_letter and len(clean_cover_letter) > 40:
            fortes.append("High motivation and role-specific career vision clearly articulated in their application pitch.")
        fortes.append("Demonstrated foundational problem solving and agile workflow readiness.")

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
        llm = LLMEngine.get_instance()
        skills_str = ", ".join(skills[:8]) if skills else "software engineering, modern technical workflows"
        gpa_str = f"with a cumulative GPA of {gpa}" if gpa else ""
        deg_str = f"pursuing {degree_program}" if degree_program else "undergraduate student"
        fac_str = f"at {faculty}" if faculty else "at NSBM Green University"
        bio_clean = bio[:250].strip() if bio else ""

        # Format candidate projects for logical evidence synthesis
        proj_str = ""
        if projects and isinstance(projects, list):
            proj_items = []
            for p in projects[:3]:
                p_title = p.get("title", "")
                p_tech = ", ".join(p.get("tech_stack", []) or p.get("techStack", []))
                if p_title:
                    proj_items.append(f"'{p_title}' ({p_tech})" if p_tech else f"'{p_title}'")
            if proj_items:
                proj_str = f"Key Projects Done: {'; '.join(proj_items)}\n"

        prompt = (
            f"<|im_start|>system\n"
            f"You are an AI talent evaluator for NSBM Green University.\n"
            f"Write a professional 2-3 sentence executive profile summary of the following candidate for corporate partners.\n"
            f"Highlight their verified competencies, actual projects built, and industry readiness based strictly on provided data.\n"
            f"Do not invent unprovided credentials. Write in third person.<|im_end|>\n"
            f"<|im_start|>user\n"
            f"Candidate: {candidate_name}\n"
            f"Academic Record: {deg_str} {fac_str} {gpa_str}\n"
            f"Technical Skills: {skills_str}\n"
            f"{proj_str}"
            f"Bio / Statement: {bio_clean}\n<|im_end|>\n"
            f"<|im_start|>assistant\n"
        )
        try:
            with _llm_lock:
                try:
                    llm.reset()
                except Exception:
                    pass
                response = llm(
                    prompt,
                    max_tokens=150,
                    temperature=0.2,
                    stop=["<|im_end|>", "\n\n", "```"]
                )
                try:
                    llm.reset()
                except Exception:
                    pass
            summary = response["choices"][0]["text"].strip()
            if summary and len(summary) > 25:
                return summary
        except Exception as e:
            logger.warning(f"LLM candidate summary inference failed: {e}")

        # Fallback if model fails or times out
        return (
            f"{candidate_name} is an undergraduate student {deg_str} {fac_str} {gpa_str}. "
            f"Demonstrates verified competencies in {skills_str}. "
            f"Equipped for technical placement contributions and collaborative software engineering roles."
        )


