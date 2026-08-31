import json
from huggingface_hub import hf_hub_download
from llama_cpp import Llama
from app.config import settings
from app.schemas import JobVacancySchema


class LLMEngine:
    _instance = None

    @classmethod
    def get_instance(cls):
        """Singleton pattern to keep LLM warm in memory across HTTP requests."""
        if cls._instance is None:
            print("[LLM Service] Preloading Qwen model into RAM...")
            model_path = hf_hub_download(
                repo_id=settings.LLM_REPO_ID,
                filename=settings.LLM_FILENAME
            )
            cls._instance = Llama(
                model_path=model_path,
                n_ctx=settings.LLM_CONTEXT_SIZE,
                n_batch=512,
                n_threads=settings.LLM_THREADS,
                verbose=False
            )
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
            f"4. `employment_type`: e.g., 'Full-time', 'Part-time', 'Contract', 'Internship'. Infer if not explicit.\n"
            f"5. `locations`: Return as a list of SEPARATE address strings. Split head office and branch offices into distinct items. Example: ['360/2/3, Katuwana Road, Homagama', '288/11/1, Makumbura Junction, Kottawa, Pannipitiya'].\n"
            f"6. `required_skills`: Break each technology/tool into individual strings. Do NOT combine them. Example: ['Ionic', 'Capacitor', 'React Native', 'Firebase'].\n"
            f"7. `preferred_skills`: Skills that are nice-to-have but not mandatory.\n"
            f"8. `responsibilities`: Extract each duty/responsibility as a separate string.\n"
            f"9. `contact_emails`: Extract all email addresses found.\n"
            f"10. `contact_phones`: Extract all phone numbers found (include country code if present).\n"
            f"11. `application_urls`: Extract any website URLs for applications.\n"
            f"12. `salary_raw`: Keep as the original text (e.g., 'Negotiable based on experience & skills').\n"
            f"13. `min_experience_years`: Set to 0 if not mentioned or if role is junior/intern level.\n"
            f"14. Return ONLY the valid JSON object inside a ```json code block. No commentary.<|im_end|>\n"
            f"<|im_start|>user\n"
            f"Job Vacancy Content:\n{raw_text}<|im_end|>\n"
            f"<|im_start|>assistant\n```json\n"
        )

        response = llm(
            prompt,
            max_tokens=1536,
            temperature=0.0,
            stop=["```", "<|im_end|>"]
        )

        output_text = response["choices"][0]["text"].strip()
        if output_text.startswith("```json"):
            output_text = output_text[7:]
        if output_text.endswith("```"):
            output_text = output_text[:-3]

        parsed = json.loads(output_text.strip())
        return JobVacancySchema.model_validate(parsed)
