from typing import List, Dict, Any
import json
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
                "api-key": settings.llm_api_key,
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

    async def chat_complete(
        self, messages: List[Dict[str, str]], tools: List[Dict[str, Any]] | None = None
    ) -> Dict[str, Any]:
        """阶段二：支持 Function Calling 的完整对话。

        与 chat() 的区别：返回 OpenAI 完整的 message 对象（含 tool_calls），
        由调用方判断是否要执行工具。无 API Key 走演示模式。

        Returns:
            message: {"role": "assistant", "content": ..., "tool_calls": [...]}
        """
        if not self.has_api_key:
            last_user = next((m["content"] for m in reversed(messages) if m["role"] == "user"), "")
            return {"role": "assistant", "content": f"【演示模式】未配置 LLM_API_KEY。原文如下：\n{last_user.strip()}"}

        body: Dict[str, Any] = {
            "model": settings.llm_model,
            "messages": messages,
        }
        if tools:
            body["tools"] = tools
            body["tool_choice"] = "auto"

        resp = await self._client.post(
            "/v1/chat/completions",
            headers={
                "api-key": settings.llm_api_key,
                "Content-Type": "application/json",
            },
            json=body,
        )
        resp.raise_for_status()
        data = resp.json()
        try:
            message = data["choices"][0]["message"]
        except (KeyError, IndexError) as e:
            raise RuntimeError(f"解析 LLM 响应失败: {data}") from e
        return {
            "role": message.get("role", "assistant"),
            "content": message.get("content") or "",
            "tool_calls": message.get("tool_calls") or [],
        }

    async def chat_complete_stream(
        self,
        messages: List[Dict[str, str]],
        tools: List[Dict[str, Any]] | None = None,
    ):
        """流式版 chat_complete：按 SSE 增量 yield 文本片段。

        无 API Key 时直接 yield 演示文本。用于 RAG 第二步生成，实现打字机效果。

        Yields:
            str: 每次的增量文本片段（不含 reasoning_content）
        """
        if not self.has_api_key:
            last_user = next(
                (m["content"] for m in reversed(messages) if m["role"] == "user"), ""
            )
            yield f"【演示模式】未配置 LLM_API_KEY。原文如下：\n{last_user.strip()}"
            return

        body: Dict[str, Any] = {
            "model": settings.llm_model,
            "messages": messages,
            "stream": True,
        }
        if tools:
            body["tools"] = tools
            body["tool_choice"] = "auto"

        async with self._client.stream(
            "POST",
            "/v1/chat/completions",
            headers={
                "api-key": settings.llm_api_key,
                "Content-Type": "application/json",
            },
            json=body,
        ) as resp:
            resp.raise_for_status()
            async for line in resp.aiter_lines():
                if not line or not line.startswith("data:"):
                    continue
                payload = line[len("data:"):].strip()
                if payload == "[DONE]":
                    break
                try:
                    chunk = json.loads(payload)
                except json.JSONDecodeError:
                    continue
                choices = chunk.get("choices") or []
                if not choices:
                    continue
                delta = choices[0].get("delta") or {}
                text = delta.get("content")
                if text:
                    yield text


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
