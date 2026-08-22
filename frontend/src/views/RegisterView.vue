<template>
  <el-card class="box">
    <h2>学号注册</h2>
    <el-form :model="form" label-width="88px" @submit.prevent="submit">
      <el-form-item label="学号">
        <el-input v-model="form.studentNo" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码（至少6位）" />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">注册</el-button>
        <el-button @click="$router.push('/login')">返回登录</el-button>
        <el-button @click="$router.push('/email-register')">邮箱注册</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { http, type ApiResponse } from "../api/http";

const router = useRouter();
const loading = ref(false);
const form = reactive({
  studentNo: "",
  phone: "",
  nickname: "",
  password: "",
  confirmPassword: ""
});

async function submit() {
  if (!form.studentNo || !form.phone || !form.nickname || !form.password || !form.confirmPassword) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  if (form.password.length < 6) {
    ElMessage.warning("密码长度不能少于6位");
    return;
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.error("两次输入的密码不一致");
    return;
  }

  loading.value = true;
  try {
    const { data } = await http.post<ApiResponse<null>>("/auth/register", {
      studentNo: form.studentNo,
      phone: form.phone,
      nickname: form.nickname,
      password: form.password
    });
    if (data.code !== 200) {
      ElMessage.error(data.message || "注册失败");
      return;
    }
    ElMessage.success("注册成功，请登录");
    await router.push("/login");
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "注册失败");
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