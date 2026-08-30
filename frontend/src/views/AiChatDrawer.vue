<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="420px"
    :with-header="false"
    :destroy-on-close="false"
    :before-close="handleClose"
  >
    <div class="drawer-chat">
      <AiChatView />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import AiChatView from "./AiChatView.vue";

const visible = defineModel<boolean>({ required: true });

function handleClose() {
  visible.value = false;
}
</script>

<style scoped>
.drawer-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
}
/* 抽屉内部覆盖 AiChatView 的全屏高度限制 */
.drawer-chat :deep(.ai-chat) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>