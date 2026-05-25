<template>
  <div class="my-publish">
    <div class="header-actions">
      <el-button type="primary" @click="$router.push('/publish')">发布新内容</el-button>
      <el-select v-model="productStatus" clearable placeholder="商品状态" style="width: 150px; margin-left: 12px">
        <el-option label="全部" value="" />
        <el-option label="在售" value="ON_SHELF" />
        <el-option label="已下架" value="OFF_SHELF" />
        <el-option label="已锁定" value="LOCKED" />
      </el-select>
      <el-select v-model="wantedStatus" clearable placeholder="求购状态" style="width: 150px; margin-left: 12px">
        <el-option label="全部" value="" />
        <el-option label="进行中" value="OPEN" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button style="margin-left: 12px" @click="load">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="load" class="tab-container">
      <el-tab-pane label="我的商品" name="products">
        <div v-if="productRows.length === 0" class="empty-tip">
          暂无商品，点击上方按钮发布新商品
        </div>
        <div v-else class="publish-list">
          <div v-for="row in productRows" :key="row.id" class="publish-card">
            <div class="product-info" @click="goToProduct(row.id, 'product')">
              <div class="product-image-wrapper">
                <img 
                  v-if="row.coverImage" 
                  :src="getImageUrl(row.coverImage)" 
                  :alt="row.title"
                  class="product-image"
                />
                <div v-else class="product-image-placeholder">
                  <el-icon size="48"><Picture /></el-icon>
                </div>
                <span class="product-type-tag">商品</span>
              </div>
              <div class="product-details">
                <h3 class="product-title">{{ row.title }}</h3>
                <p class="price">¥{{ row.price }}</p>
                <p class="view-count">浏览: {{ row.viewCount }}</p>
              </div>
            </div>
            <div class="publish-status">
              <el-tag :type="getStatusTagType(row.status)">{{ getProductStatusText(row.status) }}</el-tag>
            </div>
            <div class="publish-actions">
              <el-space wrap>
                <el-button size="small" @click="goToProduct(row.id, 'product')">查看</el-button>
                <el-button
                  v-if="row.status === 'ON_SHELF' || row.status === 'LOCKED'"
                  size="small"
                  type="danger"
                  plain
                  @click="offProduct(row.id)"
                >
                  下架
                </el-button>
              </el-space>
            </div>
          </div>
        </div>
        <div v-if="productRows.length > 0" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="productTotal"
            :page-size="pageSize"
            v-model:current-page="productPage"
            @current-change="loadProducts"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的求购" name="wanted">
        <div v-if="wantedRows.length === 0" class="empty-tip">
          暂无求购，点击上方按钮发布新求购
        </div>
        <div v-else class="publish-list">
          <div v-for="row in wantedRows" :key="row.id" class="publish-card">
            <div class="product-info" @click="goToProduct(row.id, 'wanted')">
              <div class="product-image-wrapper">
                <img 
                  v-if="row.coverImage" 
                  :src="getImageUrl(row.coverImage)" 
                  :alt="row.title"
                  class="product-image"
                />
                <div v-else class="product-image-placeholder">
                  <el-icon size="48"><Picture /></el-icon>
                </div>
                <span class="product-type-tag">求购</span>
              </div>
              <div class="product-details">
                <h3 class="product-title">{{ row.title }}</h3>
                <p class="price">预算: ¥{{ row.budgetMin || 0 }} - ¥{{ row.budgetMax || '不限' }}</p>
              </div>
            </div>
            <div class="publish-status">
              <el-tag :type="getStatusTagType(row.status)">{{ getWantedStatusText(row.status) }}</el-tag>
            </div>
            <div class="publish-actions">
              <el-space wrap>
                <el-button size="small" @click="goToProduct(row.id, 'wanted')">查看</el-button>
                <el-button
                  v-if="row.status === 'OPEN'"
                  size="small"
                  type="danger"
                  plain
                  @click="closeWanted(row.id)"
                >
                  关闭求购
                </el-button>
              </el-space>
            </div>
          </div>
        </div>
        <div v-if="wantedRows.length > 0" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="wantedTotal"
            :page-size="pageSize"
            v-model:current-page="wantedPage"
            @current-change="loadWanted"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Picture } from "@element-plus/icons-vue";
import { http, type ApiResponse } from "../api/http";

const router = useRouter();

type ProductRow = {
  id: number;
  title: string;
  price: number;
  status: string;
  viewCount: number;
  coverImage: string;
};

type WantedRow = {
  id: number;
  title: string;
  budgetMin: number;
  budgetMax: number;
  status: string;
  coverImage: string;
};

function getImageUrl(url: string) {
  if (!url) return "";
  return url;
}

function goToProduct(productId: number, type: string) {
  if (type === 'product') {
    router.push(`/products/${productId}`);
  } else {
    router.push(`/wanted/${productId}`);
  }
}

const activeTab = ref("products");
const pageSize = 10;

// 状态筛选
const productStatus = ref<string>();
const wantedStatus = ref<string>();

// 商品数据
const productRows = ref<ProductRow[]>([]);
const productTotal = ref(0);
const productPage = ref(1);

// 求购数据
const wantedRows = ref<WantedRow[]>([]);
const wantedTotal = ref(0);
const wantedPage = ref(1);

function getStatusTagType(status: string) {
  if (status === "ON_SHELF" || status === "OPEN") return "success";
  if (status === "LOCKED" || status === "PENDING_CONFIRM") return "warning";
  if (status === "OFF_SHELF" || status === "CLOSED") return "info";
  if (status === "SOLD") return "danger";
  return "default";
}

function getProductStatusText(status: string) {
  if (status === "ON_SHELF") return "在售";
  if (status === "LOCKED") return "已锁定";
  if (status === "SOLD") return "已售出";
  if (status === "OFF_SHELF") return "已下架";
  return status;
}

function getWantedStatusText(status: string) {
  if (status === "OPEN") return "进行中";
  if (status === "CLOSED") return "已关闭";
  return status;
}

function load() {
  if (activeTab.value === "products") {
    loadProducts();
  } else {
    loadWanted();
  }
}

async function loadProducts() {
  const params: Record<string, any> = { 
    mine: true, 
    page: productPage.value, 
    size: pageSize, 
    sort: "latest" 
  };
  if (productStatus.value) {
    params.status = productStatus.value;
  }
  const { data } = await http.get<
    ApiResponse<{
      records: ProductRow[];
      total: number;
    }>
  >("/products", { params });
  if (data.code !== 200) return;
  productRows.value = data.data.records;
  productTotal.value = data.data.total;
}

async function loadWanted() {
  const params: Record<string, any> = { 
    mine: true, 
    page: wantedPage.value, 
    size: pageSize 
  };
  if (wantedStatus.value) {
    params.status = wantedStatus.value;
  }
  const { data } = await http.get<
    ApiResponse<{
      records: WantedRow[];
      total: number;
    }>
  >("/wanted", { params });
  if (data.code !== 200) return;
  wantedRows.value = data.data.records;
  wantedTotal.value = data.data.total;
}

async function offProduct(id: number) {
  try {
    await ElMessageBox.confirm("确定下架该商品？", "下架", { type: "warning" });
  } catch {
    return;
  }
  const { data } = await http.post<ApiResponse<null>>(`/products/${id}/off-shelf`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "下架失败");
    return;
  }
  ElMessage.success("下架成功");
  loadProducts();
}

async function closeWanted(id: number) {
  try {
    await ElMessageBox.confirm("确定关闭该求购？", "关闭求购", { type: "warning" });
  } catch {
    return;
  }
  const { data } = await http.post<ApiResponse<null>>(`/wanted/${id}/close`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "关闭失败");
    return;
  }
  ElMessage.success("已关闭");
  loadWanted();
}

onMounted(load);
</script>

<style scoped>
.my-publish {
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

.publish-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.publish-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.product-info {
  display: flex;
  gap: 16px;
  cursor: pointer;
}

.product-image-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
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
  border-radius: 8px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.product-type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
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
  font-weight: 600;
  color: #303133;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
  margin: 4px 0;
}

.view-count {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.publish-status {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.publish-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>