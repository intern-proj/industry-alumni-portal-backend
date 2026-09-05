import asyncio
import logging
import socket
import os
from app.config import settings

logger = logging.getLogger("ai_service.eureka")

try:
    import py_eureka_client.eureka_client as eureka_client
    PY_EUREKA_AVAILABLE = True
except ImportError:
    PY_EUREKA_AVAILABLE = False


def get_container_ip() -> str:
    """Discovers the container's real routable overlay IP in Azure Container Apps / Docker."""
    host = (settings.EUREKA_INSTANCE_HOST or "").strip()
    if host and host.lower() not in ["localhost", "127.0.0.1"]:
        return host

    # 1. Probe outbound interface IP via UDP socket connect (does not send packets)
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(0.5)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        if ip and not ip.startswith("127."):
            return ip
    except Exception:
        pass

    # 2. Fallback to socket gethostbyname
    try:
        hostname = socket.gethostname()
        ip = socket.gethostbyname(hostname)
        if ip and not ip.startswith("127."):
            return ip
    except Exception:
        pass

    return "127.0.0.1"


class EurekaManager:
    """Manages Eureka Server registration and heartbeat lifecycle."""

    @staticmethod
    async def start():
        if not settings.EUREKA_ENABLED:
            logger.info("Eureka registration is disabled via config.")
            return

        if not PY_EUREKA_AVAILABLE:
            logger.warning("py_eureka_client is not installed. Skipping Eureka registration.")
            return

        try:
            container_ip = get_container_ip()
            instance_id = f"{container_ip}:{settings.EUREKA_APP_NAME.lower()}:{settings.PORT}"
            logger.info(f"Registering with Eureka Server at {settings.EUREKA_SERVER_URL} as {settings.EUREKA_APP_NAME} (IP: {container_ip})...")
            await eureka_client.init_async(
                eureka_server=settings.EUREKA_SERVER_URL,
                app_name=settings.EUREKA_APP_NAME,
                instance_port=settings.PORT,
                instance_host=container_ip,
                instance_ip=container_ip,
                instance_id=instance_id,
                health_check_url=f"http://{container_ip}:{settings.PORT}/health",
                status_page_url=f"http://{container_ip}:{settings.PORT}/health",
                home_page_url=f"http://{container_ip}:{settings.PORT}/"
            )
            logger.info(f"Successfully registered {settings.EUREKA_APP_NAME} with Eureka as {instance_id}.")
        except Exception as e:
            logger.warning(f"Could not connect to Eureka Server at {settings.EUREKA_SERVER_URL}: {e}")

    @staticmethod
    async def stop():
        if not settings.EUREKA_ENABLED or not PY_EUREKA_AVAILABLE:
            return
        try:
            logger.info(f"Deregistering {settings.EUREKA_APP_NAME} from Eureka Server...")
            await eureka_client.stop_async()
            logger.info("Successfully deregistered from Eureka.")
        except Exception as e:
            logger.warning(f"Error during Eureka deregistration: {e}")
