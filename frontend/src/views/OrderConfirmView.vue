<template>
  <el-card class="box">
    <h2>扫码确认收货</h2>
    <p v-if="!token" class="muted">链接无效，缺少 token 参数。</p>
    <template v-else>
      <p>确认后将完成订单并发放减碳积分，请确保已当面验货。</p>
      <el-button type="primary" :loading="loading" @click="submit">确认收货</el-button>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { http, type ApiResponse } from "../api/http";

const route = useRoute();
const router = useRouter();
const loading = ref(false);

const token = computed(() => (route.query.token as string) || "");

async function submit() {
  if (!token.value) return;
  loading.value = true;
  try {
    const { data } = await http.post<ApiResponse<null>>("/orders/confirm-with-token", { token: token.value });
    if (data.code !== 200) {
      ElMessage.error(data.message || "确认失败");
      return;
    }
    ElMessage.success("确认收货成功");
    await router.replace("/orders");
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "确认失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.box {
  max-width: 520px;
  margin: 24px auto;
}
.muted {
  color: #909399;
}
</style>
