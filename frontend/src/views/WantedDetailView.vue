<!--
求购详情视图
负责成员B：商品交易 + 求购专区
-->
<template>
  <el-card v-if="detail">
    <h2>{{ detail.title }}</h2>
    <el-carousel v-if="detail.imageUrls?.length" height="320px">
      <el-carousel-item v-for="(url, idx) in detail.imageUrls" :key="idx">
        <div class="slide" :style="{ backgroundImage: `url(${url})` }" />
      </el-carousel-item>
    </el-carousel>
    <p class="muted">
      预算：
      <span v-if="detail.budgetMin != null || detail.budgetMax != null">
        ¥{{ detail.budgetMin ?? "?" }} - ¥{{ detail.budgetMax ?? "?" }}
      </span>
      <span v-else>面议</span>
      · {{ detail.categoryName }} · {{ detail.userNickname }}
    </p>
    <p class="desc">{{ detail.description }}</p>
    <el-space wrap>
      <el-button
        type="primary"
        v-if="auth.token && auth.user?.id && auth.user.id !== detail.userId"
        @click="iHaveThis"
      >
        我有这个（发起聊天）
      </el-button>
      <el-button v-if="!auth.token" @click="$router.push('/login')">登录后联系</el-button>
      <el-button v-if="auth.token && auth.user?.id === detail.userId" disabled>这是你发布的求购</el-button>
    </el-space>

    <!-- AI 为你匹配：匹配 Agent 自动检索的在售商品 -->
    <div v-if="matches.length" class="ai-match">
      <div class="ai-match-head">
        <span class="ai-badge">AI</span>
        <span class="ai-match-title">为你匹配</span>
        <span class="ai-match-sub">匹配 Agent 自动检索在售商品</span>
      </div>
      <div class="source-grid">
        <div
          v-for="m in matches"
          :key="m.productId"
          class="source-card"
          @click="goDetail(m.productId)"
        >
          <div class="source-cover" :style="coverStyle(m.coverImage)" />
          <div class="source-info">
            <div class="source-title">{{ m.title }}</div>
            <div class="source-price">¥{{ m.price }}</div>
          </div>
        </div>
      </div>
      <div class="ai-match-note">点击商品卡片可查看详情</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { http, type ApiResponse } from "../api/http";
import { useAuthStore } from "../stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

type Detail = {
  id: number;
  title: string;
  description: string;
  budgetMin?: number | null;
  budgetMax?: number | null;
  categoryName: string;
  userId: number;
  userNickname: string;
  imageUrls?: string[];
};

type MatchItem = {
  productId: number;
  title: string;
  price: number;
  coverImage?: string;
};

const detail = ref<Detail | null>(null);
const matches = ref<MatchItem[]>([]);

async function load() {
  const id = route.params.id as string;
  const { data } = await http.get<ApiResponse<Detail>>(`/wanted/${id}`);
  if (data.code === 200) {
    detail.value = data.data;
  }
  loadMatches();
}

async function loadMatches() {
  const id = route.params.id as string;
  try {
    const { data } = await http.get<ApiResponse<MatchItem[]>>(`/wanted/${id}/matches`);
    if (data.code === 200) {
      matches.value = data.data;
    }
  } catch (e: any) {
    matches.value = [];
  }
}

function coverStyle(url?: string) {
  if (!url) return { backgroundColor: "#e8f5e9" };
  return { backgroundImage: `url(${url})`, backgroundSize: "cover", backgroundPosition: "center" };
}

function goDetail(id: number) {
  router.push(`/product/${id}`);
}

async function iHaveThis() {
  if (!detail.value) return;
  try {
    const { data } = await http.post<ApiResponse<{ conversationId: number }>>("/chat/conversations/start", {
      peerUserId: detail.value.userId,
      contextType: "WANTED",
      contextId: detail.value.id
    });
    if (data.code !== 200) {
      ElMessage.error(data.message || "发起聊天失败");
      return;
    }
    await router.push({ path: "/messages", query: { c: String(data.data.conversationId) } });
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "发起聊天失败");
  }
}

onMounted(async () => {
  await auth.fetchUser();
  load();
});
</script>

<style scoped>
.muted {
  color: #909399;
}
.desc {
  white-space: pre-wrap;
  line-height: 1.6;
  margin: 16px 0;
}
.slide {
  height: 320px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  background-color: #f5f7fa;
}

/* AI 为你匹配 */
.ai-match {
  margin-top: 20px;
  border: 1px solid #e0e6ec;
  border-radius: 10px;
  overflow: hidden;
}
.ai-match-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f6f8f7;
  border-bottom: 1px solid #e0e6ec;
}
.ai-badge {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: #0f9d58;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ai-match-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}
.ai-match-sub {
  font-size: 12px;
  color: #909399;
}
.ai-match-note {
  padding: 8px 14px;
  font-size: 12px;
  color: #909399;
}

/* 商品卡：复用 AiChatView source-card 样式 */
.source-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  padding: 12px 14px;
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
</style>
