from pathlib import Path
from typing import Union
import pymupdf
from rapidocr_onnxruntime import RapidOCR


class OCREngine:
    def __init__(self):
        self.ocr = RapidOCR()

    def _ocr_image(self, img_path: Path) -> str:
        result, _ = self.ocr(str(img_path))
        if not result:
            return ""
        return "\n".join([item[1].strip() for item in result if item and len(item) > 1 and item[1].strip()])

    def extract_text(self, file_path: Path) -> str:
        suffix = file_path.suffix.lower()

        if suffix == ".pdf":
            doc = pymupdf.open(file_path)
            # Direct text check
            text = "\n\n".join([page.get_text() for page in doc if page.get_text().strip()])
            if len(text.strip()) > 50:
                return text

            # Rasterize scanned pages
            ocr_text = []
            for idx, page in enumerate(doc):
                pix = page.get_pixmap(dpi=150)
                temp = Path(f"_temp_{idx}.png")
                pix.save(temp)
                ocr_text.append(self._ocr_image(temp))
                if temp.exists():
                    temp.unlink()
            return "\n\n".join(ocr_text)

        return self._ocr_image(file_path)
