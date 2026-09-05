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

        # 2. Extract structured projects, verified competencies, and bio via Gemini
        prompt = (
            "SYSTEM INSTRUCTION:\n"
            "You are an expert technical talent assessor and resume parser for NSBM Green University.\n"
            "Extract the candidate's portfolio, verified technical skills, projects built, and professional background from the resume into a strict JSON object.\n\n"
            "REQUIRED JSON SCHEMA:\n"
            "{\n"
            '  "candidate_name": "Full Name",\n'
            '  "target_roles": ["Primary Role / Specialization"],\n'
            '  "skills": ["Skill1", "Skill2", "Skill3"],\n'
            '  "projects": [\n'
            '    {\n'
            '      "title": "Project Name",\n'
            '      "tech_stack": ["Tech1", "Tech2"],\n'
            '      "description": "1 concise sentence explaining what was engineered and its measurable impact."\n'
            '    }\n'
            '  ],\n'
            '  "bio": "A professional 2-3 sentence executive bio highlighting university background, core competencies, and career focus."\n'
            "}\n\n"
            "EXTRACTION RULES:\n"
            "1. Extract real software systems, hardware labs, course capstones, or work implementations explicitly documented.\n"
            "2. Ensure skills include both programming languages, frameworks, developer tools, and domain specializations.\n"
            "3. Return ONLY the strict JSON object.\n\n"
            "USER REQUEST:\n"
            f"Candidate Resume Content:\n{raw_text[:15000]}\n"
        )

        try:
            llm = LLMEngine.get_instance()
            response = llm(
                prompt,
                max_tokens=3500,
                temperature=0.1
            )

            output_text = response["choices"][0]["text"].strip()
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
        """Repairs truncated or markdown-wrapped JSON."""
        cleaned = text.strip()
        if cleaned.startswith("```json"):
            cleaned = cleaned[7:]
        elif cleaned.startswith("```"):
            cleaned = cleaned[3:]
        if cleaned.endswith("```"):
            cleaned = cleaned[:-3]
        cleaned = cleaned.strip()

        # Locate the outermost JSON braces
        start_idx = cleaned.find('{')
        end_idx = cleaned.rfind('}')
        if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
            cleaned = cleaned[start_idx:end_idx + 1]

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
