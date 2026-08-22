"""验证 RAG 检索效果, 不依赖任何第三方库"""
import urllib.request
import json

QUERY = "考研用平板"
URL = "http://127.0.0.1:8001/api/ai/rag/search"

payload = json.dumps({"query": QUERY, "top_k": 5}, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    URL, data=payload, headers={"Content-Type": "application/json"}, method="POST"
)
with urllib.request.urlopen(req, timeout=30) as resp:
    r = json.loads(resp.read().decode("utf-8"))

print(f"查询: {QUERY}")
print(f"返回 {len(r['items'])} 条:")
for i in r["items"]:
    text = i["text"][:60].replace("\n", " ")
    print(f"  id={i['id']} dist={i['distance']:.4f} {text}")
