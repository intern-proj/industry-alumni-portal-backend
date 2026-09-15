import json
import logging
import re
from app.services.llm_engine import LLMEngine, _llm_lock
from app.schemas import StructuredResumeSchema
from app.services.resume_matcher_service import KNOWN_TECH_SKILLS

logger = logging.getLogger("ai_service.resume_extractor")

class ResumeExtractor:
    @staticmethod
    def extract_structured_resume(raw_text: str) -> StructuredResumeSchema:
        """Uses Qwen LLM to extract structured skills, projects, and bio from resume text."""
        # 1. Immediate regex extraction of hard skills as guaranteed baseline
        extracted_skills = []
        lower = (raw_text or "").lower()
        for sk in KNOWN_TECH_SKILLS:
            if re.search(r'\b' + re.escape(sk) + r'\b', lower):
                extracted_skills.append(sk.title())
        baseline_skills = list(dict.fromkeys(extracted_skills))

        if not raw_text or len(raw_text.strip()) < 20:
            return StructuredResumeSchema(skills=baseline_skills)

        # 2. Extract structured projects and bio via local LLM
        prompt = (
            f"<|im_start|>system\n"
            f"You are an expert HR AI assistant. Extract structured candidate portfolio data from the resume text into strict JSON adhering to this structure:\n"
            f'{{"candidate_name": "Full Name", "target_roles": ["Target Role"], "skills": ["Skill1", "Skill2"], "projects": [{{"title": "Project Title", "tech_stack": ["SkillUsed"], "description": "1 concise sentence description"}}], "bio": "2 sentence professional bio"}}\n'
            f"CRITICAL EXTRACTION RULES:\n"
            f"1. Extract ONLY projects and work samples explicitly documented in the candidate resume text.\n"
            f"2. If the resume does NOT contain any projects, return an empty array: \"projects\": []. DO NOT invent, fabricate, or hallucinate project names.\n"
            f"3. Return ONLY valid JSON inside ```json. Keep descriptions strictly 1 concise sentence.<|im_end|>\n"
            f"<|im_start|>user\n"
            f"Resume Content:\n{raw_text[:2500]}<|im_end|>\n"
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
                    max_tokens=750,
                    temperature=0.1,
                    stop=["```", "<|im_end|>", "\n\n\n"]
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

            parsed = ResumeExtractor._repair_and_parse_json(output_text)
            structured = StructuredResumeSchema.model_validate(parsed)
            # Merge with baseline skills
            all_skills = list(dict.fromkeys(structured.skills + baseline_skills))
            structured.skills = all_skills
            return structured
        except Exception as e:
            logger.warning(f"LLM resume extraction failed, using fallback: {e}")
            return StructuredResumeSchema(skills=baseline_skills)

    @staticmethod
    def _repair_and_parse_json(text: str) -> dict:
        """Repairs truncated JSON if the LLM output was cut off at the token boundary."""
        cleaned = text.strip()
        try:
            return json.loads(cleaned)
        except Exception:
            pass

        # 1. Close unclosed string quote if generation was cut off mid-string
        in_str = False
        escape = False
        for ch in cleaned:
            if ch == '\\' and not escape:
                escape = True
                continue
            if ch == '"' and not escape:
                in_str = not in_str
            escape = False
        if in_str:
            cleaned += '"'

        # 2. Track unclosed brackets and braces
        stack = []
        in_str = False
        escape = False
        for ch in cleaned:
            if ch == '\\' and not escape:
                escape = True
                continue
            if ch == '"' and not escape:
                in_str = not in_str
                escape = False
                continue
            escape = False
            if not in_str:
                if ch in ('{', '['):
                    stack.append(ch)
                elif ch in ('}', ']'):
                    if stack:
                        stack.pop()

        for op in reversed(stack):
            cleaned += '}' if op == '{' else ']'

        try:
            return json.loads(cleaned)
        except Exception:
            cleaned = re.sub(r',\s*([\}\]])', r'\1', cleaned)
            return json.loads(cleaned)
