from typing import List, Dict, Any
import httpx
from app.config import settings


class LLMService:
    """OpenAI 兼容聊天模型封装。复刻 Java AiService 的行为，未配置 key 时走演示模式。"""

    def __init__(self):
        self._client = httpx.AsyncClient(
            base_url=settings.llm_base_url.rstrip("/"),
            timeout=settings.llm_timeout,
        )

    async def close(self):
        await self._client.aclose()

    @property
    def has_api_key(self) -> bool:
        return bool(settings.llm_api_key)

    async def chat(self, messages: List[Dict[str, str]], **kwargs) -> str:
        """发送聊天请求，返回助手文本内容。无 API Key 走演示模式。"""
        if not self.has_api_key:
            last_user = next((m["content"] for m in reversed(messages) if m["role"] == "user"), "")
            return f"【演示模式】未配置 LLM_API_KEY。原文如下：\n{last_user.strip()}"

        body: Dict[str, Any] = {
            "model": settings.llm_model,
            "messages": messages,
        }
        body.update(kwargs)

        resp = await self._client.post(
            "/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.llm_api_key}",
                "Content-Type": "application/json",
            },
            json=body,
        )
        resp.raise_for_status()
        data = resp.json()
        try:
            return data["choices"][0]["message"]["content"]
        except (KeyError, IndexError) as e:
            raise RuntimeError(f"解析 LLM 响应失败: {data}") from e


POLISH_SYSTEM_PROMPT = (
    "你是校园二手交易平台的文案助手，请把用户提供的商品描述润色得更清晰、真诚、有吸引力，"
    "不要编造不存在的参数。"
)


async def polish_description(llm: LLMService, text: str) -> str:
    if not text or not text.strip():
        raise ValueError("描述不能为空")
    messages = [
        {"role": "system", "content": POLISH_SYSTEM_PROMPT},
        {"role": "user", "content": f"请润色以下商品描述：\n{text.strip()}"},
    ]
    return await llm.chat(messages)
