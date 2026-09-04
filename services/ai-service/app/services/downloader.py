import logging
import tempfile
from pathlib import Path
import httpx

logger = logging.getLogger("ai_service.downloader")


async def download_image_to_tempfile(url: str) -> Path:
    """Downloads remote image/PDF bytes into a temporary file with robust URL validation."""
    if not url or not isinstance(url, str):
        raise ValueError("Empty or non-string URL provided for download.")

    clean_url = url.strip()
    if not (clean_url.startswith("http://") or clean_url.startswith("https://")):
        raise ValueError(f"Invalid URL protocol (must be http:// or https://): {clean_url}")

    async with httpx.AsyncClient(timeout=20.0, follow_redirects=True) as client:
        try:
            response = await client.get(clean_url)
            response.raise_for_status()
        except httpx.HTTPError as e:
            logger.warning(f"HTTP error downloading file from {clean_url}: {e}")
            raise e

    # Determine suffix from header or URL
    content_type = response.headers.get("content-type", "").lower()
    suffix = ".pdf" if ("pdf" in content_type or clean_url.lower().endswith(".pdf")) else ".png"

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
        temp_file.write(response.content)
        return Path(temp_file.name)
