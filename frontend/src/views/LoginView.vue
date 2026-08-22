<template>
  <el-card class="box">
    <h2>登录</h2>
    <el-form :model="form" label-width="88px" @submit.prevent="submit">
      <el-form-item label="模式">
        <el-radio-group v-model="form.mode">
          <el-radio-button label="student">学生</el-radio-button>
          <el-radio-button label="admin">管理员</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="账号">
        <el-input v-model="form.account" placeholder="学号或手机号" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">登录</el-button>
        <el-button @click="$router.push('/register')">学号注册</el-button>
        <el-button @click="$router.push('/email-register')">邮箱注册</el-button>
      </el-form-item>
      <el-form-item>
        <el-link type="primary" @click="$router.push('/forgot-password')">忘记密码？</el-link>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { http, type ApiResponse } from "../api/http";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const loading = ref(false);

const form = reactive({
  mode: "student",
  account: "",
  password: ""
});

async function submit() {
  loading.value = true;
  try {
    const { data } = await http.post<ApiResponse<{ token: string; userId: number; nickname: string; role: string }>>(
      "/auth/login",
      form
    );
    if (data.code !== 200) {
      ElMessage.error(data.message || "登录失败");
      return;
    }
    auth.setToken(data.data.token);
    auth.setUser({ id: data.data.userId, nickname: data.data.nickname, role: data.data.role });
    const redirect = (route.query.redirect as string) || "/";
    await router.replace(redirect);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "登录失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.box {
  max-width: 520px;
  margin: 40px auto;
}
</style>
