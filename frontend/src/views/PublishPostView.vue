<!--
发布帖子视图
负责成员D：社区频道 + 用户管理
-->
<template>
  <el-card class="box">
    <h2>发布帖子</h2>
    <el-form :model="form" label-width="88px" @submit.prevent="submit">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" placeholder="请输入帖子标题" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="内容" required>
        <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入帖子内容" maxlength="2000" show-word-limit />
      </el-form-item>
      <el-form-item label="图片">
        <el-upload
          list-type="picture-card"
          :file-list="fileList"
          :http-request="uploadFile"
          :on-remove="handleRemove"
          accept=".jpg,.jpeg,.png,.gif,.webp"
          :limit="9"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="tip">支持 jpg/png/gif/webp，上传到服务器本地目录。</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">发布</el-button>
        <el-button @click="$router.push('/posts')">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import type { UploadRequestOptions, UploadUserFile } from "element-plus";
import { http, type ApiResponse } from "../api/http";

const router = useRouter();
const loading = ref(false);
const imageUrls = ref<string[]>([]);

const fileList = computed<UploadUserFile[]>(() =>
  imageUrls.value.map((url, idx) => ({
    name: `图片${idx + 1}`,
    url,
    status: "success"
  }))
);

const form = reactive({
  title: "",
  content: ""
});

async function uploadFile(options: UploadRequestOptions) {
  const fd = new FormData();
  fd.append("file", options.file as File);
  try {
    const { data } = await http.post<ApiResponse<{ url: string }>>(
      "/files/upload",
      fd
    );
    if (data.code !== 200) {
      throw new Error(data.message || "上传失败");
    }
    imageUrls.value = [...imageUrls.value, data.data.url];
    options.onSuccess?.(data);
  } catch (e: any) {
    options.onError?.(e);
    ElMessage.error(e?.message || "上传失败");
  }
}

function handleRemove(file: UploadUserFile) {
  const url = file.url;
  if (!url) return;
  imageUrls.value = imageUrls.value.filter((u) => u !== url);
}

async function submit() {
  if (!form.title || !form.content) {
    ElMessage.error("请填写标题和内容");
    return;
  }
  
  loading.value = true;
  try {
    const { data } = await http.post<ApiResponse<number>>(
      "/posts",
      {
        title: form.title,
        content: form.content,
        imageUrls: imageUrls.value.length ? imageUrls.value : undefined
      }
    );
    if (data.code !== 200) {
      ElMessage.error(data.message || "发布失败");
      return;
    }
    ElMessage.success("发布成功");
    await router.push("/posts");
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "发布失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.box {
  max-width: 800px;
  margin: 40px auto;
}
.tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>