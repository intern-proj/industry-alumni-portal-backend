import tempfile
from pathlib import Path
import httpx


async def download_image_to_tempfile(url: str) -> Path:
    """Downloads remote image/PDF bytes into a temporary file."""
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.get(url)
        response.raise_for_status()

    # Determine suffix from header or URL
    content_type = response.headers.get("content-type", "")
    suffix = ".pdf" if "pdf" in content_type or url.endswith(".pdf") else ".png"

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
        temp_file.write(response.content)
        return Path(temp_file.name)
