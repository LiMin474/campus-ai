from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from app.services.llm import LLMService, polish_description

router = APIRouter(prefix="/api/ai", tags=["ai"])


class PolishRequest(BaseModel):
    text: str
    user_id: int | None = None  # Java 鉴权后透传，阶段 C 仅作日志保留


class PolishResponse(BaseModel):
    text: str


async def get_llm() -> LLMService:
    from app.main import llm_service
    return llm_service


@router.post("/polish", response_model=PolishResponse)
async def polish(req: PolishRequest, llm: LLMService = Depends(get_llm)) -> PolishResponse:
    try:
        result = await polish_description(llm, req.text)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    return PolishResponse(text=result)
