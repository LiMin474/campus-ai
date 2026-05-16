<!--
求购列表视图
负责成员B：商品交易 + 求购专区
-->
<template>
  <div>
    <el-button type="primary" @click="load">刷新</el-button>
    <el-table :data="rows" style="width: 100%; margin-top: 12px" @row-click="open">
      <el-table-column prop="title" label="标题" />
      <el-table-column label="预算" width="200">
        <template #default="{ row }">
          <span v-if="row.budgetMin != null || row.budgetMax != null">
            ¥{{ row.budgetMin ?? "?" }} - ¥{{ row.budgetMax ?? "?" }}
          </span>
          <span v-else>面议</span>
        </template>
      </el-table-column>
      <el-table-column prop="userNickname" label="发布者" width="140" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="createdAt" label="时间" width="200" />
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
import { useRouter } from "vue-router";
import { http, type ApiResponse } from "../api/http";

type Row = {
  id: number;
  title: string;
  budgetMin?: number | null;
  budgetMax?: number | null;
  userNickname: string;
  categoryName: string;
  createdAt: string;
};

const router = useRouter();
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
  >("/wanted", { params: { page: page.value, size: pageSize } });
  if (data.code !== 200) return;
  rows.value = data.data.records;
  total.value = data.data.total;
}

function open(row: Row) {
  router.push(`/wanted/${row.id}`);
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
