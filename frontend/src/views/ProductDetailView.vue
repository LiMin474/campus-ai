<!--
商品详情视图
负责成员B：商品交易 + 求购专区
-->
<template>
  <el-card v-if="detail">
    <h2>{{ detail.title }}</h2>
    <p class="muted">¥{{ detail.price }} · {{ detail.categoryName }} · {{ detail.status }}</p>
    <el-carousel v-if="detail.imageUrls?.length" height="320px">
      <el-carousel-item v-for="(url, idx) in detail.imageUrls" :key="idx">
        <div class="slide" :style="{ backgroundImage: `url(${url})` }" />
      </el-carousel-item>
    </el-carousel>
    <div class="seller" v-if="detail.seller">
      <span>卖家：{{ detail.seller.nickname }}</span>
      <el-tag size="small" type="success">{{ detail.seller.creditLevel }}</el-tag>
    </div>
    <p class="desc">{{ detail.description }}</p>
    <el-space wrap>
      <el-button type="primary" v-if="auth.token" @click="buy">购买（创建订单）</el-button>
      <el-button
        v-if="auth.token && detail.seller?.id && auth.user?.id !== detail.seller.id"
        @click="contactSeller"
      >
        联系卖家
      </el-button>
      <el-button v-if="!auth.token" @click="$router.push('/login')">登录后购买</el-button>
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
  price: number;
  categoryName: string;
  status: string;
  imageUrls: string[];
  seller?: { id: number; nickname: string; creditLevel: string };
};

const detail = ref<Detail | null>(null);

async function load() {
  const id = route.params.id as string;
  const { data } = await http.get<ApiResponse<Detail>>(`/products/${id}`);
  if (data.code === 200) {
    detail.value = data.data;
  }
}

async function contactSeller() {
  if (!detail.value?.seller?.id) return;
  try {
    const { data } = await http.post<ApiResponse<{ conversationId: number }>>("/chat/conversations/start", {
      peerUserId: detail.value.seller.id,
      contextType: "PRODUCT",
      contextId: detail.value.id
    });
    if (data.code !== 200) {
      ElMessage.error(data.message || "打开会话失败");
      return;
    }
    await router.push({ path: "/messages", query: { c: String(data.data.conversationId) } });
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "打开会话失败");
  }
}

async function buy() {
  try {
    const { data } = await http.post<ApiResponse<number>>("/orders", { productId: Number(route.params.id) });
    if (data.code !== 200) {
      ElMessage.error(data.message || "下单失败");
      return;
    }
    ElMessage.success(`订单已创建，ID：${data.data}`);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "下单失败");
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
.slide {
  height: 320px;
  background-size: cover;
  background-position: center;
  border-radius: 8px;
}
.seller {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 12px 0;
}
.desc {
  white-space: pre-wrap;
  line-height: 1.6;
  margin: 12px 0 16px;
}
</style>
