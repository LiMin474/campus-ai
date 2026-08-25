<template>
  <div class="ai-chat">
    <!-- 顶栏：全宽绿色背景 -->
    <div class="chat-header">
      <div class="header-inner">
        <h1 class="header-title">AI 购物助手</h1>
        <p class="header-sub">描述你的需求，我来帮你找合适的校园二手好物</p>
      </div>
    </div>

    <!-- 消息区：可滚动 -->
    <div class="chat-body" ref="messageListRef">
      <div class="message-list">
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="message-row"
          :class="msg.role"
        >
          <!-- 用户气泡：右对齐，绿色填充 -->
          <div v-if="msg.role === 'user'" class="bubble user-bubble">
            {{ msg.content }}
          </div>

          <!-- AI 气泡：左对齐，白色卡片 -->
          <div v-else class="bubble ai-bubble">
            <div
              class="ai-text"
              v-html="renderMarkdown(msg.content, msg.sources)"
              @click="handleInlineCardClick"
            />
            <!-- 来源商品卡：只显示未被内联引用的剩余商品 -->
            <div v-if="remainingSources(msg).length" class="sources">
              <div class="sources-label">来源商品 · 检索自向量库</div>
              <div class="source-grid">
                <div
                  v-for="s in remainingSources(msg)"
                  :key="s.id"
                  class="source-card"
                  @click="goDetail(s.id)"
                >
                  <div class="source-cover" :style="coverStyle(s.coverImage)" />
                  <div class="source-info">
                    <div class="source-title">{{ s.title }}</div>
                    <div v-if="s.price" class="source-price">¥{{ s.price }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载态：AI 思考中 -->
        <div v-if="loading" class="message-row assistant">
          <div class="bubble ai-bubble">
            <span class="thinking">AI 思考中<span class="dots">...</span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区：底部固定 -->
    <div class="chat-input">
      <el-input
        v-model="inputText"
        :disabled="loading"
        placeholder="描述你的需求，比如：我想要考研用的平板..."
        @keyup.enter="send"
      />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
// 组件 name：供 App.vue 的 keep-alive include 匹配，跳详情返回时不销毁聊天记录
defineOptions({ name: "AiChatView" });

import { ref, nextTick } from "vue";
import { useRouter } from "vue-router";
import { http } from "../api/http";

// 来源商品卡片数据结构（第 4 步：调 /products/{id} 补拉真实标题/价格/封面图）
interface SourceItem {
  id: number;
  title: string;
  price?: number;
  condition?: string;
  coverImage?: string;
  text: string;
  distance?: number;
}

// 消息结构：用户 / 助手，助手可带来源商品
interface ChatMessage {
  role: "user" | "assistant";
  content: string;
  sources?: SourceItem[];
}

const router = useRouter();
const inputText = ref("");
const loading = ref(false);
const messageListRef = ref<HTMLElement | null>(null);

// 开场欢迎消息（替代之前的假数据）
const messages = ref<ChatMessage[]>([
  {
    role: "assistant",
    content:
      "你好！我是校园二手 AI 购物助手。\n描述你的需求，比如「我想要考研用的平板，性价比高的二手」，我帮你检索商品库并推荐合适好物。",
    sources: [],
  },
]);

// 封面图样式（无图时绿色占位）
function coverStyle(url?: string) {
  if (!url) return { background: "#e8f5e9" };
  return {
    backgroundImage: `url(${url})`,
    backgroundSize: "cover",
    backgroundPosition: "center",
  };
}

// 点击来源商品卡 → 跳转商品详情页
function goDetail(id: number) {
  router.push(`/products/${id}`);
}

// 轻量 Markdown 渲染：把 AI 回答的 **加粗**、- 列表、# 标题、换行转成 HTML。
// 额外支持 [[商品ID]] 标记：在该行位置内联渲染对应商品卡片。
// 不引入依赖，只覆盖常见语法；转义 HTML 防注入。
function renderMarkdown(text: string, sources?: SourceItem[]): string {
  if (!text) return "";
  const sourceMap = new Map<number, SourceItem>();
  if (sources) {
    for (const s of sources) sourceMap.set(s.id, s);
  }
  const lines = text.split("\n");
  const html: string[] = [];
  let inList = false;

  const escape = (s: string) =>
    s
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  // 行内渲染：支持 **加粗**、`代码`、以及行内 [[id]] 卡片标记（不要求独占一行）
  const inline = (s: string) =>
    escape(s)
      .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
      .replace(/`(.+?)`/g, "<code>$1</code>")
      .replace(/\[\[(\d+)\]\]/g, (m, rawId: string) => {
        const sc = sourceMap.get(Number(rawId));
        return sc ? renderInlineCard(sc) : m;
      });

  for (const raw of lines) {
    const line = raw.trimEnd();

    // 检查是否是 [[id]] 标记行
    const idMatch = line.match(/^\s*\[\[(\d+)\]\]\s*$/);
    if (idMatch) {
      if (inList) {
        html.push("</ul>");
        inList = false;
      }
      const id = Number(idMatch[1]);
      const s = sourceMap.get(id);
      if (s) {
        html.push(renderInlineCard(s));
      }
      continue;
    }

    const listMatch = line.match(/^[-*]\s+(.*)$/);
    const headMatch = line.match(/^#{1,3}\s+(.*)$/);

    if (listMatch) {
      if (!inList) {
        html.push('<ul class="md-list">');
        inList = true;
      }
      html.push(`<li>${inline(listMatch[1])}</li>`);
    } else {
      if (inList) {
        html.push("</ul>");
        inList = false;
      }
      if (headMatch) {
        html.push(`<div class="md-head">${inline(headMatch[1])}</div>`);
      } else if (line.trim() === "") {
        html.push("<div class='md-blank'></div>");
      } else {
        html.push(`<p>${inline(line)}</p>`);
      }
    }
  }
  if (inList) html.push("</ul>");
  return html.join("");
}

// 内联商品卡片 HTML（嵌在文字描述对应位置）
function renderInlineCard(s: SourceItem): string {
  const coverBg = s.coverImage
    ? `background-image:url(${s.coverImage});background-size:cover;background-position:center;`
    : "background-color:#e8f5e9;";
  const title = (s.title || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  const price = s.price != null ? `¥${s.price}` : "—";
  return `<div class="inline-card" data-id="${s.id}">
    <div class="inline-cover" style="${coverBg}"></div>
    <div class="inline-info">
      <div class="inline-title">${title}</div>
      <div class="inline-price">${price}</div>
    </div>
  </div>`;
}

// 事件委托：点击内联卡片跳详情
function handleInlineCardClick(e: MouseEvent) {
  const target = (e.target as HTMLElement).closest("[data-id]");
  if (target) {
    const id = Number(target.getAttribute("data-id"));
    if (id) goDetail(id);
  }
}

// 底部剩余商品：只显示未被 [[id]] 内联引用的
function remainingSources(msg: ChatMessage): SourceItem[] {
  if (!msg.sources) return [];
  return msg.sources.filter((s) => !msg.content.includes(`[[${s.id}]]`));
}

// 用 sources 里的 id 调 /products/{id} 补拉真实标题/价格/封面图
async function enrichSources(sources: SourceItem[]) {
  return Promise.all(
    sources.map(async (s) => {
      try {
        const res = await http.get(`/products/${s.id}`);
        const d = res.data?.data;
        if (d) {
          s.title = d.title || s.title;
          s.price = d.price != null ? Number(d.price) : undefined;
          s.condition = d.conditionLabel || s.condition;
          s.coverImage = d.imageUrls?.length ? d.imageUrls[0] : undefined;
        }
      } catch {
        // 补拉失败保留 text 截断的标题，不阻断整个回答
      }
      return s;
    })
  );
}

// 滚动到底部
async function scrollToBottom() {
  await nextTick();
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
  }
}

// 发送：调 /api/ai/rag/chat/stream 接口（SSE 流式，打字机效果）
async function send() {
  const text = inputText.value.trim();
  if (!text || loading.value) return;

  // 1. 先把用户消息加进去
  messages.value.push({ role: "user", content: text });
  inputText.value = "";
  loading.value = true;
  scrollToBottom();

  // 2. 创建占位的 assistant 消息，流式内容往里追加
  //    关键：push 后用 asstIdx 定位，后续所有更新都通过"整体替换引用"的方式
  //         messages.value[asstIdx] = { ...prev, content/sources }
  //    Vue 3 对 ref 数组里嵌套对象的增量赋值（obj.content += x）
  //    在 async+微任务密集时存在漏触发视图更新的可能，
  //    整体替换引用 100% 强制重渲染
  messages.value.push({
    role: "assistant",
    content: "",
    sources: [] as SourceItem[],
  });
  const asstIdx = messages.value.length - 1;
  const patchAsst = (delta: Partial<{ role: "assistant"; content: string; sources: SourceItem[] }>) => {
    const prev = messages.value[asstIdx];
    messages.value[asstIdx] = {
      role: "assistant",
      content: delta.content != null ? delta.content : prev.content,
      sources: delta.sources != null ? delta.sources : prev.sources,
    };
  };

  // SSE 事件处理：异步，每 2 个 delta 让出一个 macrotask 配合 nextTick，
  // 避免 Vue 在同一事件循环内合并多次 DOM 更新导致"视觉上非流式"。
  let sources: SourceItem[] = [];
  let evtCount = 0;
  const handleEvent = async (ev: any) => {
    if (!ev || !ev.type) return;
    if (ev.type === "meta") {
      sources = (ev.sources || []).map((s: any) => ({
        id: Number(s.id),
        title:
          (s.text || "").slice(0, 22) + ((s.text || "").length > 22 ? "..." : ""),
        text: s.text || "",
        distance: s.distance,
        price: s.price != null ? Number(s.price) : undefined,
        condition: s.condition || undefined,
      }));
      patchAsst({ sources });
      // 补拉完成后整体替换 sources 引用 → 强制触发 Vue 视图更新
      enrichSources(sources).then((enriched) => {
        patchAsst({ sources: [...enriched] });
      });
    } else if (ev.type === "delta") {
      // 整体替换引用改 content，100% 触发 Vue 重渲
      patchAsst({ content: messages.value[asstIdx].content + (ev.text || "") });
      evtCount += 1;
      if (evtCount % 2 === 0) {
        await new Promise<void>((r) => setTimeout(r, 0));
      }
      await nextTick();
    } else if (ev.type === "error") {
      patchAsst({ content: `抱歉，AI 助手暂时不可用：${ev.message || "服务异常"}` });
    }
    scrollToBottom();
  };

  // 按 SSE 分隔符 \n\n 切分事件（await handleEvent 异步消费）
  const consumeBuffer = async (buffer: string) => {
    let leftover = buffer;
    let idx = leftover.indexOf("\n\n");
    while (idx !== -1) {
      const rawEvent = leftover.slice(0, idx);
      leftover = leftover.slice(idx + 2);
      const dataLine = rawEvent
        .split("\n")
        .find((l: string) => l.startsWith("data:"));
      if (dataLine) {
        const payload = dataLine.slice(5).trim();
        if (payload) {
          try {
            await handleEvent(JSON.parse(payload));
          } catch {
            // 忽略无法解析的行
          }
        }
      }
      idx = leftover.indexOf("\n\n");
    }
    return leftover;
  };

  try {
    const token = localStorage.getItem("token") || "";
    const resp = await fetch("/api/ai/rag/chat/stream", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({
        query: text,         //当前 用户提问内容
        // 多轮对话：把之前的对话历史发给后端，让 AI 有记忆
        // 排除刚 push 进去的用户消息 + 空 assistant 占位（最后两条）
        history: messages.value
          .slice(0, -2)     //去掉最后两条 此轮为还没有完成的最新一轮
          .filter((m) => m.content.trim()) //过滤空消息
          .map((m) => ({
            role: m.role,
            content: m.content,
            // 工具结果记忆：assistant 消息附带上轮检索到的商品列表
            // 让 LLM 能精确回答"第一个多少钱""那个成色怎样"等指代
            // 注意：只发结构化字段，不发封面图 URL（LLM 看不了图，白占 token）
            ...(m.role === "assistant" && m.sources?.length
              ? {
                  sources: m.sources.map((s) => ({
                    id: s.id,
                    title: s.title,
                    price: s.price ?? null,
                    condition: s.condition ?? null,
                    text: s.text ?? "",
                  })),
                }
              : {}),
          }))
          .slice(-10), // 最多发 10 条（5 轮），防 Token 爆   //只取最后10条
      }),
    });
    if (!resp.ok || !resp.body) {
      throw new Error(`HTTP ${resp.status}`);
    }

    const reader = resp.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      buffer = await consumeBuffer(buffer);
    }
    if (buffer.trim()) {
      await consumeBuffer(buffer + "\n\n");
    }

    if (!messages.value[asstIdx].content.trim()) {
      patchAsst({ content: "（AI 没有返回回答）" });
    }
  } catch (e: any) {
    const msg = e.message || "网络异常";
    patchAsst({
      content: `抱歉，AI 助手暂时不可用：${msg}。请确认 Java 后端（8080）与 Python AI 服务（127.0.0.1:8001）是否在运行。`,
    });
  } finally {
    loading.value = false;
    scrollToBottom();
  }
}
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px); /* 减去顶部导航 60px 高度 */
  max-width: 900px;
  margin: 0 auto;
  background: #fafafa;
}

/* 顶栏：全宽绿色 */
.chat-header {
  background-color: #0f9d58;
  color: #fff;
  padding: 20px 16px;
}
.header-inner {
  max-width: 900px;
  margin: 0 auto;
}
.header-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}
.header-sub {
  font-size: 13px;
  opacity: 0.9;
  margin: 6px 0 0;
}

/* 消息区：flex 撑开 + 滚动 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
}
.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.message-row {
  display: flex;
}
.message-row.user {
  justify-content: flex-end;
}
.message-row.assistant {
  justify-content: flex-start;
}

/* 气泡 */
.bubble {
  max-width: 75%;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap; /* 保留 \n 换行 */
}
.user-bubble {
  background-color: #0f9d58;
  color: #fff;
  border-radius: 14px 14px 4px 14px;
}
.ai-bubble {
  background-color: #fff;
  border: 1px solid #ebeef5;
  color: #1a1a2e;
  border-radius: 4px 14px 14px 14px;
}
.ai-text {
  margin-bottom: 8px;
}
.ai-text :deep(p) {
  margin: 4px 0;
}
.ai-text :deep(.md-list) {
  margin: 4px 0;
  padding-left: 18px;
}
.ai-text :deep(.md-list li) {
  margin: 2px 0;
}
.ai-text :deep(.md-head) {
  font-weight: 700;
  font-size: 15px;
  margin: 8px 0 4px;
}
.ai-text :deep(.md-blank) {
  height: 8px;
}
.ai-text :deep(strong) {
  font-weight: 700;
  color: #0f9d58;
}
.ai-text :deep(code) {
  background: #f0f2f5;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12px;
  color: #c7254e;
}

/* 加载态：AI 思考中 */
.thinking {
  color: #909399;
  font-size: 13px;
}
.dots {
  display: inline-block;
  animation: blink 1.2s infinite;
}
@keyframes blink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

/* 内联商品卡片（嵌在文字描述对应位置，通过 v-html 渲染需用 :deep） */
.ai-text :deep(.inline-card) {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  margin: 6px 0 10px;
  transition: box-shadow 0.2s;
}
.ai-text :deep(.inline-card:hover) {
  box-shadow: 0 2px 8px rgba(15, 157, 88, 0.15);
}
.ai-text :deep(.inline-cover) {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  background-color: #e8f5e9;
}
.ai-text :deep(.inline-info) {
  padding: 6px 10px;
  flex: 1;
  min-width: 0;
}
.ai-text :deep(.inline-title) {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ai-text :deep(.inline-price) {
  font-size: 14px;
  font-weight: 700;
  color: #0f9d58;
  margin-top: 2px;
}

/* 来源商品卡 */
.sources {
  margin-top: 12px;
  border-top: 1px dashed #ebeef5;
  padding-top: 10px;
}
.sources-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.source-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.source-card {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.source-card:hover {
  box-shadow: 0 2px 8px rgba(15, 157, 88, 0.15);
}
.source-cover {
  height: 80px;
  background-color: #e8f5e9;
}
.source-info {
  padding: 8px 10px;
}
.source-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.source-price {
  font-size: 14px;
  font-weight: 700;
  color: #0f9d58;
}

/* 输入区 */
.chat-input {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  background-color: #fff;
  border-top: 1px solid #ebeef5;
}
.chat-input :deep(.el-input__wrapper) {
  border-radius: 8px;
}
:deep(.el-button--primary) {
  background-color: #0f9d58 !important;
  border-color: #0f9d58 !important;
}
:deep(.el-button--primary:hover) {
  background-color: #0bb767 !important;
  border-color: #0bb767 !important;
}
</style>
