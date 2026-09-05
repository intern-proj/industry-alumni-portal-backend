import hashlib
import json
import logging
import re
import urllib.request
import urllib.error
from typing import List, Tuple, Union
import numpy as np

from app.config import settings

logger = logging.getLogger("ai_service.embedding_engine")


class EmbeddingEngine:
    """
    High-performance, lightweight Embedding Engine powered by Google Gemini API.
    Eliminates heavy local PyTorch (550+ MB) and sentence-transformers dependencies.
    Provides fast, cloud-based dense vector embeddings with deterministic fallback.
    """
    EMBEDDING_MODEL = "models/gemini-embedding-001"

    @classmethod
    def get_bi_encoder(cls):
        """No-op compatibility method for Gemini Cloud Embedding API."""
        return cls

    @classmethod
    def get_cross_encoder(cls):
        """No-op compatibility method for Gemini Cloud Embedding API."""
        return cls

    @classmethod
    def _compute_fallback_vector(cls, text: str, dim: int = 768) -> List[float]:
        """
        Deterministic, section-weighted semantic hash vector fallback.
        Ensures the system never crashes even if offline or rate limited.
        """
        vec = np.zeros(dim, dtype=np.float32)
        words = re.findall(r'\w+', text.lower())
        if not words:
            return vec.tolist()

        for word in words:
            h = int(hashlib.md5(word.encode('utf-8')).hexdigest(), 16)
            idx = h % dim
            sign = 1.0 if ((h >> 8) & 1) else -1.0
            vec[idx] += sign * (1.0 + len(word) * 0.1)

        norm = np.linalg.norm(vec)
        if norm > 0:
            vec = vec / norm
        return vec.tolist()

    @classmethod
    def encode_text(cls, text: str) -> List[float]:
        """Encodes single text into normalized dense vector using Google Gemini Cloud API."""
        if not text or not text.strip():
            return [0.0] * 768

        clean_text = text.strip()
        api_key = getattr(settings, "GEMINI_API_KEY", "AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg")
        if not api_key:
            return cls._compute_fallback_vector(clean_text)

        url = f"https://generativelanguage.googleapis.com/v1beta/{cls.EMBEDDING_MODEL}:embedContent?key={api_key}"
        payload = {
            "model": cls.EMBEDDING_MODEL,
            "content": {"parts": [{"text": clean_text[:4000]}]}
        }

        try:
            req = urllib.request.Request(
                url,
                data=json.dumps(payload).encode('utf-8'),
                headers={"Content-Type": "application/json"}
            )
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                values = data.get("embedding", {}).get("values")
                if values:
                    arr = np.array(values, dtype=np.float32)
                    norm = np.linalg.norm(arr)
                    if norm > 0:
                        arr = arr / norm
                    return arr.tolist()
        except Exception as e:
            logger.warning(f"[EmbeddingEngine] Gemini API embedding call failed: {e}. Using deterministic fallback.")

        return cls._compute_fallback_vector(clean_text)

    @classmethod
    def encode_batch(cls, texts: List[str]) -> List[List[float]]:
        """Batch encodes multiple texts using Gemini batchEmbedContents API."""
        if not texts:
            return []

        clean_texts = [t.strip()[:4000] if (t and t.strip()) else "empty" for t in texts]
        api_key = getattr(settings, "GEMINI_API_KEY", "AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg")
        if not api_key:
            return [cls._compute_fallback_vector(t) for t in clean_texts]

        results = []
        batch_size = 16
        for i in range(0, len(clean_texts), batch_size):
            chunk = clean_texts[i:i + batch_size]
            url = f"https://generativelanguage.googleapis.com/v1beta/{cls.EMBEDDING_MODEL}:batchEmbedContents?key={api_key}"
            payload = {
                "requests": [
                    {"model": cls.EMBEDDING_MODEL, "content": {"parts": [{"text": c}]}}
                    for c in chunk
                ]
            }
            try:
                req = urllib.request.Request(
                    url,
                    data=json.dumps(payload).encode('utf-8'),
                    headers={"Content-Type": "application/json"}
                )
                with urllib.request.urlopen(req, timeout=15) as resp:
                    data = json.loads(resp.read().decode('utf-8'))
                    embeddings = data.get("embeddings", [])
                    for item in embeddings:
                        vals = item.get("values", [])
                        if vals:
                            arr = np.array(vals, dtype=np.float32)
                            norm = np.linalg.norm(arr)
                            if norm > 0:
                                arr = arr / norm
                            results.append(arr.tolist())
                        else:
                            results.append(cls._compute_fallback_vector(chunk[len(results) % len(chunk)]))
                    continue
            except Exception as e:
                logger.warning(f"[EmbeddingEngine] Gemini batchEmbedContents error: {e}. Using fallback.")

            for c in chunk:
                results.append(cls._compute_fallback_vector(c))

        return results

    @classmethod
    def compute_cosine_similarity(cls, vec1: Union[List[float], np.ndarray], vec2: Union[List[float], np.ndarray]) -> float:
        """Computes cosine similarity between two normalized vectors (scaled to 0.0 - 1.0)."""
        v1 = np.array(vec1, dtype=np.float32)
        v2 = np.array(vec2, dtype=np.float32)
        norm1 = np.linalg.norm(v1)
        norm2 = np.linalg.norm(v2)
        if norm1 == 0 or norm2 == 0:
            return 0.5
        cos_sim = float(np.dot(v1, v2) / (norm1 * norm2))
        return max(0.0, min(1.0, (cos_sim + 1.0) / 2.0))

    @classmethod
    def compute_cross_scores(cls, pairs: List[Tuple[str, str]]) -> List[float]:
        """Pairwise scoring using semantic similarity between candidate and vacancy."""
        if not pairs:
            return []

        eval_pairs = pairs[:15]
        scores = []
        for text1, text2 in eval_pairs:
            v1 = cls.encode_text(text1)
            v2 = cls.encode_text(text2)
            scores.append(cls.compute_cosine_similarity(v1, v2))
        return scores
