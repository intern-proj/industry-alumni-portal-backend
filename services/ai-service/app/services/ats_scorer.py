import re
import logging
from typing import Any, Dict, List, Optional, Set, Tuple

logger = logging.getLogger("ai_service.ats_scorer")

# Technology synonym & alias clusters
TECH_SYNONYMS = {
    "react": ["react.js", "reactjs"],
    "react native": ["react-native"],
    "next.js": ["nextjs", "next"],
    "angular": ["angular.js", "angularjs"],
    "vue": ["vue.js", "vuejs"],
    "java": ["jdk", "jvm"],
    "spring boot": ["spring", "springboot"],
    "python": ["python3", "py"],
    "fastapi": ["fast-api"],
    "django": ["django-rest-framework", "drf"],
    "node": ["node.js", "nodejs"],
    "typescript": ["ts"],
    "javascript": ["js", "ecmascript"],
    "c#": ["c-sharp"],
    ".net": ["dotnet", "asp.net"],
    "sql": ["sql-server"],
    "postgresql": ["postgres"],
    "mongodb": ["mongo"],
    "docker": ["docker-compose", "containers", "containerization"],
    "kubernetes": ["k8s"],
    "aws": ["amazon web services"],
    "azure": ["microsoft azure"],
    "git": ["github", "gitlab", "bitbucket", "version control"],
    "ci/cd": ["github actions", "jenkins", "gitlab ci", "pipelines"],
    "rest api": ["restful", "apis", "endpoints"],
    "html": ["html5"],
    "css": ["css3", "styling"],
    "tailwind": ["tailwind css"],
    "flutter": ["dart"],
    "swift": ["ios", "apple"],
    "kotlin": ["android"],
    "figma": ["ui/ux", "wireframing", "prototyping"],
    "scrum": ["agile", "sprint"]
}

FACULTY_KEYWORDS = {
    "computing": ["software", "computing", "computer science", "information technology", "cyber security", "data science", "developer", "engineer", "network"],
    "business": ["marketing", "accounting", "finance", "business", "logistics", "management", "human resources", "hr", "supply chain"],
    "engineering": ["civil", "mechanical", "electrical", "mechatronics", "biomedical", "engineering", "hardware"],
    "science": ["data analysis", "analytics", "statistics", "mathematics", "biotechnology", "science"]
}


class ATSScorer:
    """
    5-Pillar Modern ATS Matching Engine:
    Pillar 1: Technical & Core Skills Coverage (30%)
    Pillar 2: Semantic Dense Embedding Cosine Similarity (25%)
    Pillar 3: Deep Context Cross-Encoder Reranking (25%)
    Pillar 4: Institutional & Academic Alignment (10%)
    Pillar 5: Seniority Compatibility (10%)
    """

    @classmethod
    def normalize_skill(cls, skill: str) -> str:
        s = skill.strip()
        # Strip common prefixes like 'Required Skills:', 'Key Skills:', 'Skills:', 'Requirements:'
        s = re.sub(r'^(required\s+skills|preferred\s+skills|key\s+skills|skills|requirements|qualifications)\s*[:\-]\s*', '', s, flags=re.IGNORECASE)
        return s.strip().lower()

    @classmethod
    def get_skill_family(cls, skill: str) -> Set[str]:
        s = cls.normalize_skill(skill)
        family = {s}
        for root, aliases in TECH_SYNONYMS.items():
            if s == root or s in aliases:
                family.add(root)
                family.update(aliases)
        return family

    @classmethod
    def match_skill_lists(cls, candidate_skills: List[str], required_skills: List[str]) -> Tuple[List[str], List[str], float]:
        """
        Compares candidate skill set against required skills with synonym expansion.
        Returns: (matched_skills, missing_skills, coverage_percentage_0_to_100)
        """
        cand_normalized = {cls.normalize_skill(s) for s in candidate_skills if s}
        # Expand candidate skills with aliases
        expanded_cand = set()
        for s in cand_normalized:
            expanded_cand.update(cls.get_skill_family(s))

        matched = []
        missing = []

        if not required_skills:
            # Fallback if no specific skills listed
            return candidate_skills[:5], [], 80.0

        for req in required_skills:
            req_clean = re.sub(r'^(required\s+skills|preferred\s+skills|key\s+skills|skills|requirements|qualifications)\s*[:\-]\s*', '', req.strip(), flags=re.IGNORECASE).strip()
            if not req_clean:
                continue
            req_norm = cls.normalize_skill(req_clean)
            req_family = cls.get_skill_family(req_norm)

            if req_norm in expanded_cand or any(alias in expanded_cand for alias in req_family):
                matched.append(req_clean.title())
            else:
                missing.append(req_clean.title())

        coverage = (len(matched) / max(1, len(matched) + len(missing))) * 100.0
        return matched, missing, min(100.0, max(0.0, coverage))

    @classmethod
    def build_dense_resume_profile(cls, target_role: str, skills: List[str], raw_snippet: str = "", faculty: str = "") -> str:
        """
        Creates a high-density structured semantic profile under 512 tokens.
        Avoids truncation of critical technical signals.
        """
        skills_str = ", ".join(skills[:18]) if skills else "Software Engineering, Problem Solving"
        role_str = target_role or "Software Engineer / Undergraduate Intern"
        faculty_str = faculty or "Faculty of Computing"
        clean_snippet = re.sub(r'\s+', ' ', raw_snippet).strip()[:250] if raw_snippet else ""

        return f"Role: {role_str} | Faculty: {faculty_str} | Core Skills: {skills_str} | Highlights: {clean_snippet}"

    @classmethod
    def build_dense_vacancy_profile(cls, title: str, company: str, requirements: str, tags: str = "", workplace_type: str = "") -> str:
        """
        Creates a high-density structured vacancy profile under 512 tokens.
        """
        clean_req = re.sub(r'\s+', ' ', requirements or "").strip()[:250]
        tags_str = tags or "Engineering, Placement"
        mode_str = workplace_type or "Full-time / Internship"
        return f"Position: {title} at {company} | Mode: {mode_str} | Required: {clean_req} | Tags: {tags_str}"

    @classmethod
    def evaluate_institutional_fit(cls, candidate_text: str, vacancy_text: str) -> float:
        """Pillar 4 evaluation: Checks degree, faculty, and internship suitability."""
        score = 80.0
        cand_lower = candidate_text.lower()
        vac_lower = vacancy_text.lower()

        # Check faculty alignment
        for faculty, kw_list in FACULTY_KEYWORDS.items():
            vac_has_fac = any(kw in vac_lower for kw in kw_list)
            cand_has_fac = any(kw in cand_lower for kw in kw_list)
            if vac_has_fac and cand_has_fac:
                score += 15.0
                break

        # Check intern / graduate suitability
        if any(w in vac_lower for w in ["intern", "trainee", "associate", "junior", "graduate"]):
            score += 5.0

        return min(100.0, max(40.0, score))

    @classmethod
    def compute_seniority_penalty(cls, cand_seniority: str, vac_seniority: str, req_experience: float) -> float:
        """
        Calculates a penalty multiplier if the candidate's seniority level is mismatched with the vacancy.
        Returns a multiplier between 0.4 and 1.0.
        """
        if not cand_seniority or not vac_seniority:
            return 1.0
            
        cand = cand_seniority.lower()
        vac = vac_seniority.lower()
        
        cand_is_junior = any(x in cand for x in ["intern", "junior", "trainee", "student"])
        vac_is_senior = any(x in vac for x in ["senior", "lead", "manager", "head", "director", "principal"])
        
        if cand_is_junior and vac_is_senior:
            return 0.4  # Severe penalty
            
        if cand_is_junior and req_experience >= 3.0:
            return 0.7  # Moderate penalty
            
        cand_is_mid = any(x in cand for x in ["mid", "intermediate"]) or (not cand_is_junior and not any(x in cand for x in ["senior", "lead", "manager"]))
        if cand_is_mid and vac_is_senior:
            return 0.85  # Mild penalty
            
        return 1.0

    @classmethod
    def compute_composite_ats_score(
        cls,
        skills_coverage: float,
        semantic_sim: float,
        cross_encoder_prob: float,
        institutional_fit: float,
        seniority_fit: float = 100.0,
        seniority_penalty: float = 1.0
    ) -> Tuple[int, Dict[str, int], str, str]:
        """
        Calculates weighted composite match percentage:
        30% Skills + 25% Semantic Cosine + 25% Cross-Encoder + 10% Institutional + 10% Seniority.
        Returns: (final_match_percentage, breakdown_dict, fit_summary, match_tier)
        """
        # Scale 0.0 - 1.0 floats to 0 - 100
        p1 = max(0.0, min(100.0, skills_coverage))
        p2 = max(0.0, min(100.0, semantic_sim * 100.0))
        p3 = max(0.0, min(100.0, cross_encoder_prob * 100.0))
        p4 = max(0.0, min(100.0, institutional_fit))
        p5 = max(0.0, min(100.0, seniority_fit))

        raw_score = (0.30 * p1) + (0.25 * p2) + (0.25 * p3) + (0.10 * p4) + (0.10 * p5)
        raw_score = raw_score * max(0.0, min(1.0, seniority_penalty))
        
        # Bounded between 15% and 98%
        final_pct = int(round(max(15.0, min(98.0, raw_score))))

        breakdown = {
            "skills_coverage": int(round(p1)),
            "semantic_alignment": int(round(p2)),
            "cross_encoder_score": int(round(p3)),
            "institutional_fit": int(round(p4)),
            "seniority_fit": int(round(p5))
        }

        # Tier and Explainable Summary
        if seniority_penalty < 1.0:
            tier = "POTENTIAL_MATCH" if final_pct >= 50 else "WEAK_MATCH"
            summary = "Seniority Mismatch — Candidate's experience level is lower than the role's primary requirements."
        elif final_pct >= 85:
            tier = "EXCEPTIONAL_MATCH"
            summary = "Exceptional Candidate Alignment — Meets core technical competencies and aligns strongly with role architecture."
        elif final_pct >= 70:
            tier = "STRONG_MATCH"
            summary = "Strong Candidate Match — Demonstrates required skills with great potential in key project requirements."
        elif final_pct >= 55:
            tier = "MODERATE_MATCH"
            summary = "Moderate Match — Candidate meets foundational criteria with clear stretch opportunities for secondary requirements."
        else:
            tier = "POTENTIAL_MATCH"
            summary = "Potential Match — Profile shares adjacent competencies; recommended for exploratory review."

        return final_pct, breakdown, summary, tier
