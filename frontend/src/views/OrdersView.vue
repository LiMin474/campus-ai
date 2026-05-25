<template>
  <div class="orders-page">
    <div class="header-actions">
      <el-select v-model="status" clearable placeholder="订单状态" style="width: 200px">
        <el-option label="全部" value="" />
        <el-option label="待确认" value="PENDING_CONFIRM" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-button style="margin-left: 12px" @click="load">刷新</el-button>
    </div>

    <el-tabs v-model="role" @tab-change="load" class="tab-container">
      <el-tab-pane label="我是买家" name="buyer">
        <div v-if="buyerRows.length === 0" class="empty-tip">
          暂无买家订单
        </div>
        <div v-else class="order-list">
          <div v-for="row in buyerRows" :key="row.id" class="order-card">
            <div class="order-header">
              <span class="order-id">订单号: {{ row.id }}</span>
              <span class="order-time">{{ formatTime(row.createdAt) }}</span>
            </div>
            
            <div class="order-content">
              <div class="product-info" @click="goToProduct(row.productId)">
                <div class="product-image-wrapper">
                  <img 
                    v-if="row.productImage" 
                    :src="getImageUrl(row.productImage)" 
                    :alt="row.productTitle"
                    class="product-image"
                  />
                  <div v-else class="product-image-placeholder">
                    <el-icon size="48"><Picture /></el-icon>
                  </div>
                  <span class="product-type-tag">{{ row.productType === 'product' ? '商品' : '求购' }}</span>
                </div>
                <div class="product-details">
                  <h3 class="product-title">{{ row.productTitle }}</h3>
                  <p class="price">¥{{ row.finalPrice }}</p>
                  <p class="counterparty">
                    <span>卖家:</span>
                    <span>{{ row.sellerNickname }}</span>
                  </p>
                </div>
              </div>
              
              <div class="order-status">
                <el-tag :type="getStatusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
              </div>
              
              <div class="order-actions">
                <el-space wrap>
                  <el-button v-if="row.status === 'PENDING_CONFIRM'" size="small" type="primary" @click="confirm(row.id)">
                    确认收货
                  </el-button>
                  <el-button v-if="row.status === 'PENDING_CONFIRM'" size="small" @click="cancel(row.id)">取消订单</el-button>
                  <el-button v-if="row.status === 'COMPLETED'" size="small" @click="openReview(row)">
                    评价
                  </el-button>
                </el-space>
              </div>
            </div>
          </div>
        </div>
        <div v-if="buyerRows.length > 0" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="buyerTotal"
            :page-size="pageSize"
            v-model:current-page="buyerPage"
            @current-change="loadBuyerOrders"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我是卖家" name="seller">
        <div v-if="sellerRows.length === 0" class="empty-tip">
          暂无卖家订单
        </div>
        <div v-else class="order-list">
          <div v-for="row in sellerRows" :key="row.id" class="order-card">
            <div class="order-header">
              <span class="order-id">订单号: {{ row.id }}</span>
              <span class="order-time">{{ formatTime(row.createdAt) }}</span>
            </div>
            
            <div class="order-content">
              <div class="product-info" @click="goToProduct(row.productId)">
                <div class="product-image-wrapper">
                  <img 
                    v-if="row.productImage" 
                    :src="getImageUrl(row.productImage)" 
                    :alt="row.productTitle"
                    class="product-image"
                  />
                  <div v-else class="product-image-placeholder">
                    <el-icon size="48"><Picture /></el-icon>
                  </div>
                  <span class="product-type-tag">{{ row.productType === 'product' ? '商品' : '求购' }}</span>
                </div>
                <div class="product-details">
                  <h3 class="product-title">{{ row.productTitle }}</h3>
                  <p class="price">¥{{ row.finalPrice }}</p>
                  <p class="counterparty">
                    <span>买家:</span>
                    <span>{{ row.buyerNickname }}</span>
                  </p>
                </div>
              </div>
              
              <div class="order-status">
                <el-tag :type="getStatusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
              </div>
              
              <div class="order-actions">
                <el-space wrap>
                  <el-button v-if="row.status === 'PENDING_CONFIRM'" size="small" @click="openQr(row)">收货确认码</el-button>
                  <el-button v-if="row.status === 'COMPLETED'" size="small" @click="openReview(row)">
                    评价
                  </el-button>
                </el-space>
              </div>
            </div>
          </div>
        </div>
        <div v-if="sellerRows.length > 0" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="sellerTotal"
            :page-size="pageSize"
            v-model:current-page="sellerPage"
            @current-change="loadSellerOrders"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="qrVisible" title="买家扫码确认收货" width="420px">
      <p class="hint">请买家使用手机相机扫描下方二维码，登录后在页面确认收货。</p>
      <div v-if="qrDataUrl" class="qr">
        <img :src="qrDataUrl" alt="QR" />
      </div>
      <p v-if="confirmUrl" class="link">{{ confirmUrl }}</p>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="提交评价" width="520px">
      <el-form :model="reviewForm" label-width="120px">
        <el-form-item label="沟通态度">
          <el-rate v-model="reviewForm.communicationScore" />
        </el-form-item>
        <el-form-item label="描述相符">
          <el-rate v-model="reviewForm.matchScore" />
        </el-form-item>
        <el-form-item label="交易速度">
          <el-rate v-model="reviewForm.speedScore" />
        </el-form-item>
        <el-form-item label="文字评价">
          <el-input v-model="reviewForm.content" type="textarea" rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Picture } from "@element-plus/icons-vue";
import QRCode from "qrcode";
import { http, type ApiResponse } from "../api/http";

type OrderRow = {
  id: number;
  productId: number;
  productTitle: string;
  productImage: string;
  productType: string;
  buyerId: number;
  sellerId: number;
  buyerNickname: string;
  sellerNickname: string;
  status: string;
  finalPrice: number;
  createdAt: string;
};

const router = useRouter();
const role = ref<"buyer" | "seller">("buyer");
const status = ref<string>();
const pageSize = 10;

// 买家订单
const buyerRows = ref<OrderRow[]>([]);
const buyerTotal = ref(0);
const buyerPage = ref(1);

// 卖家订单
const sellerRows = ref<OrderRow[]>([]);
const sellerTotal = ref(0);
const sellerPage = ref(1);

const qrVisible = ref(false);
const qrDataUrl = ref("");
const confirmUrl = ref("");

const reviewVisible = ref(false);
const reviewForm = reactive({
  orderId: 0,
  toUserId: 0,
  communicationScore: 5,
  matchScore: 5,
  speedScore: 5,
  content: ""
});

function statusText(s: string) {
  if (s === "PENDING_CONFIRM") return "待确认";
  if (s === "COMPLETED") return "已完成";
  if (s === "CANCELLED") return "已取消";
  return s;
}

function getStatusTagType(s: string) {
  if (s === "PENDING_CONFIRM") return "warning";
  if (s === "COMPLETED") return "success";
  if (s === "CANCELLED") return "info";
  return "default";
}

function formatTime(timeStr: string) {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  return date.toLocaleString("zh-CN");
}

function getImageUrl(url: string) {
  if (!url) return "";
  return url;
}

function goToProduct(productId: number) {
  router.push(`/products/${productId}`);
}

function load() {
  if (role.value === 'buyer') {
    loadBuyerOrders();
  } else {
    loadSellerOrders();
  }
}

async function loadBuyerOrders() {
  const { data } = await http.get<
    ApiResponse<{
      records: OrderRow[];
      total: number;
    }>
  >("/orders", {
    params: { role: 'buyer', status: status.value, page: buyerPage.value, size: pageSize }
  });
  if (data.code !== 200) return;
  buyerRows.value = data.data.records;
  buyerTotal.value = data.data.total;
}

async function loadSellerOrders() {
  const { data } = await http.get<
    ApiResponse<{
      records: OrderRow[];
      total: number;
    }>
  >("/orders", {
    params: { role: 'seller', status: status.value, page: sellerPage.value, size: pageSize }
  });
  if (data.code !== 200) return;
  sellerRows.value = data.data.records;
  sellerTotal.value = data.data.total;
}

async function confirm(id: number) {
  try {
    await ElMessageBox.confirm("确认已收到货物？确认后将发放减碳积分并不可撤销。", "确认收货", { type: "warning" });
  } catch {
    return;
  }
  const { data } = await http.post<ApiResponse<null>>(`/orders/${id}/confirm`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "操作失败");
    return;
  }
  ElMessage.success("已确认收货");
  await load();
}

async function cancel(id: number) {
  try {
    await ElMessageBox.confirm("确定取消该订单？", "取消订单", { type: "warning" });
  } catch {
    return;
  }
  const { data } = await http.post<ApiResponse<null>>(`/orders/${id}/cancel`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "取消失败");
    return;
  }
  ElMessage.success("订单已取消");
  await load();
}

async function openQr(row: OrderRow) {
  const { data } = await http.post<ApiResponse<{ confirmUrl: string }>>(`/orders/${row.id}/confirm-token`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "生成失败");
    return;
  }
  confirmUrl.value = data.data.confirmUrl;
  qrDataUrl.value = await QRCode.toDataURL(data.data.confirmUrl, { width: 280, margin: 1 });
  qrVisible.value = true;
}

function openReview(row: OrderRow) {
  reviewForm.orderId = row.id;
  reviewForm.toUserId = role.value === "buyer" ? row.sellerId : row.buyerId;
  reviewForm.communicationScore = 5;
  reviewForm.matchScore = 5;
  reviewForm.speedScore = 5;
  reviewForm.content = "";
  reviewVisible.value = true;
}

async function submitReview() {
  const { data } = await http.post<ApiResponse<null>>("/orders/reviews", {
    orderId: reviewForm.orderId,
    toUserId: reviewForm.toUserId,
    communicationScore: reviewForm.communicationScore,
    matchScore: reviewForm.matchScore,
    speedScore: reviewForm.speedScore,
    content: reviewForm.content || undefined
  });
  if (data.code !== 200) {
    ElMessage.error(data.message || "提交失败");
    return;
  }
  ElMessage.success("评价已提交");
  reviewVisible.value = false;
  await load();
}

onMounted(load);
</script>

<style scoped>
.orders-page {
  max-width: 100%;
  padding: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.tab-container {
  background: white;
  border-radius: 8px;
  padding: 16px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.order-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.order-id {
  font-size: 14px;
  color: #606266;
}

.order-time {
  font-size: 12px;
  color: #909399;
}

.order-content {
  padding: 16px;
}

.product-info {
  display: flex;
  gap: 16px;
  cursor: pointer;
}

.product-image-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.product-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 8px;
  color: #ccc;
}

.product-type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 4px;
}

.product-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  font-size: 20px;
  font-weight: 600;
  color: #e64340;
  margin: 4px 0;
}

.counterparty {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.counterparty span:first-child {
  color: #909399;
  margin-right: 8px;
}

.order-status {
  margin-top: 12px;
}

.order-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e8e8e8;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.qr {
  display: flex;
  justify-content: center;
  margin: 12px 0;
}

.hint {
  color: #606266;
  line-height: 1.5;
}

.link {
  font-size: 12px;
  color: #909399;
  word-break: break-all;
}
</style>