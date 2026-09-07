import asyncio
from app.schemas import UniversalSearchRequest
from app.services.universal_search_service import UniversalSearchService

async def main():
    req = UniversalSearchRequest(query='give me jobs related to Databases', limit=5)
    res = await UniversalSearchService.execute_universal_search(req)
    print(res)

asyncio.run(main())
