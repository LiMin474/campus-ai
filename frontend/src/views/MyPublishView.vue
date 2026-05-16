<template>
  <div>
    <el-radio-group v-model="role" @change="load">
      <el-radio-button value="seller">我是卖家</el-radio-button>
      <el-radio-button value="buyer">我是买家</el-radio-button>
    </el-radio-group>

    <div v-if="role === 'seller'" style="margin-top: 16px">
      <el-table :data="productRows" style="width: 100%">
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <img v-if="row.coverImage" :src="row.coverImage" class="thumb" />
            <span v-else class="no-image">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ productStatusText(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="likeCount" label="喜欢" width="80" />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-space wrap>
              <el-button size="small" @click="openProduct(row.id)">查看</el-button>
              <el-button
                v-if="row.status === 'ON_SHELF'"
                size="small"
                type="danger"
                @click="offShelfProduct(row.id)"
              >
                下架
              </el-button>
              <el-button
                v-if="row.status === 'OFF_SHELF'"
                size="small"
                type="primary"
                @click="onShelfProduct(row.id)"
              >
                上架
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="productTotal"
          :page-size="pageSize"
          v-model:current-page="productPage"
          @current-change="loadProducts"
        />
      </div>
    </div>

    <div v-else style="margin-top: 16px">
      <el-table :data="wantedRows" style="width: 100%">
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <img v-if="row.imageUrls?.length" :src="row.imageUrls[0]" class="thumb" />
            <span v-else class="no-image">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="预算" width="120">
          <template #default="{ row }">
            ¥{{ row.budgetMin || '-' }} - ¥{{ row.budgetMax || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ wantedStatusText(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-space wrap>
              <el-button size="small" @click="openWanted(row.id)">查看</el-button>
              <el-button
                v-if="row.status === 'OPEN'"
                size="small"
                type="danger"
                @click="closeWanted(row.id)"
              >
                关闭
              </el-button>
              <el-button
                v-if="row.status === 'CLOSED'"
                size="small"
                type="primary"
                @click="reopenWanted(row.id)"
              >
                重新开启
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="wantedTotal"
          :page-size="pageSize"
          v-model:current-page="wantedPage"
          @current-change="loadWanted"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { http, type ApiResponse } from "../api/http";

const router = useRouter();
const role = ref<"seller" | "buyer">("seller");
const pageSize = 10;

type ProductRow = {
  id: number;
  title: string;
  price: number;
  status: string;
  viewCount: number;
  likeCount: number;
  createdAt: string;
  coverImage?: string;
};

type WantedRow = {
  id: number;
  title: string;
  budgetMin?: number | null;
  budgetMax?: number | null;
  status: string;
  createdAt: string;
  imageUrls: string[];
};

const productRows = ref<ProductRow[]>([]);
const productTotal = ref(0);
const productPage = ref(1);

const wantedRows = ref<WantedRow[]>([]);
const wantedTotal = ref(0);
const wantedPage = ref(1);

function productStatusText(s: string) {
  if (s === "ON_SHELF") return "上架中";
  if (s === "OFF_SHELF") return "已下架";
  return s;
}

function wantedStatusText(s: string) {
  if (s === "OPEN") return "进行中";
  if (s === "CLOSED") return "已关闭";
  return s;
}

async function load() {
  if (role.value === "seller") {
    await loadProducts();
  } else {
    await loadWanted();
  }
}

async function loadProducts() {
  const { data } = await http.get<
    ApiResponse<{
      records: ProductRow[];
      total: number;
    }>
  >("/products/me", {
    params: { page: productPage.value, size: pageSize }
  });
  if (data.code !== 200) return;
  productRows.value = data.data.records;
  productTotal.value = data.data.total;
}

async function loadWanted() {
  const { data } = await http.get<
    ApiResponse<{
      records: WantedRow[];
      total: number;
    }>
  >("/wanted/me", {
    params: { page: wantedPage.value, size: pageSize }
  });
  if (data.code !== 200) return;
  wantedRows.value = data.data.records;
  wantedTotal.value = data.data.total;
}

async function offShelfProduct(id: number) {
  try {
    await ElMessageBox.confirm("确定下架该商品？", "下架商品", { type: "warning" });
  } catch {
    return;
  }
  const { data } = await http.post<ApiResponse<void>>(`/products/${id}/off-shelf`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "下架失败");
    return;
  }
  ElMessage.success("已下架");
  await loadProducts();
}

async function onShelfProduct(id: number) {
  const { data } = await http.post<ApiResponse<void>>(`/products/${id}/on-shelf`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "上架失败");
    return;
  }
  ElMessage.success("已上架");
  await loadProducts();
}

async function closeWanted(id: number) {
  try {
    await ElMessageBox.confirm("确定关闭该求购？", "关闭求购", { type: "warning" });
  } catch {
    return;
  }
  const { data } = await http.post<ApiResponse<void>>(`/wanted/${id}/close`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "关闭失败");
    return;
  }
  ElMessage.success("已关闭");
  await loadWanted();
}

async function reopenWanted(id: number) {
  const { data } = await http.post<ApiResponse<void>>(`/wanted/${id}/reopen`);
  if (data.code !== 200) {
    ElMessage.error(data.message || "开启失败");
    return;
  }
  ElMessage.success("已开启");
  await loadWanted();
}

function openProduct(id: number) {
  router.push(`/products/${id}`);
}

function openWanted(id: number) {
  router.push(`/wanted/${id}`);
}

onMounted(load);
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}
.no-image {
  color: #909399;
  font-size: 12px;
}
</style>