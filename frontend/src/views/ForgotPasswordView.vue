<template>
  <el-card class="box">
    <h2>忘记密码</h2>
    <el-steps :active="step" finish-status="success" simple style="margin-bottom: 24px">
      <el-step title="验证身份" />
      <el-step title="重置密码" />
    </el-steps>

    <el-form v-if="step === 0" :model="form1" label-width="80px">
      <el-form-item label="学号">
        <el-input v-model="form1.studentNo" placeholder="请输入学号" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form1.email" placeholder="请输入注册时填写的邮箱">
          <template #append>
            <el-button @click="sendCode" :disabled="countdown > 0">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="验证码">
        <el-input v-model="form1.verifyCode" placeholder="请输入邮箱验证码" maxlength="5" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="verifyIdentity" :loading="loading">验证</el-button>
        <el-button @click="$router.push('/login')">返回登录</el-button>
      </el-form-item>
    </el-form>

    <el-form v-else :model="form2" label-width="80px">
      <el-form-item label="新密码">
        <el-input v-model="form2.password" type="password" show-password placeholder="请输入新密码" />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form2.confirmPassword" type="password" show-password placeholder="请确认新密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="resetPassword" :loading="loading">重置密码</el-button>
        <el-button @click="$router.push('/login')">返回登录</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { http, type ApiResponse } from "../api/http";

const router = useRouter();
const step = ref(0);
const loading = ref(false);
const countdown = ref(0);
let countdownTimer: number | null = null;

const form1 = reactive({
  studentNo: "",
  email: "",
  verifyCode: ""
});

const form2 = reactive({
  password: "",
  confirmPassword: ""
});

async function sendCode() {
  if (!form1.studentNo) {
    ElMessage.warning("请输入学号");
    return;
  }
  if (!form1.email) {
    ElMessage.warning("请输入邮箱");
    return;
  }

  loading.value = true;
  try {
    const { data } = await http.get<ApiResponse<void>>("/auth/send-reset-pwd-code", {
      params: { studentNo: form1.studentNo, email: form1.email }
    });
    if (data.code !== 200) {
      ElMessage.error(data.message || "发送失败");
      return;
    }
    ElMessage.success("验证码已发送至邮箱");
    countdown.value = 60;
    countdownTimer = window.setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
  } catch (e: any) {
    ElMessage.error(e?.message || "发送失败");
  } finally {
    loading.value = false;
  }
}

async function verifyIdentity() {
  if (!form1.studentNo || !form1.email || !form1.verifyCode) {
    ElMessage.warning("请填写完整信息");
    return;
  }

  loading.value = true;
  try {
    const { data } = await http.get<ApiResponse<string>>("/auth/verify-reset-pwd-code", {
      params: {
        studentNo: form1.studentNo,
        email: form1.email,
        verifyCode: form1.verifyCode
      }
    });
    if (data.code !== 200) {
      ElMessage.error(data.message || "验证失败");
      return;
    }
    step.value = 1;
  } catch (e: any) {
    ElMessage.error(e?.message || "验证失败");
  } finally {
    loading.value = false;
  }
}

async function resetPassword() {
  if (!form2.password || !form2.confirmPassword) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  if (form2.password !== form2.confirmPassword) {
    ElMessage.error("两次输入的密码不一致");
    return;
  }
  if (form2.password.length < 6) {
    ElMessage.error("密码长度不能少于6位");
    return;
  }

  loading.value = true;
  try {
    const { data } = await http.post<ApiResponse<void>>(
      "/auth/reset-password",
      {
        studentNo: form1.studentNo,
        email: form1.email,
        verifyCode: form1.verifyCode,
        newPassword: form2.password
      }
    );
    if (data.code !== 200) {
      ElMessage.error(data.message || "重置失败");
      return;
    }
    ElMessage.success("密码重置成功，请使用新密码登录");
    router.push("/login");
  } catch (e: any) {
    ElMessage.error(e?.message || "重置失败");
  } finally {
    loading.value = false;
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
});
</script>

<style scoped>
.box {
  max-width: 520px;
  margin: 40px auto;
}
</style>