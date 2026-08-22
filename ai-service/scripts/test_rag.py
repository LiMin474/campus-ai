"""RAG 单元测试脚本：不启动 FastAPI，直接调 service 层。

验证 embedding + chromadb + llm 链路是否通畅。

跑法（cwd 必须是 ai-service，且激活 campus-ai 环境）：
    conda activate campus-ai
    cd ai-service
    python scripts/test_rag.py
"""
import asyncio
import sys
from pathlib import Path

# 把 ai-service 目录加入 sys.path，让 app 包可被 import
_AIService_ROOT = Path(__file__).resolve().parent.parent
if str(_AIService_ROOT) not in sys.path:
    sys.path.insert(0, str(_AIService_ROOT))

from app.services.embedding import BgeEmbedding
from app.services.llm import LLMService
from app.services.rag import RagService


# 项目根目录（ai-service/scripts/ → ai-service/ → 项目根）
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
MODEL_DIR = PROJECT_ROOT / "models" / "bge-small-zh-v1.5"
CHROMA_PATH = str(PROJECT_ROOT / "ai-service" / "data" / "chroma")


def main():
    print("=" * 60)
    print("RAG 单元测试")
    print("=" * 60)

    # ---- 1. 初始化 ----
    print(f"\n[1/5] 加载 BGE 模型: {MODEL_DIR}")
    embedding = BgeEmbedding(MODEL_DIR)
    print(f"      向量维度: {embedding.dim}")

    llm = LLMService()
    rag = RagService(
        embedding=embedding,
        llm=llm,
        chroma_path=CHROMA_PATH,
        collection_name="products_bge",
        top_k=3,
    )

    # ---- 2. 灌库 ----
    print("\n[2/5] 全量重灌商品数据")
    goods = [
        {"id": "1", "text": "iPad Air 2022 九成新 考研网课配套 平板电脑 32G 白色"},
        {"id": "2", "text": "考研数学李永乐全套复习资料 纸质书 几乎全新"},
        {"id": "3", "text": "捷安特山地自行车 26寸 八成新 变速 校区自提"},
        {"id": "4", "text": "小米蓝牙耳机 Air2 真无线 降噪 续航长"},
        {"id": "5", "text": "高数同济第七版教材 课后习题全解 考研复习用"},
    ]
    n = rag.index_products(goods)
    print(f"      灌入 {n} 条商品")

    # ---- 3. 纯检索 ----
    print("\n[3/5] 纯检索: '考研用平板'")
    items = rag.search("考研用平板", top_k=3)
    for i, it in enumerate(items):
        print(f"      top{i+1}: dist={it['distance']:.4f}  {it['text']}")

    assert items, "检索结果为空"
    assert "iPad" in items[0]["text"], f"第一名应为 iPad，实际: {items[0]['text']}"
    print("      ✅ 第一名是 iPad，检索正确")

    # ---- 4. 检索无关词 ----
    print("\n[4/5] 纯检索: '耳机降噪'")
    items2 = rag.search("耳机降噪", top_k=2)
    for i, it in enumerate(items2):
        print(f"      top{i+1}: dist={it['distance']:.4f}  {it['text']}")
    assert items2 and "耳机" in items2[0]["text"], "第一名应为耳机"
    print("      ✅ 第一名是耳机，检索正确")

    # ---- 5. 完整 RAG chat ----
    print("\n[5/5] 完整 RAG chat: '考研想买个平板看网课'")

    async def _run_chat():
        return await rag.chat("考研想买个平板看网课")

    result = asyncio.run(_run_chat())
    print(f"      sources ({len(result['sources'])} 条):")
    for s in result["sources"]:
        print(f"        - {s['text']}")
    print(f"      answer:\n{result['answer']}")

    # 注：llm.close() 在 asyncio.run 后会报 event loop closed，
    # 但脚本即将退出，httpx client 会随进程清理，不显式 close 也无妨。

    print("\n" + "=" * 60)
    print("✅ 全部测试通过")
    print("=" * 60)


if __name__ == "__main__":
    main()
