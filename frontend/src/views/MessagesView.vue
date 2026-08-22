<!--
消息聊天视图
负责成员C：聊天系统
-->
<template>
  <el-row :gutter="16" class="wrap">
    <el-col :span="9">
      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="hdr">
            <span>消息</span>
            <el-badge v-if="unreadTotal" :value="unreadTotal" class="badge" />
            <el-tag v-if="!stompClient || !stompClient.connected" size="small" type="danger">
              未连接
            </el-tag>
            <el-tag v-else size="small" type="success">
              已连接
            </el-tag>
          </div>
        </template>
        <div v-if="!conversations.length" class="empty">暂无会话</div>
        <div
          v-for="c in conversations"
          :key="c.id"
          class="conv"
          :class="{ active: activeId === c.id }"
          @click="select(c.id)"
        >
          <el-avatar :size="40" :src="c.peerAvatarUrl || undefined">{{ (c.peerNickname || "?").slice(0, 1) }}</el-avatar>
          <div class="mid">
            <div class="row1">
              <span class="name">{{ c.peerNickname }}</span>
              <span class="time">{{ formatTime(c.lastMessageAt) }}</span>
            </div>
            <div class="row2">
              <span class="preview">{{ c.lastMessagePreview || "开始聊天吧" }}</span>
              <el-badge v-if="c.unreadCount" :value="c.unreadCount" class="mini" />
            </div>
            <div v-if="c.contextTitle" class="ctx">
              <el-image v-if="c.contextCoverUrl" :src="c.contextCoverUrl" class="thumb" fit="cover" />
              <span class="ctx-title">{{ c.contextTitle }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="15">
      <el-card shadow="never" class="chat-card">
        <template #header>
          <div v-if="activePeer" class="chat-hdr">
            <div>
              <div class="t">{{ activePeer.nickname }}</div>
              <div class="s" v-if="activeContextTitle">{{ activeContextTitle }}</div>
            </div>
          </div>
          <div v-else class="chat-hdr muted">请选择左侧会话</div>
        </template>
        <div ref="scrollRef" class="msgs">
          <div v-for="m in messages" :key="m.id" class="msg" :class="{ mine: m.mine }">
            <div class="bubble">
              <div class="who">{{ m.mine ? "我" : m.senderNickname }}</div>
              <div class="txt">{{ m.content }}</div>
              <div class="ts">{{ formatTime(m.createdAt) }}</div>
            </div>
          </div>
        </div>
        <div class="composer">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="2"
            maxlength="2000"
            show-word-limit
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="send"
          />
          <el-button type="primary" :disabled="!activeId || sending || !stompClient?.connected" @click="send">发送</el-button>
        </div>
      </el-card>
    </el-col>
  </el-row>
  <el-row class="tips">
    <el-col :span="24">
      <el-alert type="info" show-icon title="安全提示" description="勿脱离平台私下交易；谨防诈骗与虚假链接。" />
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { http, type ApiResponse } from "../api/http";
import { useAuthStore } from "../stores/auth";
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

type StompSubscription = { id: string; unsubscribe: () => void };

type Conv = {
  id: number;
  peerUserId: number;
  peerNickname: string;
  peerAvatarUrl?: string | null;
  contextType: string;
  contextId: number;
  contextTitle?: string | null;
  contextCoverUrl?: string | null;
  lastMessagePreview?: string | null;
  lastMessageAt?: string | null;
  unreadCount: number;
};

type Msg = {
  id: number;
  senderId: number;
  senderNickname: string;
  content: string;
  createdAt: string;
  mine: boolean;
};

const route = useRoute();
const auth = useAuthStore();
const conversations = ref<Conv[]>([]);
const messages = ref<Msg[]>([]);
const activeId = ref<number | null>(null);
const draft = ref("");
const sending = ref(false);
const scrollRef = ref<HTMLElement | null>(null);

const stompClient = ref<Stomp.Client | null>(null);
let pollList: ReturnType<typeof setInterval> | null = null;
let messageSubscription: StompSubscription | null = null;
let readSubscription: StompSubscription | null = null;

const unreadTotal = computed(() => conversations.value.reduce((a, c) => a + (c.unreadCount || 0), 0));

const activePeer = computed(() => {
  const c = conversations.value.find((x) => x.id === activeId.value);
  if (!c) return null;
  return { nickname: c.peerNickname };
});

const activeContextTitle = computed(() => {
  const c = conversations.value.find((x) => x.id === activeId.value);
  return c?.contextTitle || "";
});

function formatTime(iso?: string | null) {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }
  return d.toLocaleDateString();
}

async function loadList() {
  try {
    const { data } = await http.get<ApiResponse<Conv[]>>("/chat/conversations");
    if (data.code !== 200) return;
    conversations.value = data.data;
  } catch (error) {
    console.error('Failed to load conversations:', error);
  }
}

async function loadMessages() {
  if (!activeId.value) return;
  try {
    const { data } = await http.get<ApiResponse<Msg[]>>(`/chat/conversations/${activeId.value}/messages/latest`);
    if (data.code !== 200) return;
    messages.value = data.data;
    await nextTick();
    scrollBottom();
  } catch (error) {
    console.error('Failed to load messages:', error);
  }
}

async function markRead() {
  if (!activeId.value) return;
  try {
    await http.post(`/chat/conversations/${activeId.value}/read`);
    await loadList();
  } catch (error) {
    console.error('Failed to mark messages as read:', error);
  }
}

function scrollBottom() {
  const el = scrollRef.value;
  if (!el) return;
  el.scrollTop = el.scrollHeight;
}

async function select(id: number) {
  activeId.value = id;
  await loadMessages();
  await markRead();
  // 订阅消息
  subscribeToMessages(id);
}

function subscribeToMessages(conversationId: number) {
  if (!stompClient.value || !stompClient.value.connected) return;
  
  unsubscribeFromMessages();
  
  messageSubscription = stompClient.value.subscribe(`/queue/chat/conversations/${conversationId}`, (message) => {
    try {
      const msg: Msg = JSON.parse(message.body);
      msg.mine = msg.senderId === auth.user?.id;
      messages.value.push(msg);
      nextTick(() => scrollBottom());
    } catch (error) {
      console.error('Failed to parse message:', error);
    }
  });
  
  readSubscription = stompClient.value.subscribe(`/queue/chat/conversations/${conversationId}/read`, (message) => {
    try {
      const data = JSON.parse(message.body);
      loadList();
    } catch (error) {
      console.error('Failed to parse read notification:', error);
    }
  });
}

function unsubscribeFromMessages() {
  if (messageSubscription) {
    messageSubscription.unsubscribe();
    messageSubscription = null;
  }
  if (readSubscription) {
    readSubscription.unsubscribe();
    readSubscription = null;
  }
}

async function send() {
  if (!activeId.value || !stompClient.value || !stompClient.value.connected) return;
  const text = draft.value.trim();
  if (!text) return;
  sending.value = true;
  try {
    const headers = auth.token ? { Authorization: `Bearer ${auth.token}` } : {};
    stompClient.value.send(`/app/chat/conversations/${activeId.value}/messages`, headers, JSON.stringify({ content: text }));
    draft.value = "";
  } catch (e: any) {
    ElMessage.error(e?.message || "发送失败");
  } finally {
    sending.value = false;
  }
}

function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
  const host = window.location.host;
  const wsUrl = import.meta.env.DEV ? '/ws' : `${protocol}//${host}/ws`;
  const socket = new SockJS(wsUrl);
  stompClient.value = Stomp.over(socket);
  
  const headers = auth.token ? { Authorization: `Bearer ${auth.token}` } : {};
  console.log('WebSocket connecting with token:', auth.token ? 'present' : 'missing');
  console.log('Headers:', headers);
  console.log('WebSocket URL:', wsUrl);
  stompClient.value.connect(headers, () => {
    console.log('WebSocket connected');
    loadList();
    if (activeId.value) {
      subscribeToMessages(activeId.value);
    }
  }, (error) => {
    console.error('WebSocket connection error:', error);
    setTimeout(connectWebSocket, 5000);
  });
}

onMounted(async () => {
  await loadList();
  const q = Number(route.query.c as string);
  if (q) {
    activeId.value = q;
    await loadMessages();
    await markRead();
  }
  // 启动 WebSocket 连接
  connectWebSocket();
  // 保留轮询作为备份
  pollList = setInterval(loadList, 30000);
});

watch(
  () => route.query.c,
  async (c) => {
    const id = Number(c as string);
    if (id) {
      activeId.value = id;
      await loadMessages();
      await markRead();
      subscribeToMessages(id);
    }
  }
);

watch(activeId, (id) => {
  if (id) {
    subscribeToMessages(id);
  }
});

onBeforeUnmount(() => {
  if (pollList) clearInterval(pollList);
  if (stompClient.value) {
    stompClient.value.disconnect(() => {});
  }
});
</script>

<style scoped>
.wrap {
  align-items: stretch;
}
.list-card,
.chat-card {
  min-height: 520px;
}
.hdr {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.badge :deep(.el-badge__content) {
  border: none;
}
.empty {
  color: #909399;
  padding: 12px 0;
}
.conv {
  display: flex;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
}
.conv:hover {
  background: #f5f7fa;
}
.conv.active {
  background: #e8f5e9;
}
.mid {
  flex: 1;
  min-width: 0;
}
.row1 {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}
.row2 {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;
}
.preview {
  color: #606266;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.ctx {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.thumb {
  width: 36px;
  height: 36px;
  border-radius: 6px;
}
.ctx-title {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-hdr .t {
  font-weight: 700;
}
.chat-hdr .s {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.muted {
  color: #909399;
}
.msgs {
  height: 360px;
  overflow: auto;
  padding: 8px;
  background: #fafafa;
  border-radius: 8px;
}
.msg {
  display: flex;
  justify-content: flex-start;
  margin: 8px 0;
}
.msg.mine {
  justify-content: flex-end;
}
.bubble {
  max-width: 78%;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 8px 10px;
}
.msg.mine .bubble {
  background: #e8f5e9;
  border-color: #c8e6c9;
}
.who {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.txt {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
}
.ts {
  margin-top: 6px;
  font-size: 11px;
  color: #a8abb2;
  text-align: right;
}
.composer {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  align-items: flex-end;
}
.tips {
  margin-top: 16px;
}
</style>
