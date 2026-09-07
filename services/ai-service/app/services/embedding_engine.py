import hashlib
import logging
import re
from typing import List, Tuple, Union
import numpy as np

logger = logging.getLogger("ai_service.embedding_engine")


class EmbeddingEngine:
    _bi_encoder = None
    _cross_encoder = None
    _bi_encoder_failed = False
    _cross_encoder_failed = False

    BI_ENCODER_MODEL = "BAAI/bge-small-en-v1.5"
    CROSS_ENCODER_MODEL = "cross-encoder/ms-marco-MiniLM-L-6-v2"

    @classmethod
    def get_bi_encoder(cls):
        """Loads or returns singleton bi-encoder for dense semantic embeddings."""
        if cls._bi_encoder is None and not cls._bi_encoder_failed:
            try:
                from sentence_transformers import SentenceTransformer
                logger.info(f"Loading Bi-Encoder embedding model: {cls.BI_ENCODER_MODEL}...")
                cls._bi_encoder = SentenceTransformer(cls.BI_ENCODER_MODEL)
                logger.info("Bi-Encoder model loaded successfully into memory.")
            except Exception as e:
                logger.warning(f"Could not load Bi-Encoder {cls.BI_ENCODER_MODEL}: {e}. Activating deterministic semantic fallback.")
                cls._bi_encoder_failed = True
        return cls._bi_encoder

    @classmethod
    def get_cross_encoder(cls):
        """Loads or returns singleton cross-encoder for deep pairwise reranking."""
        if cls._cross_encoder is None and not cls._cross_encoder_failed:
            try:
                from sentence_transformers import CrossEncoder
                logger.info(f"Loading Cross-Encoder reranker model: {cls.CROSS_ENCODER_MODEL}...")
                cls._cross_encoder = CrossEncoder(cls.CROSS_ENCODER_MODEL)
                logger.info("Cross-Encoder model loaded successfully into memory.")
            except Exception as e:
                logger.warning(f"Could not load Cross-Encoder {cls.CROSS_ENCODER_MODEL}: {e}. Activating fallback scoring.")
                cls._cross_encoder_failed = True
        return cls._cross_encoder

    @classmethod
    def _compute_fallback_vector(cls, text: str, dim: int = 384) -> List[float]:
        """
        Deterministic, section-weighted semantic hash vector fallback.
        Ensures the system never crashes even if model downloads are in progress or offline.
        """
        vec = np.zeros(dim, dtype=np.float32)
        words = re.findall(r'\w+', text.lower())
        if not words:
            return vec.tolist()

        for word in words:
            # Word hashing trick
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
        """Encodes single text into normalized 384-dim dense vector."""
        if not text or not text.strip():
            return [0.0] * 384

        try:
            model = cls.get_bi_encoder()
            if model is not None:
                vec = model.encode(text.strip(), normalize_embeddings=True)
                return vec.tolist()
        except Exception as e:
            logger.warning(f"Bi-encoder encode failed: {e}. Using deterministic fallback vector.")

        return cls._compute_fallback_vector(text)

    @classmethod
    def encode_batch(cls, texts: List[str]) -> List[List[float]]:
        """Batch encodes multiple texts into normalized vectors."""
        clean_texts = [t.strip() if (t and t.strip()) else "empty" for t in texts]
        try:
            model = cls.get_bi_encoder()
            if model is not None:
                vecs = model.encode(clean_texts, normalize_embeddings=True, batch_size=16)
                return vecs.tolist()
        except Exception as e:
            logger.warning(f"Bi-encoder batch encode failed: {e}. Using deterministic fallback.")

        return [cls._compute_fallback_vector(t) for t in clean_texts]

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
        """Pairwise scoring using cross-encoder with batch limit of 15 pairs."""
        if not pairs:
            return []

        # Cap batch size to top 15 pairs to preserve CPU responsiveness
        eval_pairs = pairs[:15]

        try:
            model = cls.get_cross_encoder()
            if model is not None:
                scores = model.predict(eval_pairs)
                normalized = []
                for s in scores:
                    val = float(s)
                    prob = 1.0 / (1.0 + np.exp(-val))
                    normalized.append(float(prob))
                return normalized
        except Exception as e:
            logger.warning(f"Cross-encoder scoring error: {e}. Falling back to 0.6.")

        return [0.6] * len(eval_pairs)
