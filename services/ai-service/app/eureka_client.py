import asyncio
import logging
from app.config import settings

logger = logging.getLogger("ai_service.eureka")

try:
    import py_eureka_client.eureka_client as eureka_client
    PY_EUREKA_AVAILABLE = True
except ImportError:
    PY_EUREKA_AVAILABLE = False


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
            logger.info(f"Registering with Eureka Server at {settings.EUREKA_SERVER_URL} as {settings.EUREKA_APP_NAME}...")
            await eureka_client.init_async(
                eureka_server=settings.EUREKA_SERVER_URL,
                app_name=settings.EUREKA_APP_NAME,
                instance_port=settings.PORT,
                instance_host=settings.EUREKA_INSTANCE_HOST,
                health_check_url=f"http://{settings.EUREKA_INSTANCE_HOST}:{settings.PORT}/health",
                status_page_url=f"http://{settings.EUREKA_INSTANCE_HOST}:{settings.PORT}/health",
                home_page_url=f"http://{settings.EUREKA_INSTANCE_HOST}:{settings.PORT}/"
            )
            logger.info(f"Successfully registered {settings.EUREKA_APP_NAME} with Eureka.")
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
