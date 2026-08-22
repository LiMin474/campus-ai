<template>
  <el-card class="box">
    <h2>邮箱注册</h2>
    <el-steps :active="step" finish-status="success" simple style="margin-bottom: 24px">
      <el-step title="填写信息" />
      <el-step title="验证邮箱" />
      <el-step title="完成注册" />
    </el-steps>

    <el-form v-if="step === 0" :model="form" label-width="80px">
      <el-form-item label="学号">
        <el-input v-model="form.studentNo" placeholder="请输入学号" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" placeholder="请输入邮箱">
          <template #append>
            <el-button @click="sendCode" :disabled="countdown > 0">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="验证码">
        <el-input v-model="form.verifyCode" placeholder="请输入邮箱验证码" maxlength="5" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码（至少6位）" />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="nextStep" :loading="loading">下一步</el-button>
        <el-button @click="$router.push('/login')">返回登录</el-button>
      </el-form-item>
    </el-form>

    <div v-else-if="step === 1" class="success-box">
      <el-icon size="64" color="#67C23A"><Check /></el-icon>
      <h3>验证码已发送！</h3>
      <p>我们已向 <strong>{{ form.email }}</strong> 发送了验证码</p>
      <p>请查收邮件并输入验证码完成注册</p>
      <el-button type="primary" @click="step = 0">重新输入验证码</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Check } from "@element-plus/icons-vue";
import { http, type ApiResponse } from "../api/http";

const router = useRouter();
const step = ref(0);
const loading = ref(false);
const countdown = ref(0);
let countdownTimer: number | null = null;

const form = reactive({
  studentNo: "",
  nickname: "",
  email: "",
  verifyCode: "",
  password: "",
  confirmPassword: ""
});

async function sendCode() {
  if (!form.studentNo) {
    ElMessage.warning("请输入学号");
    return;
  }
  if (!form.email) {
    ElMessage.warning("请输入邮箱");
    return;
  }
  if (!form.email.includes("@")) {
    ElMessage.warning("请输入有效的邮箱地址");
    return;
  }

  loading.value = true;
  try {
    const { data } = await http.get<ApiResponse<string>>("/auth/send-register-code", {
      params: { studentNo: form.studentNo, email: form.email }
    });
    if (data.code !== 200) {
      ElMessage.error(data.message || "发送失败");
      return;
    }
    ElMessage.success("验证码已发送至邮箱");
    step.value = 1;
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

async function nextStep() {
  if (!form.studentNo || !form.nickname || !form.email || !form.verifyCode || !form.password || !form.confirmPassword) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  if (!form.email.includes("@")) {
    ElMessage.warning("请输入有效的邮箱地址");
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
    const { data } = await http.post<ApiResponse<string>>(
      "/auth/complete-register",
      null,
      {
        params: {
          studentNo: form.studentNo,
          email: form.email,
          verifyCode: form.verifyCode,
          nickname: form.nickname,
          password: form.password
        }
      }
    );
    if (data.code !== 200) {
      ElMessage.error(data.message || "注册失败");
      return;
    }
    ElMessage.success("注册成功！");
    await router.push("/login");
  } catch (e: any) {
    ElMessage.error(e?.message || "注册失败");
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

.success-box {
  text-align: center;
  padding: 40px 20px;
}

.success-box h3 {
  margin: 20px 0 10px;
  color: #67C23A;
}

.success-box p {
  color: #606266;
  margin: 10px 0;
}
</style>