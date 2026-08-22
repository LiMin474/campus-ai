<!--
发布商品视图
负责成员B：商品交易 + 求购专区
-->
<template>
  <el-card class="box">
    <h2>发布商品</h2>
    <el-form :model="form" label-width="100px" @submit.prevent="submit">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="120" show-word-limit />
      </el-form-item>
      <el-form-item label="分类" required>
        <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="价格" required>
        <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
      </el-form-item>
      <el-form-item label="成色">
        <el-select v-model="form.conditionLabel" clearable placeholder="可选" style="width: 100%">
          <el-option label="全新" value="全新" />
          <el-option label="九成新" value="九成新" />
          <el-option label="八成新" value="八成新" />
          <el-option label="七成新" value="七成新" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <div class="description-wrapper">
          <el-input v-model="form.description" type="textarea" rows="5" maxlength="5000" show-word-limit />
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
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import type { UploadRequestOptions, UploadUserFile } from "element-plus";
import { http, type ApiResponse } from "../api/http";

type Category = { id: number; name: string };

const router = useRouter();
const categories = ref<Category[]>([]);
const imageUrls = ref<string[]>([]);
const loading = ref(false);
const polishing = ref(false);

const fileList = computed<UploadUserFile[]>(() =>
  imageUrls.value.map((url, idx) => ({
    name: `图片${idx + 1}`,
    url,
    status: "success"
  }))
);

const form = reactive({
  title: "",
  categoryId: undefined as number | undefined,
  price: 0,
  conditionLabel: "" as string | undefined,
  description: ""
});

async function loadCategories() {
  const { data } = await http.get<ApiResponse<Category[]>>("/categories");
  if (data.code === 200) {
    categories.value = data.data;
  }
}

async function uploadFile(options: UploadRequestOptions) {
  const fd = new FormData();
  fd.append("file", options.file as File);
  try {
    const { data } = await http.post<ApiResponse<{ url: string }>>("/files/upload", fd);
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
  if (!form.title.trim()) {
    ElMessage.warning("请填写标题");
    return;
  }
  if (!form.categoryId) {
    ElMessage.warning("请选择分类");
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.post<ApiResponse<number>>("/products", {
      title: form.title.trim(),
      description: form.description || undefined,
      price: form.price,
      categoryId: form.categoryId,
      conditionLabel: form.conditionLabel || undefined,
      imageUrls: imageUrls.value.length ? imageUrls.value : undefined
    });
    if (data.code !== 200) {
      ElMessage.error(data.message || "发布失败");
      return;
    }
    ElMessage.success("发布成功");
    await router.push(`/products/${data.data}`);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "发布失败");
  } finally {
    loading.value = false;
  }
}

async function polishDescription() {
  if (!form.description.trim()) {
    ElMessage.warning("请先输入描述内容");
    return;
  }
  polishing.value = true;
  try {
    const { data } = await http.post<ApiResponse<{ text: string }>>("/ai/polish", {
      text: form.description
    });
    if (data.code === 200) {
      form.description = data.data.text;
      ElMessage.success("润色完成");
    } else {
      ElMessage.error(data.message || "润色失败");
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "润色失败");
  } finally {
    polishing.value = false;
  }
}

onMounted(async () => {
  await loadCategories();
});
</script>

<style scoped>
.box {
  max-width: 720px;
  margin: 0 auto;
}
.tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
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
</style>
