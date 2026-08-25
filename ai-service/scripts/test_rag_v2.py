# -*- coding: utf-8 -*-
"""临时测试脚本：阶段二 9 参数 + RRF + 4 步输出 验证。

直接 import rag.py 跑。测完可删。
"""
import asyncio
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]  # ai-service 目录
sys.path.insert(0, str(ROOT))

from app.services.rag import RagService, condition_to_num
from app.services.embedding import BgeEmbedding
from app.services.llm import LLMService

CHROMA = str(ROOT / "data" / "chroma")
MODEL = str(ROOT.parents[0] / "models" / "bge-small-zh-v1.5")

embedding = BgeEmbedding(model_dir=MODEL)
llm = LLMService()
rag = RagService(embedding=embedding, llm=llm, chroma_path=CHROMA, top_k=5)


def print_sources(title, sources):
    print(f"\n=== {title}  ({len(sources)} items) ===")
    for s in sources:
        p = s.get("price")
        c = s.get("condition")
        print(f"  id={s['id']:>3}  price={str(p):>8}  cond={str(c):>8}  dist={s['distance']:.3f}  | {(s['text'] or '')[:40]}")


# ------------------------------------------------------------------
# 第一组：直接测 search_products（不含 LLM），验证过滤/RRF 逻辑
# ------------------------------------------------------------------
print("\n" + "=" * 70)
print("A 组：search_products 纯函数测试（过滤 + RRF）")
print("=" * 70)

# A1：平板，3000以内 + 九成新以上（基线）
r = rag.search_products(query="平板", max_price=3000, min_condition="九成新", top_k=10)
print_sources("A1: 平板 max_price=3000 min_cond=九成新  =>  全<=3000 & >=九成新？", r["results"])
assert all((s["price"] is None or float(s["price"]) <= 3000) for s in r["results"]), "A1 价格上限过滤失败"
assert all((s["condition"] is None or condition_to_num(s["condition"]) >= condition_to_num("九成新")) for s in r["results"]), "A1 成色下限过滤失败"
print("  ✅ A1 过滤正确")

# A2：平板，不要低于 3000（价格下限）
r = rag.search_products(query="平板", min_price=3000, top_k=10)
print_sources("A2: 平板 min_price=3000  =>  全>=3000？", r["results"])
for s in r["results"]:
    if s["price"] is not None:
        assert float(s["price"]) >= 3000, f"A2 价格下限失败 id={s['id']} price={s['price']}"
print(f"  ✅ A2 过滤正确（{len(r['results'])} 条，可能为0说明库中暂无平板>=3000，属正常）")

# A3：价格区间 10~50
r = rag.search_products(query="考研资料", min_price=10, max_price=50, top_k=10)
print_sources("A3: 考研资料 [10,50] 区间", r["results"])
for s in r["results"]:
    if s["price"] is not None:
        assert 10 <= float(s["price"]) <= 50, f"A3 区间失败 id={s['id']} price={s['price']}"
print("  ✅ A3 区间过滤正确")

# A4：成色上限 <=八成新
r = rag.search_products(query="自行车", max_condition="八成新", top_k=10)
print_sources("A4: 自行车 cond<=八成新", r["results"])
for s in r["results"]:
    if s["condition"] is not None:
        assert condition_to_num(s["condition"]) <= condition_to_num("八成新"), f"A4 成色上限失败 id={s['id']} cond={s['condition']}"
print(f"  ✅ A4 成色上限正确（{len(r['results'])} 条）")

# A5：排除词 "滑板 不要长板"
r = rag.search_products(query="滑板", keywords_exclude=["长板"], top_k=10)
print_sources("A5: 滑板 排除'长板'", r["results"])
for s in r["results"]:
    assert "长板" not in (s["text"] or ""), f"A5 排除词失败 id={s['id']} text含长板"
print(f"  ✅ A5 排除词正确（{len(r['results'])} 条）")

# A6：RRF 排序：平板 + 价格越低越好（sort_by=price_asc）
r_plain = rag.search_products(query="平板", top_k=10)
r_sorted = rag.search_products(query="平板", sort_by="price_asc", sort_weight=0.7, top_k=10)
print_sources("A6_ref: 平板 默认相关性排序", r_plain["results"])
print_sources("A6: 平板 sort_by=price_asc sw=0.7", r_sorted["results"])
# 只要求：sort_by 后价格最低的商品排名比默认排名更靠前（体现 RRF 生效）
def find_pos(results, target_id):
    for i, s in enumerate(results):
        if str(s["id"]) == str(target_id):
            return i
    return 999
if r_sorted["results"] and r_plain["results"]:
    # 找到最便宜的商品
    cheapest_id = min(r_sorted["results"], key=lambda s: s["price"] if isinstance(s["price"], (int, float)) else 1e9)["id"]
    pos_plain = find_pos(r_plain["results"], cheapest_id)
    pos_sorted = find_pos(r_sorted["results"], cheapest_id)
    if pos_sorted < pos_plain:
        print(f"  ✅ A6 RRF 生效：最便宜商品 id={cheapest_id} 在默认排名 {pos_plain+1} → 排序偏好排名 {pos_sorted+1}，更靠前了")
    elif cheapest_id == (r_sorted["results"][0]["id"] if r_sorted["results"] else None):
        print("  ✅ A6 RRF 生效：最便宜商品已在第一位")
    else:
        print(f"  ⚠️ A6 RRF 可能未体现差异：默认排名 {pos_plain+1} vs 排序后排名 {pos_sorted+1}，可能候选太少或相关性太强")

# ------------------------------------------------------------------
# 第二组：chat() 端到端（含 LLM），验证参数理解 + 4 步输出
# 每条可能花 5~20 秒 LLM 推理
# ------------------------------------------------------------------
print("\n" + "=" * 70)
print("B 组：chat 端到端（LLM Function Calling + 4 步输出）")
print("=" * 70)

cases = [
    ("B1", "平板，不要低于 3000 元的，我要高端点的",          "含 min_price 参数？不应含 max_price"),
    ("B2", "滑板，不要长板那种",                               "含 keywords_exclude=['长板']？"),
    ("B3", "平板，越新越好",                                    "sort_by=condition_asc？"),
    ("B4", "耳机，便宜点就行",                                  "sort_by=price_asc？不应出现瞎编的 max_price"),
    ("B5", "我要个平板 3000以内 九成新以上",                    "正常三参数"),
    ("B6", "给我推荐点好东西吧随便看看",                        "不调工具，直接返回澄清话术"),
]

async def main():
    for code, query, check_point in cases:
        print(f"\n--- {code}: {query!r}   ({check_point}) ---")
        try:
            resp = await rag.chat(query, top_k=5)
        except Exception as e:
            print(f"  ❌ EXCEPTION: {type(e).__name__}: {e}")
            continue
        ans = resp["answer"] or ""
        # 截断输出
        preview = ans[:600] + ("…[截断]" if len(ans) > 600 else "")
        print(f"  answer 预览:\n    {preview.replace(chr(10), chr(10)+'    ')}")
        src = resp["sources"]
        print(f"  sources 数量: {len(src)}")
        if src:
            print("    sources: " + ", ".join(f"id={s['id']}¥{s['price']}{s['condition']}" for s in src[:5]))
        # 4 步结构快速检查
        steps_present = all(tok in ans for tok in ["步骤 1", "步骤 2", "步骤 3", "步骤 4"]) or \
                        all(tok in ans for tok in ["需求重述", "匹配商品", "差异告知", "下一步建议"])
        print(f"  4 步结构：{'✅ 存在' if steps_present else '⚠️ 未检测到4步标题（不一定错，内容可能用了同义表达）'}")


try:
    asyncio.run(main())
except Exception as e:
    print(f"B 组运行失败：{type(e).__name__}: {e}")

print("\n全部测试结束。")
