"""
从 MySQL 查出所有在售商品, 灌进 ChromaDB 向量库
零依赖(只用 Python 标准库), 不需要 pymysql

用法:
    conda activate campus-ai
    cd ai-service
    python scripts/seed_chroma.py

前提:
    1. Python AI 服务已启动 (python -m uvicorn app.main:app --port 8001)
    2. MySQL 在跑, 里面有商品数据
"""
import json
import subprocess
import sys
import urllib.request
import urllib.error

# MySQL 配置(与 application.yml 一致)
MYSQL_CMD = [
    "mysql",
    "-u", "root",
    "-p123456",
    "--default-character-set=utf8",
    "campus_trade",
    "-e",
    "SELECT id, title, description FROM product WHERE status = 'ON_SHELF' ORDER BY id",
    "--batch",   # TSV 输出, 列名一行, 数据每行一条
    "--raw",      # 不转义特殊字符
]

# Python AI 服务地址
AI_SERVICE_URL = "http://127.0.0.1:8001/api/ai/rag/index"


def fetch_products():
    """用 mysql 命令行查商品, 解析 TSV 输出"""
    result = subprocess.run(
        MYSQL_CMD,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if result.returncode != 0:
        print("MySQL 查询失败:")
        print(result.stderr)
        sys.exit(1)

    lines = result.stdout.strip().split("\n")
    if len(lines) < 2:
        return []

    # 第一行是列名(id\ttitle\tdescription), 跳过
    products = []
    for line in lines[1:]:
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        pid, title, desc = parts[0], parts[1], parts[2]
        # text = 标题 + 描述, 给 RAG 检索用
        text = f"{title} {desc}".strip()
        products.append({"id": pid, "text": text})
    return products


def index_products(products):
    """调 Python AI 服务的 /rag/index 接口灌库"""
    payload = json.dumps({"products": products}, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        AI_SERVICE_URL,
        data=payload,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            body = resp.read().decode("utf-8")
            return json.loads(body)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        print(f"HTTP 错误 {e.code}: {body}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"连接失败: {e.reason}")
        print("请确认 Python AI 服务已启动:")
        print("  conda activate campus-ai")
        print("  cd ai-service")
        print("  python -m uvicorn app.main:app --port 8001")
        sys.exit(1)


def main():
    print("[1/3] 从 MySQL 查商品...")
    products = fetch_products()
    print(f"    查到 {len(products)} 件在售商品")

    if not products:
        print("没有商品, 退出")
        return

    print("    预览前3条:")
    for p in products[:3]:
        preview = p["text"][:60].replace("\n", " ")
        print(f"      id={p['id']}: {preview}...")

    print("[2/3] 调 Python AI 服务灌向量库...")
    result = index_products(products)
    print(f"    返回: {result}")

    indexed = result.get("indexed", 0)
    print(f"[3/3] 完成! 向量库新增 {indexed} 条向量")
    print()
    print("现在可以测试检索, 浏览器打开:")
    print("  http://127.0.0.1:8001/docs")
    print("在 /api/ai/rag/search 里试: {\"query\": \"考研用平板\"}")


if __name__ == "__main__":
    main()
