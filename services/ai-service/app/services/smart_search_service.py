import re
from typing import Any, Dict, List, Optional
from app.schemas import SmartSearchParsedIntent, SmartSearchRequest, SmartSearchResponse, SmartSearchResultItem


KNOWN_SKILLS = [
    "python", "java", "c++", "c#", "react", "angular", "vue", "node.js", "nodejs",
    "spring", "spring boot", "springboot", "docker", "kubernetes", "aws", "azure", "gcp",
    "sql", "postgresql", "mysql", "mongodb", "git", "ci/cd", "devops", "linux",
    "typescript", "javascript", "html", "css", "figma", "machine learning", "ai",
    "data science", "nlp", "cybersecurity", "flutter", "dart", "kotlin", "swift",
    "adobe creative suite", "graphic design", "full-stack", "backend", "frontend"
]

KNOWN_LOCATIONS = [
    "colombo", "kandy", "galle", "malabe", "homagama", "nsbm", "sri lanka", "remote", "hybrid", "on-site"
]


class SmartSearchService:
    """
    Parses complex natural language queries into structured intent and filters
    candidate, company, or vacancy listings with match scoring and reason highlights.
    """

    @classmethod
    def parse_query_intent(cls, query: str) -> SmartSearchParsedIntent:
        import logging
        logger = logging.getLogger("ai_service.smart_search")

        q_lower = query.lower()
        extracted_skills = []
        for sk in KNOWN_SKILLS:
            # Word boundary matching
            pattern = r'\b' + re.escape(sk) + r'\b'
            if re.search(pattern, q_lower):
                extracted_skills.append(sk.title())

        extracted_locs = []
        for loc in KNOWN_LOCATIONS:
            if re.search(r'\b' + re.escape(loc) + r'\b', q_lower):
                extracted_locs.append(loc.title())

        workplace_type = None
        if "remote" in q_lower:
            workplace_type = "REMOTE"
        elif "hybrid" in q_lower:
            workplace_type = "HYBRID"
        elif "on-site" in q_lower or "onsite" in q_lower or "office" in q_lower:
            workplace_type = "ON_SITE"

        tokens = [w for w in re.findall(r'\b[a-zA-Z0-9_\+#\.]+\b', q_lower) if len(w) > 2 and w not in ["find", "with", "that", "the", "and", "for", "are"]]

        # If fast extraction found explicit skills or tokens, return immediately for sub-millisecond response
        if extracted_skills or extracted_locs:
            return SmartSearchParsedIntent(
                raw_query=query,
                required_skills=extracted_skills,
                locations=extracted_locs,
                workplace_type=workplace_type,
                keywords=tokens
            )

        # Fallback to concise LLM parsing for ambiguous natural language
        try:
            from app.services.llm_engine import LLMEngine
            import json

            llm = LLMEngine.get_instance()
            prompt = (
                f"<|im_start|>system\n"
                f"Extract search intent as strict JSON with keys: required_skills (list), locations (list), workplace_type (REMOTE/HYBRID/ON_SITE or null), keywords (list).\n"
                f"<|im_end|>\n"
                f"<|im_start|>user\n"
                f"{query}<|im_end|>\n"
                f"<|im_start|>assistant\n```json\n"
            )

            response = llm(
                prompt,
                max_tokens=90,
                stop=["<|im_end|>", "```"],
                temperature=0.1
            )
            raw_output = response["choices"][0]["text"].strip()
            if raw_output.startswith("```json"):
                raw_output = raw_output[7:]
            if raw_output.endswith("```"):
                raw_output = raw_output[:-3]

            parsed_dict = json.loads(raw_output.strip())
            parsed_dict["raw_query"] = query
            if "keywords" not in parsed_dict or not parsed_dict["keywords"]:
                parsed_dict["keywords"] = tokens

            return SmartSearchParsedIntent(**parsed_dict)
        except Exception as e:
            logger.warning(f"Fast search intent parsing fallback used: {e}")
            return SmartSearchParsedIntent(
                raw_query=query,
                required_skills=extracted_skills,
                locations=extracted_locs,
                workplace_type=workplace_type,
                keywords=tokens
            )

    @classmethod
    def execute_smart_search(cls, request: SmartSearchRequest) -> SmartSearchResponse:
        from app.services.embedding_engine import EmbeddingEngine

        intent = cls.parse_query_intent(request.query)
        items = request.items_to_rank or []

        if not items:
            return SmartSearchResponse(
                status="success",
                search_type=request.search_type,
                parsed_intent=intent,
                results=[],
                total_found=0
            )

        scored_results: List[SmartSearchResultItem] = []

        # Batch encode for semantic search, capturing skills, program, bio, description, and completed projects
        item_texts = []
        for item in items:
            title = item.get("title", item.get("companyName", item.get("name", "")))
            desc = item.get("description", item.get("requirements", item.get("notes", item.get("bio", ""))))
            skills_val = item.get("skills", [])
            skills_str = " ".join(skills_val) if isinstance(skills_val, list) else str(skills_val or "")
            program = item.get("program", item.get("degreeProgram", item.get("faculty", "")))
            tags = item.get("tags", item.get("industry", ""))

            # Parse project evidence for candidates
            proj_raw = item.get("projects")
            proj_text = ""
            if isinstance(proj_raw, str):
                try:
                    p_list = json.loads(proj_raw)
                    if isinstance(p_list, list):
                        proj_text = " ".join([f"{p.get('title', '')} {' '.join(p.get('tech_stack', []) or p.get('techStack', []))} {p.get('description', '')}" for p in p_list])
                except Exception:
                    proj_text = proj_raw
            elif isinstance(proj_raw, list):
                proj_text = " ".join([f"{p.get('title', '')} {' '.join(p.get('tech_stack', []) or p.get('techStack', []))} {p.get('description', '')}" for p in proj_raw])

            item_texts.append(f"{title} {desc} {skills_str} {program} {tags} {proj_text}".strip().lower())

        query_vec = EmbeddingEngine.encode_text(request.query.lower())
        item_vecs = EmbeddingEngine.encode_batch(item_texts)

        for idx, (item, item_text, item_vec) in enumerate(zip(items, item_texts, item_vecs)):
            sim_score = EmbeddingEngine.compute_cosine_similarity(query_vec, item_vec)

            # Map similarity to a 20-70 base score
            semantic_base = max(15, min(70, int(sim_score * 80)))
            score = semantic_base
            reasons = []

            # ── 1. LOGICAL THINKING: Verified Project Evidence ──
            proj_raw = item.get("projects")
            parsed_projects = []
            if isinstance(proj_raw, str):
                try:
                    p_parsed = json.loads(proj_raw)
                    if isinstance(p_parsed, list):
                        parsed_projects = p_parsed
                except Exception:
                    pass
            elif isinstance(proj_raw, list):
                parsed_projects = proj_raw

            project_evidence = []
            for p in parsed_projects:
                p_title = p.get("title", "Project")
                p_tech = [str(t).lower() for t in (p.get("tech_stack") or p.get("techStack") or [])]
                p_desc = (p.get("description") or "").lower()

                # Check if this project proves any required skills or query terms
                for sk in intent.required_skills:
                    sk_lower = sk.lower()
                    if sk_lower in p_tech or sk_lower in p_desc:
                        project_evidence.append((sk, p_title))

                # Check intent keywords
                for kw in intent.keywords:
                    if len(kw) > 3 and (kw in p_tech or kw in p_desc):
                        if not any(e[0].lower() == kw for e in project_evidence):
                            project_evidence.append((kw.title(), p_title))

            if project_evidence:
                # Award significant evidence score boost for actually building things with the technology!
                evidence_boost = min(35, len(project_evidence) * 15)
                score += evidence_boost
                for sk, p_name in project_evidence[:2]:
                    reasons.append(f"✓ Proven implementation: Built '{p_name}' using {sk}")

            # ── 2. Verified Technical Skills Alignment ──
            matched_skills = [sk for sk in intent.required_skills if sk.lower() in item_text]
            if matched_skills:
                score += min(20, len(matched_skills) * 10)
                reasons.append(f"Demonstrates competencies: {', '.join(matched_skills)}")

            # ── 3. Location and Workplace Flexibility ──
            matched_locs = [loc for loc in intent.locations if loc.lower() in item_text]
            if matched_locs:
                score += 10
                reasons.append(f"Location match: {', '.join(matched_locs)}")

            if intent.workplace_type and intent.workplace_type.lower() in item_text:
                score += 10
                reasons.append(f"{intent.workplace_type.replace('_', ' ').title()} flexibility")

            # ── 4. Keyword Match Boost ──
            matched_kw = [kw for kw in intent.keywords if kw in item_text]
            if matched_kw and not project_evidence:
                score += min(10, len(matched_kw) * 4)
                if not reasons:
                    reasons.append(f"Contains matching keywords: {', '.join(matched_kw[:3])}")

            if sim_score > 0.45 and not reasons:
                reasons.append("High semantic alignment with search query")

            final_score = int(max(20, min(99, score)))
            item_id = item.get("id", item.get("vacancy_id", item.get("userId", idx)))

            if not reasons:
                reasons.append("Profile indexed for matching criteria")

            scored_results.append(SmartSearchResultItem(
                id=item_id,
                item=item,
                match_score=final_score,
                highlight_reasons=reasons
            ))

        # Sort by score descending
        scored_results.sort(key=lambda x: x.match_score, reverse=True)
        top_results = scored_results[:request.limit]

        return SmartSearchResponse(
            status="success",
            search_type=request.search_type,
            parsed_intent=intent,
            results=top_results,
            total_found=len(scored_results)
        )
