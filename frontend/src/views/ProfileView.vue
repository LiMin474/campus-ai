<template>
  <el-card v-if="profile">
    <h2>个人中心</h2>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="基本信息" name="info">
        <div class="profile-header">
          <div class="avatar-section">
            <el-avatar :size="100" :src="profile.avatarUrl || defaultAvatar">
              {{ profile.nickname?.charAt(0) || '用户' }}
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :http-request="uploadAvatar"
              accept="image/*"
              class="avatar-upload"
            >
              <el-button type="primary" size="small" :loading="uploading">
                {{ uploading ? '上传中...' : '更换头像' }}
              </el-button>
            </el-upload>
          </div>

          <div class="profile-info">
            <p><strong>昵称：</strong>{{ profile.nickname }}</p>
            <p><strong>学号：</strong>{{ profile.studentNo || '未设置' }}</p>
            <p><strong>手机：</strong>{{ profile.phone || '未绑定' }}</p>
            <p><strong>信誉等级：</strong>
              <el-tag type="success">{{ profile.creditLevel }}</el-tag>
              ({{ profile.creditScore }}分)
            </p>
            <p><strong>减碳积分：</strong>
              <el-tag type="warning">{{ profile.carbonPoints }}</el-tag>
            </p>
          </div>
        </div>

        <el-divider />

        <div class="actions">
          <el-button type="primary" @click="showEditDialog = true">编辑资料</el-button>
          <el-button type="success" @click="signIn">每日签到</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="收到的评价" name="received">
        <el-button style="margin-bottom: 12px" @click="loadReceivedReviews">刷新</el-button>
        <div v-if="receivedReviews.length === 0" class="empty-tip">
          暂无收到的评价
        </div>
        <div v-else class="review-list">
          <div v-for="review in receivedReviews" :key="review.id" class="review-card">
            <div class="review-product" v-if="review.productImageUrl">
              <el-image
                :src="review.productImageUrl"
                fit="cover"
                class="product-image"
                :preview-src-list="[review.productImageUrl]"
              />
            </div>
            <div class="review-header">
              <span class="reviewer">{{ review.fromUserNickname }}</span>
              <span class="product">商品：{{ review.orderProductTitle }}</span>
              <span class="time">{{ formatTime(review.createdAt) }}</span>
            </div>
            <div class="review-scores">
              <div class="score-item">
                <span>沟通态度：</span>
                <el-rate :model-value="review.communicationScore" disabled />
              </div>
              <div class="score-item">
                <span>描述相符：</span>
                <el-rate :model-value="review.matchScore" disabled />
              </div>
              <div class="score-item">
                <span>交易速度：</span>
                <el-rate :model-value="review.speedScore" disabled />
              </div>
              <div class="avg-score">
                平均评分：<el-tag type="success">{{ review.avgScore?.toFixed(1) }}</el-tag>
              </div>
            </div>
            <div v-if="review.content" class="review-content">
              <p>{{ review.content }}</p>
            </div>
          </div>
        </div>
        <div v-if="receivedTotal > 0" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="receivedTotal"
            :page-size="pageSize"
            v-model:current-page="receivedPage"
            @current-change="loadReceivedReviews"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="发出的评价" name="given">
        <el-button style="margin-bottom: 12px" @click="loadGivenReviews">刷新</el-button>
        <div v-if="givenReviews.length === 0" class="empty-tip">
          暂无发出的评价
        </div>
        <div v-else class="review-list">
          <div v-for="review in givenReviews" :key="review.id" class="review-card">
            <div class="review-product" v-if="review.productImageUrl">
              <el-image
                :src="review.productImageUrl"
                fit="cover"
                class="product-image"
                :preview-src-list="[review.productImageUrl]"
              />
            </div>
            <div class="review-header">
              <span class="reviewer">评价对象：{{ review.toUserNickname }}</span>
              <span class="product">商品：{{ review.orderProductTitle }}</span>
              <span class="time">{{ formatTime(review.createdAt) }}</span>
            </div>
            <div class="review-scores">
              <div class="score-item">
                <span>沟通态度：</span>
                <el-rate :model-value="review.communicationScore" disabled />
              </div>
              <div class="score-item">
                <span>描述相符：</span>
                <el-rate :model-value="review.matchScore" disabled />
              </div>
              <div class="score-item">
                <span>交易速度：</span>
                <el-rate :model-value="review.speedScore" disabled />
              </div>
              <div class="avg-score">
                平均评分：<el-tag type="success">{{ review.avgScore?.toFixed(1) }}</el-tag>
              </div>
            </div>
            <div v-if="review.content" class="review-content">
              <p>{{ review.content }}</p>
            </div>
          </div>
        </div>
        <div v-if="givenTotal > 0" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="givenTotal"
            :page-size="pageSize"
            v-model:current-page="givenPage"
            @current-change="loadGivenReviews"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="showEditDialog" title="编辑资料" width="400px">
    <el-form :model="editForm" label-width="80px">
      <el-form-item label="昵称">
        <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="editForm.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="editForm.password" type="password" placeholder="不修改请留空" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showEditDialog = false">取消</el-button>
      <el-button type="primary" @click="submitEdit" :loading="editLoading">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import type { UploadRequestOptions } from "element-plus";
import { http, type ApiResponse } from "../api/http";
import { useAuthStore } from "../stores/auth";

type Profile = {
  id: number;
  nickname: string;
  studentNo: string;
  phone: string;
  avatarUrl: string;
  creditScore: number;
  creditLevel: string;
  carbonPoints: number;
};

type Review = {
  id: number;
  orderId: number;
  orderProductTitle: string;
  productImageUrl: string;
  fromUserId: number;
  fromUserNickname: string;
  toUserId: number;
  toUserNickname: string;
  communicationScore: number;
  matchScore: number;
  speedScore: number;
  avgScore: number;
  content: string;
  createdAt: string;
};

const profile = ref<Profile | null>(null);
const auth = useAuthStore();
const activeTab = ref("info");
const showEditDialog = ref(false);
const uploading = ref(false);
const editLoading = ref(false);

const editForm = reactive({
  nickname: "",
  phone: "",
  password: ""
});

const defaultAvatar = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI2U1ZTVmNSIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZD0iTSAzMCA3NSBBIDUgNSAwIDAgMCAzNSA2MEggNjVWNzBoLTE1TTcgNTBIODBBIDUgNSAwIDAgMCA3MyA1NUg3MCI+6/w+DRg9CkkIHg9IjUwIiB5PSI0NSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjZmZmIj7lm77kl4XhkJYmIGJpbyB1bml0czogbWluLXDDs24sIHNhbjogbGluZSwgd2VpZ2h0OiBub3JtYWwsIGNvbG9yOiAjZmZmIj7lm77kl4XhkJb9oImlkOiAyMikiPjwvdGV4dD48L3N2Zz4=";

const receivedReviews = ref<Review[]>([]);
const receivedTotal = ref(0);
const receivedPage = ref(1);

const givenReviews = ref<Review[]>([]);
const givenTotal = ref(0);
const givenPage = ref(1);

const pageSize = 10;

async function load() {
  const { data } = await http.get<ApiResponse<Profile>>("/user/me");
  if (data.code === 200) {
    profile.value = data.data;
    editForm.nickname = data.data.nickname || "";
    editForm.phone = data.data.phone || "";
    editForm.password = "";
  }
}

async function uploadAvatar(options: UploadRequestOptions) {
  const fd = new FormData();
  fd.append("file", options.file as File);
  uploading.value = true;
  try {
    const { data } = await http.post<ApiResponse<{ url: string }>>(
      "/files/upload",
      fd,
      { headers: { "Content-Type": "multipart/form-data" } }
    );
    if (data.code !== 200) {
      throw new Error(data.message || "上传失败");
    }
    const avatarUrl = data.data.url;
    await updateProfile({ avatarUrl });
    ElMessage.success("头像更新成功");
  } catch (e: any) {
    ElMessage.error(e?.message || "上传失败");
  } finally {
    uploading.value = false;
  }
}

async function updateProfile(updateData: { nickname?: string; phone?: string; password?: string; avatarUrl?: string }) {
  const requestData: any = {};
  if (updateData.nickname !== undefined) requestData.nickname = updateData.nickname;
  if (updateData.phone !== undefined) requestData.phone = updateData.phone;
  if (updateData.password !== undefined && updateData.password) requestData.password = updateData.password;
  if (updateData.avatarUrl !== undefined) requestData.avatarUrl = updateData.avatarUrl;

  const { data } = await http.put<ApiResponse<void>>("/user/me", requestData);
  if (data.code === 200) {
    await load();
    await auth.fetchUser();
  } else {
    throw new Error(data.message);
  }
}

async function submitEdit() {
  editLoading.value = true;
  try {
    await updateProfile({
      nickname: editForm.nickname,
      phone: editForm.phone,
      password: editForm.password
    });
    ElMessage.success("资料更新成功");
    showEditDialog.value = false;
  } catch (e: any) {
    ElMessage.error(e?.message || "更新失败");
  } finally {
    editLoading.value = false;
  }
}

async function loadReceivedReviews() {
  const { data } = await http.get<
    ApiResponse<{
      records: Review[];
      total: number;
    }>
  >("/orders/reviews/received", {
    params: { page: receivedPage.value, size: pageSize }
  });
  if (data.code === 200) {
    receivedReviews.value = data.data.records;
    receivedTotal.value = data.data.total;
  }
}

async function loadGivenReviews() {
  const { data } = await http.get<
    ApiResponse<{
      records: Review[];
      total: number;
    }>
  >("/orders/reviews/given", {
    params: { page: givenPage.value, size: pageSize }
  });
  if (data.code === 200) {
    givenReviews.value = data.data.records;
    givenTotal.value = data.data.total;
  }
}

function handleTabChange(tab: string) {
  if (tab === "received") {
    loadReceivedReviews();
  } else if (tab === "given") {
    loadGivenReviews();
  }
}

async function signIn() {
  const { data } = await http.post<ApiResponse<number>>("/user/sign-in");
  if (data.code !== 200) {
    ElMessage.error(data.message || "签到失败");
    return;
  }
  ElMessage.success(`签到成功，当前积分：${data.data}`);
  await load();
}

function formatTime(timeStr: string) {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  return date.toLocaleString("zh-CN");
}

onMounted(load);
</script>

<style scoped>
.profile-header {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-upload {
  display: flex;
  justify-content: center;
}

.profile-info {
  flex: 1;
}

.profile-info p {
  margin: 12px 0;
  font-size: 16px;
  line-height: 1.5;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e8e8e8;
}

.review-product {
  margin-bottom: 12px;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.reviewer {
  font-weight: 500;
  color: #303133;
}

.product {
  color: #606266;
}

.time {
  color: #909399;
  font-size: 12px;
}

.review-scores {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avg-score {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.review-content {
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.review-content p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>