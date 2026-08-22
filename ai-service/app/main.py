from pathlib import Path

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.services.llm import LLMService
from app.services.embedding import BgeEmbedding
from app.services.rag import RagService
from app.api.polish import router as polish_router
from app.api.rag import router as rag_router

llm_service: LLMService | None = None
rag_service: RagService | None = None


# 自动寻找BGE模型文件夹 
def _resolve_model_dir() -> str:
    """BGE 模型目录：优先用配置，否则推算项目根 / models / bge-small-zh-v1.5。"""
    if settings.embedding_model_dir:
        return settings.embedding_model_dir
    # app/main.py → app/ → ai-service/ → 项目根
    project_root = Path(__file__).resolve().parent.parent.parent
    return str(project_root / "models" / "bge-small-zh-v1.5")

#FASTAPI 生命周期钩子
@asynccontextmanager
async def lifespan(app: FastAPI):
    global llm_service, rag_service
    llm_service = LLMService()
    # 初始化 RAG：加载 BGE 模型 + 连 ChromaDB
    try:
        embedding = BgeEmbedding(_resolve_model_dir())
        rag_service = RagService(
            embedding=embedding,
            llm=llm_service,
            chroma_path=settings.chroma_path,
            collection_name=settings.rag_collection_name,
            top_k=settings.rag_top_k,
        )
    except Exception as e:
        # 模型/向量库加载失败不阻断 FastAPI 启动，polish 等接口仍可用
        rag_service = None
        print(f"[WARN] RAG 服务初始化失败: {e}")
    yield
    await llm_service.close()


app = FastAPI(title="Campus Trade AI Service", version="0.1.0", lifespan=lifespan, debug=settings.debug)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(polish_router)
app.include_router(rag_router)


@app.get("/healthz")
async def healthz():
    return {
        "status": "ok",
        "llm_key_configured": bool(settings.llm_api_key),
        "rag_ready": rag_service is not None,
    }
