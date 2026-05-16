<template>
  <div>
    <el-button type="primary" @click="$router.push('/publish')">发布新商品</el-button>
    <el-button @click="load">刷新</el-button>
    <el-table :data="rows" style="width: 100%; margin-top: 12px">
      <el-table-column prop="id" label="#" width="70" />
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/products/${row.id}`)">查看</el-button>
          <el-button
            v-if="row.status === 'ON_SHELF' || row.status === 'LOCKED'"
            size="small"
            type="danger"
            plain
            @click="off(row.id)"
          >
            下架
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="page"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { http, type ApiResponse } from "../api/http";

type Row = {
  id: number;
  title: string;
  price: number;
  status: string;
  viewCount: number;
};

const rows = ref<Row[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;

async function load() {
  const { data } = await http.get<
    ApiResponse<{
      records: Row[];
      total: number;
    }>
  >("/products", { params: { mine: true, page: page.value, size: pageSize, sort: "latest" } });
  if (data.code !== 200) return;
  rows.value = data.data.records;
  total.value = data.data.total;
}

async function off(id: number) {
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
  ElMessage.success("已下架");
  await load();
}

onMounted(load);
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
