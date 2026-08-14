from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.services.llm import LLMService
from app.api.polish import router as polish_router

llm_service: LLMService | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global llm_service
    llm_service = LLMService()
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


@app.get("/healthz")
async def healthz():
    return {"status": "ok", "llm_key_configured": bool(settings.llm_api_key)}
