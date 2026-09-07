import asyncio
from app.schemas import UniversalSearchRequest
from app.services.universal_search_service import UniversalSearchService

async def main():
    req = UniversalSearchRequest(query='mobile app development', limit=10)
    res = await UniversalSearchService.execute_universal_search(req)
    for r in res.results:
        print(f"Title: {r.item.get('title')}, Score: {r.match_score}, Reasons: {r.highlight_reasons}")

asyncio.run(main())
