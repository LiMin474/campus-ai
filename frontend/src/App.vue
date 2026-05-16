<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand" @click="$router.push('/')">
        <span class="logo">♻</span>
        校园二手
      </div>
      <el-menu mode="horizontal" :ellipsis="false" class="menu" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/posts">社区</el-menu-item>
      </el-menu>
      <div class="search">
        <el-input
          v-model="searchKeyword"
          style="width: 600px"
          placeholder="搜索您感兴趣的商品或求购..."
          clearable
          @keyup.enter="search"
        >
          <template #append>
            <el-button @click="search"
              ><el-icon><Search /></el-icon
            ></el-button>
          </template>
        </el-input>
      </div>

      <div class="right">
        <template v-if="auth.token">
          <template v-if="!auth.user">
            <el-button link type="primary" loading>加载中</el-button>
          </template>
          <template v-else-if="auth.user.role === 'ADMIN'">
            <el-dropdown>
              <el-button link type="primary">
                管理后台
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/admin/dashboard')"
                    >仪表盘</el-dropdown-item
                  >
                  <el-dropdown-item @click="$router.push('/admin/users')"
                    >用户管理</el-dropdown-item
                  >
                  <el-dropdown-item @click="$router.push('/admin/reports')"
                    >举报管理</el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <el-button type="primary" round @click="$router.push('/publish')">
              <el-icon><Plus /></el-icon>
              发布
            </el-button>
            <el-button link type="primary" @click="$router.push('/messages')">
              <el-icon><ChatDotRound /></el-icon>
            </el-button>
            <el-button link type="primary" @click="$router.push('/orders')"
              >我的订单</el-button
            >
            <el-button link type="primary" @click="$router.push('/my-products')"
              >我的商品</el-button
            >
            <el-button link type="primary" @click="$router.push('/profile')"
              >个人中心</el-button
            >
          </template>
          <el-button link @click="logout">退出</el-button>
        </template>

        <template v-else>
          <el-button link @click="$router.push('/register')">注册</el-button>
          <el-button link @click="$router.push('/login')">登录</el-button>
        </template>
      </div>
    </el-header>
    <el-main class="main-wrapper">
      <div class="main">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "./stores/auth";
import { ArrowDown, Search, ChatDotRound, Plus } from "@element-plus/icons-vue";
import { ref } from "vue";

const auth = useAuthStore();
const router = useRouter();
const searchKeyword = ref("");

onMounted(() => {
  auth.fetchUser();
});

function logout() {
  auth.clear();
  router.push("/");
}

function search() {
  if (searchKeyword.value) {
    router.push({ path: "/products", query: { keyword: searchKeyword.value } });
  }
}
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  background: #f5f7fa;
  font-family:
    system-ui,
    -apple-system,
    Segoe UI,
    Roboto,
    Helvetica,
    Arial,
    sans-serif;
}
</style>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #0f9d58;
  cursor: pointer;
  padding-right: 24px;
  white-space: nowrap;
}
.logo {
  font-size: 20px;
}
.menu {
  flex: 0 0 auto;
  border-bottom: none;
  margin-right: 30px;
  font-weight: 600;
  font-size: 20px;
}

.search {
  flex: 1;
  max-width: 500px;
  margin-right: 20px;
}
.publish-btn {
  margin-right: 20px;
}
.right {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  margin-left: auto;
  justify-content: flex-end;
}
.header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
  height: 60px;
}
.main-wrapper {
  width: 100%;
  padding: 0;
  margin: 0;
}
.main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  padding: 24px 16px 48px;
}
</style>
