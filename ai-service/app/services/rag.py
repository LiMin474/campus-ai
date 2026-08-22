"""RAG 业务逻辑：商品检索 + LLM 生成。

编排 embedding + chromadb + llm，对外暴露 index / search / chat 三个能力。
- index: 全量重灌（方案 A），清空 collection 后写入
- search: 纯检索，返回 top_k 商品文本
- chat: 完整 RAG，检索 + 拼 prompt + LLM 生成
"""
from typing import List, Dict, Any

import chromadb

from app.services.embedding import BgeEmbedding
from app.services.llm import LLMService


# RAG 系统 prompt：约束 LLM 只基于检索到的商品作答，避免幻觉
RAG_SYSTEM_PROMPT = (
    "你是校园二手交易平台的购物助手。请根据下方提供的商品信息回答用户问题，"
    "推荐合适的商品。如果商品列表中没有相关商品，请如实告知用户。"
    "不要编造不存在的商品或参数。"
)


class RagService:
    def __init__(
        self,
        embedding: BgeEmbedding,
        llm: LLMService,
        chroma_path: str,
        collection_name: str = "products_bge",
        top_k: int = 3,
    ):
        self.embedding = embedding
        self.llm = llm
        self.top_k = top_k

        client = chromadb.PersistentClient(path=chroma_path)
        # 用 get_or_create：灌库时再清空，避免首次启动报错
        self.collection = client.get_or_create_collection(collection_name)  # 如果集合已经存在就获取；不存在就新建一个空集合。

    # ------------------------------------------------------------------
    # index: 全量重灌 先删光旧的所有商品，再把新商品一次性写入。适合后台更新商品后重新灌库。
    # ------------------------------------------------------------------
    def index_products(self, products: List[Dict[str, Any]]) -> int:
        """全量重灌商品数据。先清空 collection 再写入。

        Args:
            products: [{"id": "1", "text": "商品描述"}, ...]

        Returns:
            写入条数
        """
        if not products:
            return 0

        # 清空旧数据（全量重灌策略）
        existing_ids = self.collection.get().get("ids", [])
        if existing_ids:
            self.collection.delete(ids=existing_ids)

        ids = [str(p["id"]) for p in products]
        documents = [p["text"] for p in products]
        embeddings = self.embedding.encode(documents)

        self.collection.upsert(ids=ids, documents=documents, embeddings=embeddings)
        return len(ids)

    # ------------------------------------------------------------------
    # search: 纯检索
    # ------------------------------------------------------------------
    def search(self, query: str, top_k: int | None = None) -> List[Dict[str, Any]]:
        """纯向量检索，返回 top_k 商品。

        Args:
            query: 用户查询文本
            top_k: 返回条数，默认用 self.top_k

        Returns:
            [{"id": "1", "text": "iPad...", "distance": 0.59}, ...]
            按距离从小到大（越相似越前）
        """
        if not query or not query.strip():
            return []

        k = top_k or self.top_k
        query_emb = self.embedding.encode([query])

        # 核心：Chroma 拿查询向量，在库里面比对所有商品向量，返回距离最小最相似的 k 条。
        result = self.collection.query(query_embeddings=query_emb, n_results=k)

        items = []
        ids = result.get("ids", [[]])[0]
        documents = result.get("documents", [[]])[0]
        distances = result.get("distances", [[]])[0]
        for i, doc in enumerate(documents):
            items.append({
                "id": ids[i] if i < len(ids) else "",
                "text": doc,
                "distance": float(distances[i]) if i < len(distances) else 0.0,
            })
        return items

    # ------------------------------------------------------------------
    # chat: 完整 RAG（检索 + 生成）
    # ------------------------------------------------------------------
    async def chat(self, query: str, top_k: int | None = None) -> Dict[str, Any]:
        """完整 RAG：检索 top_k 商品 → 拼 prompt → LLM 生成。

        Args:
            query: 用户问题
            top_k: 检索条数

        Returns:
            {
                "answer": "LLM 生成的回答",
                "sources": [{"id": "1", "text": "iPad...", "distance": 0.59}, ...]
            }
        """
        if not query or not query.strip():
            raise ValueError("查询不能为空")

        # 1) 检索相关商品
        sources = self.search(query, top_k=top_k)

        # 2) 拼 prompt
        if sources:
            goods_text = "\n".join(
                f"{i+1}. {s['text']}" for i, s in enumerate(sources)
            )
            user_content = (
                f"以下是检索到的相关商品：\n{goods_text}\n\n"
                f"用户问题：{query.strip()}"
            )
        else:
            user_content = f"未检索到相关商品。\n用户问题：{query.strip()}"

        messages = [
            {"role": "system", "content": RAG_SYSTEM_PROMPT},
            {"role": "user", "content": user_content},
        ]

        # 3) LLM 生成
        answer = await self.llm.chat(messages)

        return {
            "answer": answer,
            "sources": sources,
        }
