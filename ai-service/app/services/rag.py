"""RAG 业务逻辑：商品检索 + LLM 生成。

编排 embedding + chromadb + llm，对外暴露 index / search / chat 三个能力。
- index: 全量重灌（方案 A），清空 collection 后写入
- search: 纯检索，返回 top_k 商品文本
- chat: 完整 RAG（Function Calling 循环），LLM 自主决策/澄清/生成
"""
import asyncio
import json
import time
from typing import List, Dict, Any

import chromadb

from app.services.embedding import BgeEmbedding
from app.services.llm import LLMService


# 模拟流式时的最小 yield 间隔（秒）：控制打字机节奏
# 每个 delta 2~3 字符 × 22ms ≈ 90~130 字/秒，中文阅读舒适速度
# 真实 LLM SSE 如果一次性把所有 delta 推过来，也会按这个节奏节流，
# 避免 100ms 内发完导致前端"看起来非流式"
MIN_DELTA_INTERVAL = 0.022


# 成色 -> 数字 映射表（全新=10，数字越大成色越好）
# 覆盖实际 seed 数据中可能出现的说法
CONDITION_MAP = {
    "全新": 10,
    "99新": 9.9,
    "几乎全新": 9.6,
    "九五新": 9.5,
    "九成新": 9,
    "85新": 8.5,
    "八成新": 8,
    "七成新": 7,
    "六成新": 6,
    "五成新": 5,
}


def condition_to_num(condition: str | None) -> float | None:
    if not condition:
        return None
    c = str(condition).strip()
    return CONDITION_MAP.get(c)


# ------------------------------------------------------------------
# Agent 总 System Prompt：约束 LLM 做什么/不做什么
#   - 参数判断规则（带数字→硬过滤，模糊词→排序偏好）
#   - 澄清决策规则（影响查找时才问）
#   - few-shot 示例
#   - 4 步结构强约束输出
# ------------------------------------------------------------------
AGENT_SYSTEM_PROMPT = """# 角色设定
你是校园二手交易平台的 AI 购物助手。你的职责是：帮助用户在二手商品库中找到最符合其需求的商品。

重要规则：
1. 绝对不要编造不存在的商品或参数。所有推荐必须基于工具返回的真实商品信息。
2. 不要回答二手商品查找以外的问题（如"怎么注销账号""买火车票"），直接告诉用户你的职责范围。
3. 商品介绍顺序必须严格按照 search_products 工具返回的顺序，禁止自行重新排序。

====================
参数判断规则（调用 search_products 时必须严格遵守）
====================

1) 【query】必填：用户想要找的物品关键词（如 平板、考研资料、滑板、AirPods）

2) 【min_price / max_price】价格区间
   · 用户明确说了具体数字价格：
     - "不低于 3000""3000 起步""3000 以上" → min_price
     - "3000 以内""不超过 3000""最多 3000" → max_price
     - "1000 到 3000 之间""1000~3000" → min_price + max_price 同时传
   · 用户只说模糊词（"便宜点""贵点无所谓""性价比高"）→ 绝对不要传 min_price/max_price，
     改用 sort_by=price_asc（便宜优先）或 sort_by=price_desc（贵优先）做排序偏好

3) 【min_condition / max_condition】成色区间
   · 用户明确说了具体成色数值：
     - "九成新以上""至少九五新" → min_condition（值：九成新/九五新……）
     - "八成新以内""不要超过八成新""最多八成新" → max_condition
   · 用户说模糊词（"不要太新""旧一点也行""稍微新点""别太旧"）→ 绝对不要传 min_condition/max_condition，
     改用 sort_by：
     - "不要太新""旧一点""旧点也行" → sort_by = condition_desc（成色从旧到新排，越旧越靠前）
     - "不要太旧""稍微新点""新一点更好" → sort_by = condition_asc（成色从新到旧排，越新越靠前）

4) 【keywords_exclude】排除词（list of string）
   · 当用户明确说"不要XX""排除XX""不要XX那种"时使用，例如：
     "滑板，不要长板" → keywords_exclude = ["长板"]
     "手机，不要苹果的" → keywords_exclude = ["苹果", "iPhone"]
   · 商品文本如果包含任何一个排除词，就会被过滤掉
   · 没说排除时，绝对不要传这个字段

5) 【category_hint】分类软提示（string，可选）
   · 当用户明确指定了大分类（"我要书本类的考研资料""我要数码类的耳机"），可以把用户说的分类名传过来
   · 注意：这是软提示，不是强过滤，最终要不要看由检索排序和你生成文案时决定

6) 【sort_by / sort_weight】排序偏好
   · sort_by 可选枚举：
     - "relevance"（默认，纯向量相关性，一般不传）
     - "price_asc"：价格越低越靠前
     - "price_desc"：价格越高越靠前
     - "condition_asc"：成色越新越靠前
     - "condition_desc"：成色越旧越靠前
   · sort_weight（0~1 之间的小数，默认 0.5）
     - 越接近 0 → 越看重相关性；越接近 1 → 越看重排序偏好
     - 一般不传，用默认 0.5 即可；用户说"完全不管相关性只看便宜"才传接近 1 的值（如 0.8）

====================
澄清决策规则（只在严重影响查找时才问用户）
====================

遇到以下任一种情况时，请【不要调用 search_products 工具】，直接回复一段澄清话术给用户：

  1) 完全没说想要找什么物品或大分类（例如："给我推荐点好东西吧""随便看看""有啥便宜的"）
     → 澄清示例："请问您想找哪类商品呢？可选：1️⃣ 数码（平板/耳机/手机…）2️⃣ 书本（考研资料/教材…）3️⃣ 出行（滑板/自行车…）4️⃣ 生活物品"

  2) 关键词严重多义词，且上下文完全无法推断（例如：只说"我要苹果"，不说"吃的"还是"数码"）
     → 澄清示例："您说的『苹果』是指水果还是 Apple 品牌的数码产品呢？我好帮您精准查找。"

  3) 约束明显冲突（例如："全新 iPad 100 元以内""99 新九成新以上"自相矛盾）
     → 澄清示例："全新 iPad 正常价格通常在 2000 元以上，与您 100 元的预算有冲突。请问您希望：① 放宽预算？还是 ② 降低成色要求（九成新也可以）？"

  4) 问题超出二手商品查找范围（例如："怎么注销账号""帮我买火车票""平台怎么注册"）
     → 澄清示例："我是校园二手购物助手，只负责推荐商品。关于账号注销/注册，请前往个人中心或联系客服处理。"

其它任何"轻微模糊"（如价格/成色模糊词、OR 多类需求等），一律不要打扰用户，
直接用你的参数判断力和 sort_by 排序偏好兜底。

====================
few-shot 参数示例（请遵循以上规则举一反三）
====================

示例 1（带数字 + 多条件）：
  用户：「平板，3000 以内，九成新以上」
  正确参数：{"query": "平板", "max_price": 3000, "min_condition": "九成新"}

示例 2（价格下限，注意不是 max_price！）：
  用户：「平板，不要低于 3000 元的，我要一个高端点的」
  正确参数：{"query": "平板", "min_price": 3000}

示例 3（价格区间）：
  用户：「考研复习全书，价格 10 块到 50 之间」
  正确参数：{"query": "考研复习全书", "min_price": 10, "max_price": 50}

示例 4（成色上限）：
  用户：「自行车，不要太新的，八成新以内吧，我也不想花太多」
  正确参数：{"query": "自行车", "max_condition": "八成新"}

示例 5（成色模糊词 → sort_by，不要 max_condition！）：
  用户：「平板，不要太新，便宜点就行」
  正确参数：{"query": "平板", "sort_by": "condition_desc", "sort_weight": 0.5}
  错误做法：{"max_condition": "八成新"} ← 粗暴强过滤，会误杀九成新的好商品

示例 6（排除词）：
  用户：「滑板，不要长板那种，我要双翘板」
  正确参数：{"query": "滑板", "keywords_exclude": ["长板"]}

示例 7（模糊便宜 → 排序，不要 max_price！）：
  用户：「耳机，便宜点就行，给我排一下看看」
  正确参数：{"query": "耳机", "sort_by": "price_asc"}
  错误做法：{"max_price": 100} ← 自己瞎编了用户没说的数字 100，错了

示例 8（分类软提示 + 带数字成色）：
  用户：「给我书本类的考研资料，九成新以上优先」
  正确参数：{"query": "考研资料", "category_hint": "书本", "min_condition": "九成新"}

====================
多轮对话规则（当对话历史中已有上下文时）
====================

当对话历史中已有之前的问答时，你需要结合上下文理解用户当前意图：

  1) 【延续搜索】用户说"有更便宜的吗""再便宜点""换个颜色"等延续词
     → query 沿用上一轮的物品关键词，调整 sort_by 或价格约束
     → 例：上一轮"我要平板，3000以内" → 本轮"有更便宜的吗"
       正确参数：{"query": "平板", "max_price": 3000, "sort_by": "price_asc"}
       错误做法：{"query": "更便宜"} ← query 不能写成"更便宜"，向量检索会乱

  2) 【直接回答】用户问"那个多少钱""第一个多少钱""它有保修吗"
     → 如果用户问的商品在上一轮工具结果中已返回，不要调工具，直接从上一轮结果里找价格回答
     → 例：上一轮推荐了 iPad Air 2800 元 → 本轮"那个多少钱"
       正确行为：不调工具，直接回答"上一轮推荐的 iPad Air 价格为 2800 元"

  3) 【切换需求】用户说"那有没有耳机""换个品类看看"
     → 这是新需求，正常调工具，query 用新的关键词

示例 9（多轮 · 延续搜索）：
  上一轮用户：「我要个平板，3000以内」→ 你推荐了 iPad Air 2800
  本轮用户：「有更便宜的吗」
  正确参数：{"query": "平板", "max_price": 2800, "sort_by": "price_asc"}
  说明：query 沿用"平板"，max_price 从上一轮推荐价格下探，sort_by 改成价格升序

示例 10（多轮 · 直接回答，不调工具）：
  上一轮用户：「推荐个耳机」→ 你推荐了 AirPods Pro 2 900元
  本轮用户：「那个多少钱」
  正确行为：不调用 search_products，直接回答："上一轮推荐的 AirPods Pro 2 价格为 900 元，九成新。"

====================
最终回答输出格式（必须严格按 4 步结构输出，不得跳步，不得用其他结构）
====================

【步骤 1：需求重述】（必填，必须写）
  用一句话复述用户的原始需求，包含：物品是什么、价格约束、成色约束、排序偏好、分类偏好等。
  目的：让用户一眼确认你理解对了，没有语义偏差。
  例："您要找：平板类商品，价格在 3000 元以内，成色九成新以上，优先越便宜越好。"

【步骤 2：匹配商品】（必填，必须写。若工具返回结果为空，则写"暂无匹配商品，建议放宽条件。"）
  按工具返回的【给定顺序】介绍商品，禁止自行重新排序！
  每件商品单独一段，编号 ①②③…
  每件商品必须写出这些信息（如果缺失就写"—"）：
    · 编号 + 标题/名称
    · 价格
    · 成色
    · 推荐理由（结合步骤 1 的用户需求，说清楚"为什么这件商品适合你"）
  ★ 每件商品介绍完后，必须另起一行写商品 ID 引用标记，格式为 [[商品ID]]
    （例如商品 id 为 11，就写 [[11]]）。前端会在此位置自动渲染该商品的可点击卡片。
    每件商品都必须写对应的 [[ID]] 标记，缺一不可！

【步骤 3：差异告知】（必填，哪怕完全匹配也要写）
  逐项对比步骤 1 的需求和步骤 2 的商品实际情况，回答两个问题：
  a) 用户提出的所有【价格/成色/分类/排除词】硬约束，是否全部都被满足？
     → 如果全部满足：写 ✅  所有硬约束都满足您的需求。
     → 如果有未满足：写 ⚠️  存在以下差异：逐条列出（例如：您要求全新的平板，当前在售的都是九成新；您要的 iPad Pro 暂无在售，已用性能接近的 iPad Air 替代）
  b) 有没有用户明确提到的品牌/品类/型号，在商品库里完全搜不到？有就写，没有就跳过。

【步骤 4：下一步建议】（必填，至少写 1~2 条可操作的建议）
  例：
    · "需要我放宽预算到 3500 看看有没有更新的平板吗？"
    · "也可以搜一下考研复习资料或 Kindle 电子书阅读器，搭平板一起学习很合适。"
    · "如果对成色没那么严格，可以试试把『九成新以上』放宽到『八成新以内』，可能有更多实惠的选择。"
"""


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
        self.collection = client.get_or_create_collection(collection_name)

    # ------------------------------------------------------------------
    # index: 全量重灌（清空再写入）
    # ------------------------------------------------------------------
    def index_products(self, products: List[Dict[str, Any]]) -> int:
        if not products:
            return 0

        existing_ids = self.collection.get().get("ids", [])
        if existing_ids:
            self.collection.delete(ids=existing_ids)

        ids = [str(p["id"]) for p in products]
        documents = [p["text"] for p in products]
        embeddings = self.embedding.encode(documents)

        metadatas = [
            {
                k: v
                for k, v in {
                    "price": p.get("price"),
                    "condition": p.get("condition"),
                }.items()
                if v is not None
            }
            for p in products
        ]

        self.collection.upsert(
            ids=ids,
            documents=documents,
            embeddings=embeddings,
            metadatas=metadatas,
        )
        return len(ids)

    def upsert_products(self, products: List[Dict[str, Any]]) -> int:
        """增量灌库：按 id 覆盖写入，不删其它商品（商品发布/编辑时调用）"""
        if not products:
            return 0
        ids = [str(p["id"]) for p in products]
        documents = [p["text"] for p in products]
        embeddings = self.embedding.encode(documents)
        metadatas = [
            {
                k: v
                for k, v in {
                    "price": p.get("price"),
                    "condition": p.get("condition"),
                }.items()
                if v is not None
            }
            for p in products
        ]
        self.collection.upsert(
            ids=ids,
            documents=documents,
            embeddings=embeddings,
            metadatas=metadatas,
        )
        return len(ids)

    def delete_product(self, product_id: str) -> int:
        """按商品 id 从向量库删除（商品下架/删除时调用）"""
        try:
            existing = self.collection.get(ids=[product_id]).get("ids", [])
        except Exception:
            existing = []
        if not existing:
            return 0
        self.collection.delete(ids=existing)
        return len(existing)

    # ------------------------------------------------------------------
    # search: 纯检索（带 metadata）
    # ------------------------------------------------------------------
    def search(self, query: str, top_k: int | None = None) -> List[Dict[str, Any]]:
        if not query or not query.strip():
            return []

        k = top_k or self.top_k
        query_emb = self.embedding.encode([query])

        result = self.collection.query(
            query_embeddings=query_emb,
            n_results=k,
            include=["documents", "distances", "metadatas"],
        )

        ids = result.get("ids", [[]])[0]
        documents = result.get("documents", [[]])[0]
        distances = result.get("distances", [[]])[0]
        metadatas = result.get("metadatas", [[]])[0]
        items = []
        for i, doc in enumerate(documents):
            meta = metadatas[i] if i < len(metadatas) and metadatas[i] else {}
            items.append({
                "id": ids[i] if i < len(ids) else "",
                "text": doc,
                "distance": float(distances[i]) if i < len(distances) else 0.0,
                "price": meta.get("price"),
                "condition": meta.get("condition"),
            })
        return items

    # ------------------------------------------------------------------
    # Function Calling 工具定义（9 参数）
    # ------------------------------------------------------------------
    SEARCH_PRODUCTS_TOOL = {
        "type": "function",
        "function": {
            "name": "search_products",
            "description": "检索校园二手商品库，支持价格区间、成色区间、排除词过滤，并可按价格/成色偏好与相关性融合排序。",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "商品关键词（必填），如 平板、考研资料、滑板、AirPods",
                    },
                    "min_price": {
                        "type": "number",
                        "description": "价格下限（元）。仅当用户明确说了具体数字（如不低于 3000、1000 以上）时使用。",
                    },
                    "max_price": {
                        "type": "number",
                        "description": "价格上限（元）。仅当用户明确说了具体数字（如 3000 以内、最多 3000、1000~3000 的上限）时使用。",
                    },
                    "min_condition": {
                        "type": "string",
                        "enum": ["全新", "99新", "几乎全新", "九五新", "九成新", "85新", "八成新", "七成新", "六成新", "五成新"],
                        "description": "最低成色（含）。仅当用户明确说了具体成色数字（九成新以上、至少九五新）时使用。模糊词不要用这个，改用 sort_by。",
                    },
                    "max_condition": {
                        "type": "string",
                        "enum": ["全新", "99新", "几乎全新", "九五新", "九成新", "85新", "八成新", "七成新", "六成新", "五成新"],
                        "description": "最高成色（含）。仅当用户明确说了具体成色数字（八成新以内、不要超过八成新）时使用。模糊词不要用这个，改用 sort_by。",
                    },
                    "keywords_exclude": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "排除关键词列表。商品文本中包含任何一个词就会被过滤。仅当用户明确说『不要/排除XX』时使用。",
                    },
                    "category_hint": {
                        "type": "string",
                        "description": "分类软提示。当用户明确指定了大分类（书本类、数码类、生活物品等）时传入用户说的分类名。不作为强过滤，仅作为检索和生成文案时的参考。",
                    },
                    "sort_by": {
                        "type": "string",
                        "enum": ["relevance", "price_asc", "price_desc", "condition_asc", "condition_desc"],
                        "description": "排序偏好。默认 relevance（纯相关性，一般不传）。用户说模糊词（便宜点/越新越好/不要太新）时使用对应枚举：price_asc 价格低到高、price_desc 价格高到低、condition_asc 成色新到旧、condition_desc 成色旧到新。",
                    },
                    "sort_weight": {
                        "type": "number",
                        "minimum": 0,
                        "maximum": 1,
                        "description": "排序权重（0~1），默认 0.5。越接近 1 越看重 sort_by 偏好，越接近 0 越看重相关性。一般不传。",
                    },
                },
                "required": ["query"],
            },
        },
    }

    # ------------------------------------------------------------------
    # search_products 工具执行：召回 → 过滤 → RRF 排序
    # ------------------------------------------------------------------
    def search_products(
        self,
        query: str,
        min_price: float | None = None,
        max_price: float | None = None,
        min_condition: str | None = None,
        max_condition: str | None = None,
        keywords_exclude: List[str] | None = None,
        category_hint: str | None = None,
        sort_by: str = "relevance",
        sort_weight: float = 0.5,
        top_k: int = 10,
    ) -> Dict[str, Any]:
        """向量检索 top_k 候选 → 按参数过滤 → (可选)RRF 融合排序。

        只对 top_k 条操作，保证数据量大时不崩。
        category_hint 本层作为信息保留传给 LLM 文案阶段，不参与硬过滤。
        """
        # 1) 向量检索召回候选（带相关性距离、metadata）
        candidates = self.search(query, top_k=top_k)

        # 2) 参数归一化
        min_price_f = float(min_price) if min_price is not None else None
        max_price_f = float(max_price) if max_price is not None else None
        min_cond_num = condition_to_num(min_condition)
        max_cond_num = condition_to_num(max_condition)
        kws_exclude = [k.strip() for k in keywords_exclude if k and k.strip()] if keywords_exclude else []

        # 3) 过滤层：只对 top_k 候选逐条件判断。规则：有明确值且商品对应字段存在+不满足才排除；否则保留
        results = []
        for c in candidates:
            # 价格下限
            price = c.get("price")
            if min_price_f is not None and price is not None:
                try:
                    if float(price) < min_price_f:
                        continue
                except (TypeError, ValueError):
                    pass
            # 价格上限
            if max_price_f is not None and price is not None:
                try:
                    if float(price) > max_price_f:
                        continue
                except (TypeError, ValueError):
                    pass
            # 成色下限
            cond = c.get("condition")
            cond_num = condition_to_num(cond)
            if min_cond_num is not None and cond_num is not None:
                if cond_num < min_cond_num:
                    continue
            # 成色上限
            if max_cond_num is not None and cond_num is not None:
                if cond_num > max_cond_num:
                    continue
            # 排除词（商品描述文本中包含任何一个就排除）
            text = c.get("text") or ""
            hit_exclude = False
            for kw in kws_exclude:
                if kw and kw in text:
                    hit_exclude = True
                    break
            if hit_exclude:
                continue
            results.append(c)

        # 4) 排序层：RRF 融合（仅 sort_by 不是 relevance 且结果条数 >=2 时才做）
        if sort_by != "relevance" and len(results) >= 2:
            # a) 信号A：相关性排名（距离从小到大排 → rank=1 最相关）
            sorted_rel = sorted(range(len(results)), key=lambda i: results[i].get("distance", 999))
            rank_rel = {idx: (r + 1) for r, idx in enumerate(sorted_rel)}

            # b) 信号B：偏好排名（依据 sort_by 枚举）
            def biz_key(item):
                """返回用于排序的可比较 key；缺失字段排中间/最后不影响主排序。"""
                if sort_by in ("price_asc", "price_desc"):
                    p = item.get("price")
                    try:
                        v = float(p)
                    except (TypeError, ValueError):
                        v = float("inf")  # 缺失价格，放到偏好排序末尾
                    return v
                elif sort_by in ("condition_asc", "condition_desc"):
                    v = condition_to_num(item.get("condition"))
                    if v is None:
                        return -1  # 缺失成色，放到偏好排序末尾
                    return v
                return 0

            reverse = sort_by in ("price_desc", "condition_asc")  # 新/贵：值越大排前（升序取反=降序）
            sorted_biz = sorted(range(len(results)), key=lambda i: biz_key(results[i]), reverse=reverse)
            rank_biz = {idx: (r + 1) for r, idx in enumerate(sorted_biz)}

            # c) RRF 融合：score = (1 - sw) * 1/(k + rank_rel) + sw * 1/(k + rank_biz)
            k_rrf = 60
            sw = sort_weight if 0 <= sort_weight <= 1 else 0.5
            sw_rel = 1.0 - sw
            sw_biz = sw

            def rrf_score(idx):
                a = 1.0 / (k_rrf + rank_rel[idx])
                b = 1.0 / (k_rrf + rank_biz[idx])
                return sw_rel * a + sw_biz * b

            idx_sorted = sorted(range(len(results)), key=rrf_score, reverse=True)
            results = [results[i] for i in idx_sorted]
        # 否则（relevance 或 结果少）：保持检索的距离升序即可

        # 5) 返回（带上所有过滤参数、排序参数，方便 chat() 组装 tool 消息时给第二次 LLM 看）
        return {
            "query": query,
            "filters": {
                "min_price": min_price,
                "max_price": max_price,
                "min_condition": min_condition,
                "max_condition": max_condition,
                "keywords_exclude": keywords_exclude or [],
                "category_hint": category_hint,
            },
            "sort": {
                "sort_by": sort_by,
                "sort_weight": sort_weight,
            },
            "results": results,
        }

    # ------------------------------------------------------------------
    # chat()：Function Calling 循环
    #   - 第一次 LLM：带 tools + system prompt
    #     → 有 tool_calls：执行 search_products，再第二次 LLM
    #     → 无 tool_calls：直接返回（澄清话术/闲聊）
    #   - 组装 tool 消息时：开头强制怼上用户原提问 + 工具执行参数摘要（经验 151210）
    # ------------------------------------------------------------------
    async def chat(self, query: str, top_k: int | None = None) -> Dict[str, Any]:
        if not query or not query.strip():
            raise ValueError("查询不能为空")

        user_query_text = query.strip()

        # 1) 第一轮：带 tools + AGENT_SYSTEM_PROMPT，让 LLM 决策调工具还是澄清
        messages = [
            {"role": "system", "content": AGENT_SYSTEM_PROMPT},
            {"role": "user", "content": user_query_text},
        ]
        first = await self.llm.chat_complete(messages, tools=[self.SEARCH_PRODUCTS_TOOL])

        # 2) 无 tool_calls → 直接返回文本（澄清/闲聊/超出范围）
        if not first.get("tool_calls"):
            return {
                "answer": first.get("content") or "（AI 没有返回回答）",
                "sources": [],
            }

        # 3) 执行工具（支持多个 tool_calls，但我们只有 search_products 一个工具）
        sources: List[Dict[str, Any]] = []
        latest_tool_result: Dict[str, Any] | None = None
        for tc in first["tool_calls"]:
            fn = tc.get("function", {})
            name = fn.get("name", "")
            if name != "search_products":
                continue
            try:
                args = json.loads(fn.get("arguments") or "{}")
            except json.JSONDecodeError:
                args = {}

            tool_result = self.search_products(
                query=args.get("query") or user_query_text,
                min_price=args.get("min_price"),
                max_price=args.get("max_price"),
                min_condition=args.get("min_condition"),
                max_condition=args.get("max_condition"),
                keywords_exclude=args.get("keywords_exclude"),
                category_hint=args.get("category_hint"),
                sort_by=args.get("sort_by") or "relevance",
                sort_weight=float(args.get("sort_weight", 0.5) or 0.5),
                top_k=top_k or 10,
            )
            latest_tool_result = tool_result
            sources = tool_result.get("results", [])

            # 4) 把工具执行结果作为 tool 消息回传，【开头强制怼上用户原提问和执行参数】
            #    （经验 151210：防止第二次 LLM 忽略历史 user 消息，直接只看工具 JSON）
            filt = tool_result.get("filters", {})
            sort = tool_result.get("sort", {})
            exec_params_parts = [f"query={tool_result.get('query')}"]
            for k in ("min_price", "max_price", "min_condition", "max_condition"):
                if filt.get(k) is not None:
                    exec_params_parts.append(f"{k}={filt[k]}")
            if filt.get("keywords_exclude"):
                exec_params_parts.append(f"keywords_exclude={filt['keywords_exclude']}")
            if filt.get("category_hint"):
                exec_params_parts.append(f"category_hint={filt['category_hint']}")
            if sort.get("sort_by") and sort["sort_by"] != "relevance":
                exec_params_parts.append(f"sort_by={sort['sort_by']}")
                if sort.get("sort_weight", 0.5) != 0.5:
                    exec_params_parts.append(f"sort_weight={sort['sort_weight']}")
            exec_params_summary = ", ".join(exec_params_parts)

            tool_message_content = (
                f"【用户原始需求】{user_query_text}\n\n"
                f"【工具执行参数】{exec_params_summary}\n\n"
                f"【匹配商品数量】{len(sources)} 件。\n\n"
                f"【工具返回商品列表（已按给定顺序排好）】：\n"
                f"{json.dumps(sources, ensure_ascii=False, indent=2)}\n\n"
                f"【输出要求】请严格遵守 system prompt 里的【4 步结构】回答用户：需求重述 → 匹配商品 → 差异告知 → 下一步建议。"
                f"介绍商品时必须严格按照上面 JSON 列表的给定顺序，禁止自行重新排序。"
            )

            messages.append(first)
            messages.append({
                "role": "tool",
                "tool_call_id": tc.get("id", ""),
                "content": tool_message_content,
            })

        # 5) 第二轮：LLM 基于（system + 历史 + tool 强约束消息）生成最终回答
        second = await self.llm.chat_complete(messages)
        answer = second.get("content") or (
            "已为您检索到相关商品，详情请看下方商品卡片。"
        )

        return {
            "answer": answer,
            "sources": sources,
        }

    # ------------------------------------------------------------------
    # chat_stream()：流式版 chat（SSE 打字机效果）
    #   事件格式（每行一个 JSON）：
    #     {"type": "meta", "sources": [...], "query": "..."}  先发元信息
    #     {"type": "delta", "text": "..."}                    增量文本
    #     {"type": "done"}                                     结束
    #   与 chat() 的区别：第二轮用 chat_complete_stream 增量 yield。
    # ------------------------------------------------------------------
    async def chat_stream(self, query: str, history: list[dict] | None = None, top_k: int | None = None):
        if not query or not query.strip():
            raise ValueError("查询不能为空")

        user_query_text = query.strip()

        # 1) 第一轮：Function Calling 决策（必须等完整响应）
        messages = [
            {"role": "system", "content": AGENT_SYSTEM_PROMPT},
        ]
        # 多轮对话：把历史消息插入 system 和当前 query 之间
        # 让 LLM 知道之前聊了什么，能理解"再便宜点""那个"等指代
        if history:
            for h in history:
                role = h.get("role", "user")
                content = h.get("content", "")
                if not content.strip():
                    continue
                # 工具结果记忆：assistant 消息若带上轮检索的商品列表（sources），
                # 格式化为文本附在 content 后，让 LLM 能精确回答
                # "第一个多少钱""那个成色怎样"等指代问题
                sources = h.get("sources")
                if role == "assistant" and isinstance(sources, list) and sources:
                    rows = []
                    for i, s in enumerate(sources, 1):
                        title = s.get("title") or s.get("text") or ""
                        price = s.get("price")
                        cond = s.get("condition")
                        desc = s.get("text") or ""
                        # 描述文本可能较长（含整条向量化文本），截断到 80 字防 token 膨胀
                        if len(desc) > 80:
                            desc = desc[:80] + "…"
                        rows.append(
                            f"第{i}个：id={s.get('id')}, 标题={title}, "
                            f"价格={price if price is not None else '未知'}元, "
                            f"成色={cond if cond else '未知'}, "
                            f"描述={desc}"
                        )
                    content = (
                        content
                        + "\n\n【上一轮工具返回的商品列表（按顺序编号）】\n"
                        + "\n".join(rows)
                    )
                messages.append({"role": role, "content": content})
        messages.append({"role": "user", "content": user_query_text})
        first = await self.llm.chat_complete(messages, tools=[self.SEARCH_PRODUCTS_TOOL])

        # 2) 无 tool_calls → 直接返回文本（澄清/闲聊），按节奏分片 yield 实现打字机
        if not first.get("tool_calls"):
            text = first.get("content") or "（AI 没有返回回答）"
            yield {"type": "meta", "sources": [], "query": user_query_text}
            # 每 2 字符一个 delta，按 MIN_DELTA_INTERVAL 节流出
            for i in range(0, len(text), 2):
                yield {"type": "delta", "text": text[i:i + 2]}
                await asyncio.sleep(MIN_DELTA_INTERVAL)
            yield {"type": "done"}
            return

        # 3) 执行工具（与 chat() 相同的组装逻辑）
        sources: List[Dict[str, Any]] = []
        latest_tool_result: Dict[str, Any] | None = None
        for tc in first["tool_calls"]:
            fn = tc.get("function", {})
            name = fn.get("name", "")
            if name != "search_products":
                continue
            try:
                args = json.loads(fn.get("arguments") or "{}")
            except json.JSONDecodeError:
                args = {}

            tool_result = self.search_products(
                query=args.get("query") or user_query_text,
                min_price=args.get("min_price"),
                max_price=args.get("max_price"),
                min_condition=args.get("min_condition"),
                max_condition=args.get("max_condition"),
                keywords_exclude=args.get("keywords_exclude"),
                category_hint=args.get("category_hint"),
                sort_by=args.get("sort_by") or "relevance",
                sort_weight=float(args.get("sort_weight", 0.5) or 0.5),
                top_k=top_k or 10,
            )
            latest_tool_result = tool_result
            sources = tool_result.get("results", [])

            filt = tool_result.get("filters", {})
            sort = tool_result.get("sort", {})
            exec_params_parts = [f"query={tool_result.get('query')}"]
            for k in ("min_price", "max_price", "min_condition", "max_condition"):
                if filt.get(k) is not None:
                    exec_params_parts.append(f"{k}={filt[k]}")
            if filt.get("keywords_exclude"):
                exec_params_parts.append(f"keywords_exclude={filt['keywords_exclude']}")
            if filt.get("category_hint"):
                exec_params_parts.append(f"category_hint={filt['category_hint']}")
            if sort.get("sort_by") and sort["sort_by"] != "relevance":
                exec_params_parts.append(f"sort_by={sort['sort_by']}")
                if sort.get("sort_weight", 0.5) != 0.5:
                    exec_params_parts.append(f"sort_weight={sort['sort_weight']}")
            exec_params_summary = ", ".join(exec_params_parts)

            tool_message_content = (
                f"【用户原始需求】{user_query_text}\n\n"
                f"【工具执行参数】{exec_params_summary}\n\n"
                f"【匹配商品数量】{len(sources)} 件。\n\n"
                f"【工具返回商品列表（已按给定顺序排好）】：\n"
                f"{json.dumps(sources, ensure_ascii=False, indent=2)}\n\n"
                f"【输出要求】请严格遵守 system prompt 里的【4 步结构】回答用户：需求重述 → 匹配商品 → 差异告知 → 下一步建议。"
                f"介绍商品时必须严格按照上面 JSON 列表的给定顺序，禁止自行重新排序。"
            )

            messages.append(first)
            messages.append({
                "role": "tool",
                "tool_call_id": tc.get("id", ""),
                "content": tool_message_content,
            })

        # 4) 先发元信息（含 sources，让前端能提前渲染商品卡片）
        yield {"type": "meta", "sources": sources, "query": user_query_text}

        # 5) 第二轮：按 LLM SSE 增量 yield，并用 MIN_DELTA_INTERVAL 做"节奏节流"
        #    即便 Dots/HTTP 一次性把所有 delta 推给我们，这里也会按 ~90字/秒 均匀
        #    地转发给前端，确保人眼能看清逐字打字机效果（不是 160ms 全部刷出）
        full = ""
        last_yield = 0.0
        async for piece in self.llm.chat_complete_stream(messages):
            full += piece
            now = time.monotonic()
            wait = MIN_DELTA_INTERVAL - (now - last_yield)
            if wait > 0:
                await asyncio.sleep(wait)
            yield {"type": "delta", "text": piece}
            last_yield = time.monotonic()
        if not full:
            # 兜底：无内容时给一段提示，同样按节奏分片
            fallback = "已为您检索到相关商品，详情请看下方商品卡片。"
            for i in range(0, len(fallback), 2):
                now = time.monotonic()
                wait = MIN_DELTA_INTERVAL - (now - last_yield)
                if wait > 0:
                    await asyncio.sleep(wait)
                yield {"type": "delta", "text": fallback[i:i + 2]}
                last_yield = time.monotonic()
        yield {"type": "done"}
