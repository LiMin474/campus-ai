from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

# ai-service/.env 的绝对路径（无论从哪个 CWD 启动都能读到）
_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=str(_ENV_FILE), env_file_encoding="utf-8", extra="ignore")

    host: str = "0.0.0.0"
    port: int = 8001
    debug: bool = True

    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com"
    llm_model: str = "deepseek-chat"
    llm_timeout: int = 60

    java_base_url: str = "http://localhost:8080"
    java_timeout: int = 10

    jwt_secret: str = ""

    # ========== RAG 配置 ==========
    # BGE 模型路径：留空则用脚本相对路径推算（项目根 / models / bge-small-zh-v1.5）
    embedding_model_dir: str = ""
    chroma_path: str = "data/chroma"
    rag_collection_name: str = "products_bge"
    rag_top_k: int = 3


settings = Settings()
