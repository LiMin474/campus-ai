<!--
求购详情视图
负责成员B：商品交易 + 求购专区
-->
<template>
  <el-card v-if="detail">
    <h2>{{ detail.title }}</h2>
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
};

const detail = ref<Detail | null>(null);

async function load() {
  const id = route.params.id as string;
  const { data } = await http.get<ApiResponse<Detail>>(`/wanted/${id}`);
  if (data.code === 200) {
    detail.value = data.data;
  }
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
</style>
