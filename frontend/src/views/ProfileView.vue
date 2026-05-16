<!--
用户中心视图
负责成员A：用户中心 + 订单评价
-->
<template>
  <el-card v-if="profile">
    <h2>个人中心</h2>
    <p>昵称：{{ profile.nickname }}</p>
    <p>信誉：{{ profile.creditLevel }}（{{ profile.creditScore }}）</p>
    <p>减碳积分：{{ profile.carbonPoints }}</p>
    <el-button type="primary" @click="signIn">每日签到</el-button>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { http, type ApiResponse } from "../api/http";

type Profile = {
  nickname: string;
  creditScore: number;
  creditLevel: string;
  carbonPoints: number;
};

const profile = ref<Profile | null>(null);

async function load() {
  const { data } = await http.get<ApiResponse<Profile>>("/user/me");
  if (data.code === 200) {
    profile.value = data.data;
  }
}

async function signIn() {
  const { data } = await http.post<ApiResponse<number>>("/user/sign-in");
  if (data.code !== 200) {
    ElMessage.error(data.message || "签到失败");
    return;
  }
  ElMessage.success(`签到成功，当前积分：${data.data}`);
  await load();
}

onMounted(load);
</script>
