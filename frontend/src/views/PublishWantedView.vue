<template>
  <div class="publish-wanted">
    <div class="form-container">
      <h2 class="title">发布求购</h2>
      
      <el-form :model="form" label-width="120px" class="wanted-form">
        <el-form-item label="求购标题" required>
          <el-input v-model="form.title" placeholder="请输入求购物品名称" />
        </el-form-item>

        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="求购描述">
          <div class="description-wrapper">
            <el-input v-model="form.description" type="textarea" :rows="5" maxlength="5000" show-word-limit placeholder="请描述你想要的物品，包括规格、新旧程度等要求" />
            <el-button 
              type="primary" 
              icon="Sparkles" 
              class="ai-button"
              @click="polishDescription"
              :loading="polishing"
            >
              AI润色
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="预算范围">
          <el-row :gutter="12">
            <el-col :span="11">
              <el-input v-model.number="form.budgetMin" placeholder="最低预算（元）" type="number" />
            </el-col>
            <el-col :span="2" class="text-center">-</el-col>
            <el-col :span="11">
              <el-input v-model.number="form.budgetMax" placeholder="最高预算（元）" type="number" />
            </el-col>
          </el-row>
        </el-form-item>
        
        <el-form-item label="参考图片">
          <el-upload
            list-type="picture-card"
            :file-list="fileList"
            :http-request="uploadFile"
            :on-remove="handleRemove"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            :limit="3"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="tip">支持 jpg/png/gif/webp，最多上传3张参考图片</div>
        </el-form-item>
        
        <el-form-item class="submit-btn">
          <el-button type="success" @click="submit" :loading="loading">发布</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { http, type ApiResponse } from "../api/http";
import type { UploadUserFile } from "element-plus";

const router = useRouter();
const loading = ref(false);
const polishing = ref(false);
const categories = ref<{ id: number; name: string }[]>([]);
const imageUrls = ref<string[]>([]);
const fileList = computed<UploadUserFile[]>(() =>
  imageUrls.value.map((url, idx) => ({
    name: `图片${idx + 1}`,
    url,
    status: "success"
  }))
);

const form = ref({
  title: "",
  description: "",
  budgetMin: undefined as number | undefined,
  budgetMax: undefined as number | undefined,
  categoryId: undefined as number | undefined
});

async function uploadFile(options: any) {
  const fd = new FormData();
  fd.append("file", options.file);
  fd.append("type", "wanted");
  try {
    console.log("开始上传文件:", options.file.name);
    const { data } = await http.post<ApiResponse<{ url: string }>>("/files/upload", fd, {
      headers: { "Content-Type": "multipart/form-data" }
    });
    console.log("上传响应:", data);
    if (data.code !== 200) {
      throw new Error(data.message || "上传失败");
    }
    imageUrls.value = [...imageUrls.value, data.data.url];
    console.log("上传成功后 imageUrls:", imageUrls.value);
    options.onSuccess?.(data);
  } catch (e: any) {
    console.error("上传失败:", e);
    options.onError?.(e);
    ElMessage.error(e?.message || "上传失败");
  }
}

function handleRemove(file: any) {
  const url = file.url;
  if (!url) return;
  imageUrls.value = imageUrls.value.filter((u) => u !== url);
}

async function loadCategories() {
  const { data } = await http.get<ApiResponse<{ id: number; name: string }[]>>("/categories");
  if (data.code === 200) {
    categories.value = data.data;
  }
}

async function polishDescription() {
  if (!form.value.description.trim()) {
    ElMessage.warning("请先输入求购描述");
    return;
  }
  polishing.value = true;
  try {
    const { data } = await http.post<ApiResponse<{ polishedText: string }>>("/ai/polish", {
      text: form.value.description.trim(),
      type: "wanted",
      title: form.value.title
    });
    if (data.code === 200) {
      form.value.description = data.data.polishedText;
      ElMessage.success("润色成功");
    } else {
      ElMessage.error(data.message || "润色失败");
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "润色失败，请重试");
  } finally {
    polishing.value = false;
  }
}

async function submit() {
  if (!form.value.title.trim()) {
    ElMessage.warning("请输入求购标题");
    return;
  }
  if (!form.value.categoryId) {
    ElMessage.warning("请选择分类");
    return;
  }
  
  console.log("提交时 imageUrls:", imageUrls.value);
  
  loading.value = true;
  try {
    const requestData = {
      title: form.value.title.trim(),
      description: form.value.description.trim() || undefined,
      budgetMin: form.value.budgetMin,
      budgetMax: form.value.budgetMax,
      categoryId: form.value.categoryId,
      imageUrls: imageUrls.value.length ? imageUrls.value : undefined
    };
    console.log("提交请求数据:", requestData);
    
    const { data } = await http.post<ApiResponse<number>>("/wanted", requestData);
    
    if (data.code === 200) {
      ElMessage.success("发布成功！");
      await router.push(`/wanted/${data.data}`);
    } else {
      ElMessage.error(data.message || "发布失败");
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "发布失败，请重试");
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadCategories();
});
</script>

<style scoped>
.publish-wanted {
  padding: 20px;
}

.form-container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.title {
  background-color: #0f9d58;
  color: white;
  padding: 12px 24px;
  border-radius: 6px;
  display: inline-block;
  margin-bottom: 24px;
  font-size: 24px;
}

.wanted-form {
  font-size: 16px;
}

.wanted-form .el-form-item {
  margin-bottom: 24px;
}

.wanted-form .el-form-item__label {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.wanted-form .el-input,
.wanted-form .el-select {
  font-size: 16px;
  width: 100%;
}

.wanted-form .el-input__inner,
.wanted-form .el-select__input {
  padding: 12px 15px;
  font-size: 16px;
}

.wanted-form .el-textarea__inner {
  padding: 12px 15px;
  font-size: 16px;
  line-height: 1.6;
}

.text-center {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.description-wrapper {
  position: relative;
  padding-bottom: 50px;
  width: 100%;
}
.description-wrapper .el-input {
  width: 100% !important;
}
.description-wrapper .el-textarea {
  width: 100% !important;
}
.ai-button {
  position: absolute;
  right: 0;
  bottom: 0;
  padding: 10px 30px;
  font-size: 15px;
  min-width: 100px;
}

.submit-btn {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.submit-btn .el-button {
  padding: 14px 36px;
  font-size: 18px;
}
</style>
