import base64
import json
import logging
import os
import urllib.request
from pathlib import Path
from typing import Union
import pymupdf

logger = logging.getLogger(__name__)


class OCREngine:
    def __init__(self):
        self.api_key = os.getenv("GEMINI_API_KEY", "AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg")
        self.model = os.getenv("GEMINI_MODEL", "gemini-3.1-flash-lite")

    def _ocr_image(self, img_path: Path) -> str:
        """Extract text from an image using Gemini Flash multimodal vision with automatic failover."""
        if not self.api_key:
            return ""
        try:
            with open(img_path, "rb") as f:
                img_data = base64.b64encode(f.read()).decode("utf-8")
            suffix = img_path.suffix.lower().replace(".", "")
            mime = "image/jpeg" if suffix in ["jpg", "jpeg"] else "image/png"

            payload = {
                "contents": [{
                    "parts": [
                        {"inline_data": {"mime_type": mime, "data": img_data}},
                        {"text": "Extract all text from this flyer or document image accurately. Return only the extracted plain text without commentary."}
                    ]
                }],
                "generationConfig": {
                    "temperature": 0.0,
                    "maxOutputTokens": 4000
                }
            }

            models_to_try = [self.model] + [m for m in ["gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3-flash-preview", "gemini-flash-lite-latest"] if m != self.model]

            for model_name in models_to_try:
                url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={self.api_key}"
                req = urllib.request.Request(
                    url,
                    data=json.dumps(payload).encode("utf-8"),
                    headers={"Content-Type": "application/json"}
                )
                try:
                    with urllib.request.urlopen(req, timeout=30) as resp:
                        data = json.loads(resp.read().decode("utf-8"))
                        candidates = data.get("candidates", [])
                        if candidates and "content" in candidates[0]:
                            parts = candidates[0]["content"].get("parts", [])
                            if parts:
                                return parts[0].get("text", "").strip()
                except Exception as exc:
                    logger.warning(f"Gemini OCR model '{model_name}' failed: {exc}")
                    continue
        except Exception as e:
            logger.warning(f"Gemini OCR image extraction failed: {e}")
        return ""

    def extract_text(self, file_path: Path) -> str:
        suffix = file_path.suffix.lower()

        if suffix == ".pdf":
            doc = pymupdf.open(file_path)
            # Direct text check for digital PDFs (vast majority of resumes)
            text = "\n\n".join([page.get_text() for page in doc if page.get_text().strip()])
            if len(text.strip()) > 50:
                return text

            # Rasterize scanned pages for Gemini vision OCR
            ocr_text = []
            for idx, page in enumerate(doc):
                pix = page.get_pixmap(dpi=150)
                temp = Path(f"_temp_{idx}.png")
                pix.save(temp)
                try:
                    extracted = self._ocr_image(temp)
                    if extracted:
                        ocr_text.append(extracted)
                finally:
                    if temp.exists():
                        temp.unlink()
            return "\n\n".join(ocr_text)

        return self._ocr_image(file_path)

