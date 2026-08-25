<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand" @click="$router.push('/')">
        <span class="logo">♻</span>
        校园二手
      </div>
      <el-menu
        mode="horizontal"
        :ellipsis="false"
        class="menu"
        :key="menuActive || '_none_'"
        :default-active="menuActive"
      >
        <el-menu-item index="/" @click="router.push('/')">首页</el-menu-item>
        <el-menu-item index="/posts" @click="router.push('/posts')">社区</el-menu-item>
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
            <el-button
              type="primary"
              round
              :class="{ 'publish-active': isNavActive('/publish') }"
              @click="router.push('/publish')"
            >
              <el-icon><Plus /></el-icon>
              发布
            </el-button>
            <button
              type="button"
              class="nav-action"
              :class="{ 'is-active': isNavActive('/messages') }"
              @click="router.push('/messages')"
            >
              <el-icon><ChatDotRound /></el-icon>
              <span>消息</span>
            </button>
            <button
              type="button"
              class="nav-action"
              :class="{ 'is-active': isNavActive('/orders') }"
              @click="router.push('/orders')"
            >
              我的订单
            </button>
            <button
              type="button"
              class="nav-action"
              :class="{ 'is-active': isNavActive('/my-publish') }"
              @click="router.push('/my-publish')"
            >
              我的发布
            </button>
            <div
              class="profile-entry"
              :class="{ 'is-active': isNavActive('/profile') }"
              title="个人中心"
              @click="router.push('/profile')"
            >
              <el-avatar :size="32" :src="auth.user.avatarUrl || defaultAvatar">
                {{ auth.user.nickname?.charAt(0) || "用" }}
              </el-avatar>
            </div>
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
        <router-view v-slot="{ Component }">
          <!-- keep-alive 只缓存 AI 助手页：跳详情返回时聊天记录不丢失 -->
          <keep-alive :include="['AiChatView']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </el-main>

    <!-- AI 助手浮动球：右下角悬浮，点击进入对话页 -->
    <div
      v-if="route.path !== '/ai-chat'"
      class="ai-fab"
      title="AI 购物助手"
      @click="router.push('/ai-chat')"
    >
      <el-icon :size="26"><MagicStick /></el-icon>
    </div>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "./stores/auth";
import { ArrowDown, Search, ChatDotRound, Plus, MagicStick } from "@element-plus/icons-vue";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const searchKeyword = ref("");
const defaultAvatar = "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

const menuActive = computed(() => {
  if (route.path === "/") return "/";
  if (route.path === "/posts" || route.path.startsWith("/posts/")) return "/posts";
  return "";
});

function isNavActive(path: string) {
  if (path === "/publish") {
    return route.path === "/publish" || route.path.startsWith("/publish-");
  }
  if (path === "/orders") {
    return route.path === "/orders" || route.path.startsWith("/orders/");
  }
  return route.path === path || route.path.startsWith(`${path}/`);
}

watch(
  () => route.query.keyword,
  (keyword) => {
    if (route.path === "/products" && typeof keyword === "string") {
      searchKeyword.value = keyword;
    }
  },
  { immediate: true }
);

onMounted(() => {
  auth.fetchUser();
});

function logout() {
  auth.clear();
  router.push("/");
}

function search() {
  const keyword = searchKeyword.value.trim();
  if (keyword) {
    router.push({ path: "/products", query: { keyword } });
  } else {
    router.push({ path: "/products" });
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
  --nav-primary: #0f9d58;
  --nav-primary-hover-bg: #e8f7ef;
  --nav-primary-active-bg: #d0efe0;
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

.menu :deep(.el-menu-item) {
  font-size: 16px;
  color: #606266;
  border-bottom: 3px solid transparent !important;
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}

.menu :deep(.el-menu-item:hover) {
  color: var(--nav-primary);
  background: var(--nav-primary-hover-bg) !important;
}

.menu :deep(.el-menu-item.is-active) {
  color: var(--nav-primary) !important;
  font-weight: 700;
  background: var(--nav-primary-active-bg) !important;
  border-bottom-color: var(--nav-primary) !important;
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

.nav-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #606266;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition: color 0.2s, background 0.2s, box-shadow 0.2s;
}

.nav-action:hover {
  color: var(--nav-primary);
  background: var(--nav-primary-hover-bg);
}

.nav-action.is-active {
  color: var(--nav-primary);
  font-weight: 600;
  background: var(--nav-primary-active-bg);
  box-shadow: inset 0 -2px 0 var(--nav-primary);
}

.publish-active {
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.9), 0 0 0 4px var(--nav-primary);
}

.header :deep(.el-button--primary) {
  --el-button-bg-color: var(--nav-primary);
  --el-button-border-color: var(--nav-primary);
  --el-button-hover-bg-color: #0d8a4d;
  --el-button-hover-border-color: #0d8a4d;
  --el-button-active-bg-color: #0b7844;
  --el-button-active-border-color: #0b7844;
}

.profile-entry {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 50%;
  transition: box-shadow 0.2s;
}

.profile-entry:hover :deep(.el-avatar),
.profile-entry.is-active :deep(.el-avatar) {
  box-shadow: 0 0 0 2px var(--nav-primary);
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

/* AI 助手浮动球：右下角悬浮 */
.ai-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0f9d58, #0bb767);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(15, 157, 88, 0.35);
  z-index: 2000;
  transition: transform 0.2s, box-shadow 0.2s;
}
.ai-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 8px 24px rgba(15, 157, 88, 0.5);
}
</style>
