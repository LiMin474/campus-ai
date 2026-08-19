import chromadb
import numpy as np
import onnxruntime as ort
from pathlib import Path
from tokenizers import Tokenizer

# ============================================================
# 1. 自封装 BGE-small-zh-v1.5 ONNX 推理（不依赖 sentence-transformers / optimum）
#    规范：CLS pooling + L2 normalize（BGE 官方推荐）
# ============================================================
MODEL_DIR = Path(__file__).resolve().parent.parent.parent / "models" / "bge-small-zh-v1.5"
print(f"加载模型: {MODEL_DIR}")

# 1.1 tokenizer（HuggingFace tokenizers 库，读 tokenizer.json）
tokenizer = Tokenizer.from_file(str(MODEL_DIR / "tokenizer.json"))

# 1.2 ONNX 会话（CPU 即可，bge-small 很小）
sess_options = ort.SessionOptions()
sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
session = ort.InferenceSession(
    str(MODEL_DIR / "onnx" / "model.onnx"),
    sess_options=sess_options,
    providers=["CPUExecutionProvider"],
)
# 拿输入名（一般是 input_ids / attention_mask / token_type_ids）
input_names = [i.name for i in session.get_inputs()]


def encode(texts, max_length=512):
    """文本 -> 向量（CLS pooling + L2 normalize）"""
    if isinstance(texts, str):
        texts = [texts]

    vectors = []
    for text in texts:
        # 1) tokenize：BGE 用 BERT tokenizer，要加 [CLS]/[SEP]
        enc = tokenizer.encode(text)
        input_ids = enc.ids[:max_length]
        attention_mask = enc.attention_mask[:max_length]

        # 2) 构造 onnx 输入 dict（按模型实际输入名给）
        feeds = {
            "input_ids": np.array([input_ids], dtype=np.int64),
            "attention_mask": np.array([attention_mask], dtype=np.int64),
        }
        # 如果模型需要 token_type_ids，补一个全 0
        if "token_type_ids" in input_names:
            feeds["token_type_ids"] = np.zeros_like(feeds["input_ids"])

        # 3) onnx 推理 -> last_hidden_state [1, seq_len, 512]
        outputs = session.run(None, feeds)
        last_hidden_state = outputs[0]

        # 4) CLS pooling：取第 0 个 token（[CLS]）的向量
        cls_vec = last_hidden_state[0, 0, :]

        # 5) L2 normalize
        norm = np.linalg.norm(cls_vec)
        cls_vec = cls_vec / norm if norm > 0 else cls_vec

        vectors.append(cls_vec.tolist())

    return vectors


# ============================================================
# 2. 假商品数据
# ============================================================
goods = [
    {"id": "1", "text": "iPad Air 2022 九成新 考研网课配套 平板电脑 32G 白色"},
    {"id": "2", "text": "考研数学李永乐全套复习资料 纸质书 几乎全新"},
    {"id": "3", "text": "捷安特山地自行车 26寸 八成新 变速 校区自提"},
]

# ============================================================
# 3. 连 Chroma，新建 BGE collection（512 维，与旧 products 分开）
# ============================================================
client = chromadb.PersistentClient(path="./data/chroma")
collection = client.get_or_create_collection("products_bge")

# ============================================================
# 4. 灌库（显式传 BGE 向量；upsert 方便重复运行）
# ============================================================
ids = [g["id"] for g in goods]
documents = [g["text"] for g in goods]
embeddings = encode(documents)
collection.upsert(ids=ids, documents=documents, embeddings=embeddings)

# ============================================================
# 5. 查询
# ============================================================
query_emb = encode(["考研用平板"])
result = collection.query(query_embeddings=query_emb, n_results=2)

# ============================================================
# 6. 打印
# ============================================================
print("\n查询: 考研用平板\n")
for text, dist in zip(result["documents"][0], result["distances"][0]):
    print(f"{text}  ——  距离 {dist:.4f}")
