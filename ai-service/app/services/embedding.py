"""BGE-small-zh-v1.5 ONNX 本地向量封装。

只负责"文本 → 向量"，不碰 ChromaDB / LLM。
规范：CLS pooling + L2 normalize（BGE 官方推荐）。
"""
from pathlib import Path

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer


class BgeEmbedding:
    """加载本地 BGE-small-zh-v1.5 ONNX 模型，把文本编码成 512 维单位向量。"""

    "构造方法 创建对象时自动执行 引入 模型bge-small-zh-v1.5"
    def __init__(self, model_dir: str | Path):
        model_dir = Path(model_dir)
        if not model_dir.exists():
            raise FileNotFoundError(f"模型目录不存在: {model_dir}")

        # tokenizer（HuggingFace tokenizers 库，读 tokenizer.json）
        self.tokenizer = Tokenizer.from_file(str(model_dir / "tokenizer.json"))

        # ONNX 会话（CPU 即可，bge-small 很小）
        sess_options = ort.SessionOptions()
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        self.session = ort.InferenceSession(
            str(model_dir / "onnx" / "model.onnx"),
            sess_options=sess_options,
            providers=["CPUExecutionProvider"],
        )
        # 模型实际输入名（一般含 input_ids / attention_mask，可能有 token_type_ids）
        self._input_names = [i.name for i in self.session.get_inputs()]
        self._dim = self.session.get_outputs()[0].shape[-1]

    @property
    def dim(self) -> int:
        """向量维度（BGE-small = 512）。"""
        return self._dim

    def encode(self, texts, max_length: int = 512) -> list[list[float]]:
        """文本 → 向量（CLS pooling + L2 normalize）。

        Args:
            texts: 单条 str 或 str 列表
            max_length: token 最大长度

        Returns:
            list[list[float]]，每条文本一个 512 维单位向量
        """
        "若只传入了一个字符串 则自动包装为列表"
        if isinstance(texts, str):
            texts = [texts]

        vectors = []
        for text in texts:
            # 1) tokenize：BGE 用 BERT tokenizer，会自动加 [CLS]/[SEP]
            enc = self.tokenizer.encode(text)
            input_ids = enc.ids[:max_length]
            attention_mask = enc.attention_mask[:max_length]

            # 2) 构造 onnx 输入 dict（按模型实际输入名给）
            feeds = {
                "input_ids": np.array([input_ids], dtype=np.int64),
                "attention_mask": np.array([attention_mask], dtype=np.int64),
            }
            # 如果模型需要 token_type_ids，补一个全 0
            if "token_type_ids" in self._input_names:
                feeds["token_type_ids"] = np.zeros_like(feeds["input_ids"])

            # 3) onnx 推理 -> last_hidden_state [1, seq_len, dim]
            outputs = self.session.run(None, feeds)
            last_hidden_state = outputs[0]

            # 4) CLS pooling：取第 0 个 token（[CLS]）的向量
            cls_vec = last_hidden_state[0, 0, :]

            # 5) L2 normalize
            norm = np.linalg.norm(cls_vec)
            cls_vec = cls_vec / norm if norm > 0 else cls_vec

            vectors.append(cls_vec.tolist())

        return vectors
