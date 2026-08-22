"""RAG HTTP 接口。

三个端点：
- POST /api/ai/rag/index  全量重灌商品
- POST /api/ai/rag/search 纯检索
- POST /api/ai/rag/chat   RAG 对话（检索 + 生成）
"""
from typing import List
from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel

from app.services.rag import RagService

# tags=["ai‑rag"]：给接口分组，在 FastAPI 自带文档页 http://127.0.0.1:8000/docs
router = APIRouter(prefix="/api/ai/rag", tags=["ai-rag"])


# ------------------------------------------------------------------
# 请求 / 响应模型
# ------------------------------------------------------------------
# 定义前端发什么 JSON 过来，后端返回什么 JSON 回去，并且自动校验数据
class ProductItem(BaseModel): # id编号 + 商品文本描述
    id: str | int
    text: str


class IndexRequest(BaseModel): # /index 接口的请求体，前端一次性传一批商品列表过来灌库
    products: List[ProductItem]


class IndexResponse(BaseModel): # 灌库成功后返回：写入了多少条商品
    indexed: int


class SearchRequest(BaseModel): # /search 纯检索接口请求体：用户搜索词，可选返回条数。
    query: str
    top_k: int | None = None


class SearchItem(BaseModel): # 单条检索结果：商品 id，商品文本，向量相似度距离。
    id: str
    text: str
    distance: float


class SearchResponse(BaseModel): # 返回列表
    query: str
    items: List[SearchItem]


class ChatRequest(BaseModel):  # /chat 问答接口请求，用户问题。
    query: str
    top_k: int | None = None


class ChatResponse(BaseModel):  # RAG 问答返回：大模型生成的回答 + 检索出来的商品来源列表。
    answer: str
    sources: List[SearchItem]


# ------------------------------------------------------------------
# 依赖：拿 RagService 单例
# ------------------------------------------------------------------
async def get_rag() -> RagService:
    from app.main import rag_service
    if rag_service is None:
        raise HTTPException(status_code=503, detail="RAG 服务未就绪")
    return rag_service


# ------------------------------------------------------------------
# 接口
# ------------------------------------------------------------------
@router.post("/index", response_model=IndexResponse)
async def index(req: IndexRequest, rag: RagService = Depends(get_rag)) -> IndexResponse:
    try:
        products = [{"id": p.id, "text": p.text} for p in req.products]
        count = rag.index_products(products)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    return IndexResponse(indexed=count)


@router.post("/search", response_model=SearchResponse)
async def search(req: SearchRequest, rag: RagService = Depends(get_rag)) -> SearchResponse:
    try:
        items = rag.search(req.query, top_k=req.top_k)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    return SearchResponse(
        query=req.query,
        items=[SearchItem(**it) for it in items],
    )


@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, rag: RagService = Depends(get_rag)) -> ChatResponse:
    try:
        result = await rag.chat(req.query, top_k=req.top_k)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    return ChatResponse(
        answer=result["answer"],
        sources=[SearchItem(**s) for s in result["sources"]],
    )
