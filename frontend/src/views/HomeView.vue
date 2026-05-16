<!--
首页视图 - 商品列表
负责成员B：商品交易 + 求购专区
-->
<template>
  <div>
    <!-- 第一行：分类和类型切换（全宽背景） -->
    <div class="toolbar-full-width">
      <div class="toolbar-inner">
        <el-row :gutter="16" class="toolbar">
          <!-- 左边：分类选择 -->
          <el-col :span="16">
            <el-button-group>
              <el-button
                style="border-radius: 6px;" 
                :type="categoryId === undefined ? 'primary' : 'default'"
                @click="
                  categoryId = undefined;
                  load();
                "
              >
                全部
              </el-button>
              <el-button
               style="border-radius: 6px;" 
                v-for="c in categories"
                :key="c.id"
                round
                :type="categoryId === c.id ? 'primary' : 'default'"
                @click="
                  categoryId = c.id;
                  load();
                "
              >
                {{ c.name }}
              </el-button>
            </el-button-group>
          </el-col>
          
          <!-- 右边：商品/求购切换 -->
          <el-col :span="8" class="text-right">
             <span class="split-line">|</span>
            <el-button-group>
              <el-button
                :type="activeTab === 'product' ? 'primary' : 'default'"
                @click="
                  activeTab = 'product';
                  load();
                "
              >
                商品
              </el-button>
              <el-button
                :type="activeTab === 'wanted' ? 'primary' : 'default'"
                @click="
                  activeTab = 'wanted';
                  load();
                "
              >
                求购
              </el-button>
            </el-button-group>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 第二行：结果数量和排序方式（全宽背景） -->
    <div class="sort-bar-full-width">
      <div class="sort-bar-inner">
        <el-row :gutter="16" class="sort-bar">
          <!-- 左边：结果数量 -->
          <el-col :span="12">
            <span class="text-gray">共找到 <strong class="text-primary">{{ total }}</strong> 个结果</span>
          </el-col>
          
          <!-- 右边：排序方式 -->
          <el-col :span="12" class="text-right">
            <el-button-group>
              <el-button
                :type="sort === 'latest' ? 'primary' : 'default'"
                @click="
                  sort = 'latest';
                  load();
                "
              >
                最新发布
              </el-button>
              <el-button
                :type="sort === 'hot' ? 'primary' : 'default'"
                @click="
                  sort = 'hot';
                  load();
                "
              >
                浏览最多
              </el-button>
              <el-button
                :type="sort.startsWith('price') ? 'primary' : 'default'"
                @click="
                  sort = sort === 'price_asc' ? 'price_desc' : 'price_asc';
                  load();
                "
              >
                价格
                <el-icon :class="{ 'rotate-180': sort === 'price_desc' }">
                  <ArrowUp />
                </el-icon>
              </el-button>
            </el-button-group>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 商品列表 -->
    <el-row :gutter="16" class="grid">
      <el-col v-for="p in items" :key="p.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card" @click="goDetail(p.id)">
          <div class="cover" :style="coverStyle(p.coverImage)" />
          <div class="title">{{ p.title }}</div>
          <div class="price">{{ priceText(p) }}</div>
          <div class="meta">
            <span>{{ publisherName(p) }}</span>
            <span>{{ p.viewCount ?? 0 }} 浏览</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分页 -->
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
import { useRouter, useRoute } from "vue-router";
import { http, type ApiResponse } from "../api/http";
import { ArrowUp } from "@element-plus/icons-vue";

type Category = { id: number; name: string };
type Item = {
  id: number;
  title: string;
  price?: number;
  budgetMin?: number | null;
  budgetMax?: number | null;
  sellerNickname?: string;
  userNickname?: string;
  viewCount?: number;
  coverImage?: string | null;
};

const router = useRouter();
const route = useRoute();
const categories = ref<Category[]>([]);
const items = ref<Item[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;
const keyword = ref("");
const categoryId = ref<number | undefined>();
const sort = ref<"latest" | "hot" | "price_asc" | "price_desc">("latest");
const activeTab = ref<"product" | "wanted">("product");

function coverStyle(url?: string | null) {
  if (!url) {
    return { background: "#e8f5e9" };
  }
  return {
    backgroundImage: `url(${url})`,
    backgroundSize: "cover",
    backgroundPosition: "center",
  };
}

function priceText(p: Item) {
  if (activeTab.value === "wanted") {
    if (p.budgetMin != null || p.budgetMax != null) {
      return `¥${p.budgetMin ?? "?"} - ¥${p.budgetMax ?? "?"}`;
    }
    return "面议";
  }
  return `¥${p.price ?? ""}`;
}

function publisherName(p: Item) {
  if (activeTab.value === "wanted") {
    return p.userNickname || "匿名";
  }
  return p.sellerNickname || "匿名";
}

async function load() {
  const url = activeTab.value === "product" ? "/products" : "/wanted";
  const { data } = await http.get<
    ApiResponse<{
      records: Item[];
      total: number;
      current: number;
      size: number;
    }>
  >(url, {
    params: {
      page: page.value,
      size: pageSize,
      keyword: keyword.value || route.query.keyword || undefined,
      categoryId: categoryId.value,
      sort: sort.value,
    },
  });
  if (data.code !== 200) return;
  items.value = data.data.records;
  total.value = data.data.total;
}

async function loadCategories() {
  const { data } = await http.get<ApiResponse<Category[]>>("/categories");
  if (data.code === 200) {
    categories.value = data.data;
  }
}

function goDetail(id: number) {
  const path = activeTab.value === "product" ? `/products/${id}` : `/wanted/${id}`;
  router.push(path);
}

onMounted(async () => {
  // 如果URL有keyword参数，使用搜索关键词
  if (route.query.keyword) {
    keyword.value = route.query.keyword as string;
  }
  await loadCategories();
  await load();
});
</script>

<style scoped>
.text-right {
  display: flex;
  justify-content: flex-end;
}
.toolbar-full-width {
  width: 100%;
  background-color: white;
  padding: 12px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
/* 竖线样式 */
.split-line {
  color: #ddd;
  font-size: 25px;
}
.toolbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}
.toolbar {
  margin-bottom: 0;
}
.sort-bar-full-width {
  width: 100%;
  background-color: #fafafa;
  padding: 12px 0;
  margin-bottom: 16px;
}
.sort-bar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}
.sort-bar {
  margin-bottom: 0;
}
.toolbar :deep(.el-button-group) {
  display: flex;
  gap: 8px;
}
.sort-bar :deep(.el-button-group) {
  display: flex;
  gap: 12px;
}
.text-gray {
  color: #909399;
}
.sort-bar .text-primary {
  color: #0f9d58 !important;
  font-weight: 700;
}
.grid {
  margin-top: 8px;
}
.card {
  cursor: pointer;
  margin-bottom: 16px;
}
.cover {
  height: 160px;
  border-radius: 8px;
  margin-bottom: 8px;
}
.title {
  font-weight: 600;
  margin-bottom: 6px;
}
.price {
  color: #0f9d58;
  font-size: 18px;
  font-weight: 700;
}
.meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
.pager {
  display: flex;
  justify-content: center;
  margin: 16px 0 32px;
}
.rotate-180 {
  transform: rotate(180deg);
  transition: transform 0.3s ease;
}
/* 自定义：选中后的按钮颜色（primary 按钮） */
:deep(.el-button--primary) {
  background-color: #0f9d58 !important;  
  border-color: #0f9d58 !important;      /* 边框色 */
  color: #fff !important;                /* 文字颜色 */
}

/* 鼠标 hover 效果 */
:deep(.el-button--primary:hover) {
  background-color: #0bb767 !important;
  border-color: #0bb767 !important;
}
/* 给分类按钮加间距，生效！ */
.toolbar :deep(.el-button-group) {
  display: flex;
  gap: 20px;   /* 按钮之间的距离，8px 最好看 */
  flex-wrap: wrap; /* 分类太多自动换行，不会挤出去 */
}
</style>
