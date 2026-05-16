<!--
管理员仪表盘视图
负责成员E：管理员管理模块
-->
<template>
  <div class="admin-dashboard">
    <el-card shadow="hover" class="dashboard-card">
      <template #header>
        <div class="card-header">
          <span>管理控制台</span>
          <el-button type="primary" @click="refreshData">
            <el-icon><Refresh /></el-icon> 刷新数据
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <div class="stats-grid">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon user-icon"><el-icon><User /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.userCount || 0 }}</div>
            <div class="stat-label">用户总数</div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon product-icon"><el-icon><Goods /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.productCount || 0 }}</div>
            <div class="stat-label">商品总数</div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon order-icon"><el-icon><ShoppingCart /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.orderCount || 0 }}</div>
            <div class="stat-label">订单总数</div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon sales-icon"><el-icon><Money /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">¥{{ (dashboardData.totalSales || 0).toFixed(2) }}</div>
            <div class="stat-label">总销售额</div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon post-icon"><el-icon><Document /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.postCount || 0 }}</div>
            <div class="stat-label">社区帖子</div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon report-icon"><el-icon><Warning /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.pendingReportCount || 0 }}</div>
            <div class="stat-label">待处理举报</div>
          </div>
        </el-card>
      </div>

      <!-- 图表区域 -->
      <div class="charts-grid">
        <!-- 每日统计图表 -->
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>最近7天统计</span>
          </template>
          <div class="chart-container">
            <canvas ref="dailyChartRef"></canvas>
          </div>
        </el-card>

        <!-- 分类统计图表 -->
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>分类统计</span>
          </template>
          <div class="chart-container">
            <canvas ref="categoryChartRef"></canvas>
          </div>
        </el-card>

        <!-- 订单状态图表 -->
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>订单状态</span>
          </template>
          <div class="chart-container">
            <canvas ref="orderStatusChartRef"></canvas>
          </div>
        </el-card>

        <!-- 商品状态图表 -->
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>商品状态</span>
          </template>
          <div class="chart-container">
            <canvas ref="productStatusChartRef"></canvas>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh, User, Goods, ShoppingCart, Money, Document, Warning } from '@element-plus/icons-vue';
import Chart from 'chart.js/auto';
import { http } from '../../api/http';

interface DailyStat {
  date: string;
  userCount: number;
  productCount: number;
  orderCount: number;
  salesAmount: number;
}

interface CategoryStat {
  categoryName: string;
  productCount: number;
  salesAmount: number;
}

interface StatusStat {
  status: string;
  count: number;
}

interface DashboardData {
  userCount: number;
  productCount: number;
  pendingReportCount: number;
  orderCount: number;
  postCount: number;
  totalSales: number;
  dailyStats: DailyStat[];
  categoryStats: CategoryStat[];
  orderStatusStats: StatusStat[];
  productStatusStats: StatusStat[];
}

const dashboardData = ref<DashboardData>({
  userCount: 0,
  productCount: 0,
  pendingReportCount: 0,
  orderCount: 0,
  postCount: 0,
  totalSales: 0,
  dailyStats: [],
  categoryStats: [],
  orderStatusStats: [],
  productStatusStats: []
});

const dailyChartRef = ref<HTMLCanvasElement>();
const categoryChartRef = ref<HTMLCanvasElement>();
const orderStatusChartRef = ref<HTMLCanvasElement>();
const productStatusChartRef = ref<HTMLCanvasElement>();

let dailyChart: Chart | null = null;
let categoryChart: Chart | null = null;
let orderStatusChart: Chart | null = null;
let productStatusChart: Chart | null = null;

const loadDashboardData = async () => {
  try {
    const response = await http.get('/api/admin/dashboard');
    dashboardData.value = response.data;
    renderCharts();
  } catch (error) {
    ElMessage.error('加载数据失败');
    console.error('Failed to load dashboard data:', error);
  }
};

const refreshData = () => {
  loadDashboardData();
};

const renderCharts = () => {
  renderDailyChart();
  renderCategoryChart();
  renderOrderStatusChart();
  renderProductStatusChart();
};

const renderDailyChart = () => {
  if (!dailyChartRef.value) return;

  const ctx = dailyChartRef.value.getContext('2d');
  if (!ctx) return;

  if (dailyChart) {
    dailyChart.destroy();
  }

  const labels = dashboardData.value.dailyStats.map(stat => stat.date);
  const userData = dashboardData.value.dailyStats.map(stat => stat.userCount);
  const productData = dashboardData.value.dailyStats.map(stat => stat.productCount);
  const orderData = dashboardData.value.dailyStats.map(stat => stat.orderCount);

  dailyChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '用户注册',
          data: userData,
          borderColor: '#409EFF',
          backgroundColor: 'rgba(64, 158, 255, 0.1)',
          tension: 0.4
        },
        {
          label: '商品发布',
          data: productData,
          borderColor: '#67C23A',
          backgroundColor: 'rgba(103, 194, 58, 0.1)',
          tension: 0.4
        },
        {
          label: '订单创建',
          data: orderData,
          borderColor: '#E6A23C',
          backgroundColor: 'rgba(230, 162, 60, 0.1)',
          tension: 0.4
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'top' as const
        }
      },
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  });
};

const renderCategoryChart = () => {
  if (!categoryChartRef.value) return;

  const ctx = categoryChartRef.value.getContext('2d');
  if (!ctx) return;

  if (categoryChart) {
    categoryChart.destroy();
  }

  const labels = dashboardData.value.categoryStats.map(stat => stat.categoryName);
  const data = dashboardData.value.categoryStats.map(stat => stat.productCount);

  categoryChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: '商品数量',
        data,
        backgroundColor: [
          '#409EFF',
          '#67C23A',
          '#E6A23C',
          '#F56C6C',
          '#909399',
          '#722ED1'
        ]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        }
      },
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  });
};

const renderOrderStatusChart = () => {
  if (!orderStatusChartRef.value) return;

  const ctx = orderStatusChartRef.value.getContext('2d');
  if (!ctx) return;

  if (orderStatusChart) {
    orderStatusChart.destroy();
  }

  const labels = dashboardData.value.orderStatusStats.map(stat => stat.status);
  const data = dashboardData.value.orderStatusStats.map(stat => stat.count);

  orderStatusChart = new Chart(ctx, {
    type: 'pie',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: [
          '#409EFF',
          '#67C23A',
          '#E6A23C',
          '#F56C6C',
          '#909399'
        ]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right' as const
        }
      }
    }
  });
};

const renderProductStatusChart = () => {
  if (!productStatusChartRef.value) return;

  const ctx = productStatusChartRef.value.getContext('2d');
  if (!ctx) return;

  if (productStatusChart) {
    productStatusChart.destroy();
  }

  const labels = dashboardData.value.productStatusStats.map(stat => stat.status);
  const data = dashboardData.value.productStatusStats.map(stat => stat.count);

  productStatusChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: [
          '#67C23A',
          '#F56C6C',
          '#E6A23C',
          '#909399'
        ]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right' as const
        }
      }
    }
  });
};

onMounted(() => {
  loadDashboardData();
});

onUnmounted(() => {
  if (dailyChart) dailyChart.destroy();
  if (categoryChart) categoryChart.destroy();
  if (orderStatusChart) orderStatusChart.destroy();
  if (productStatusChart) productStatusChart.destroy();
});
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.dashboard-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 24px;
}

.user-icon {
  background-color: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

.product-icon {
  background-color: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.order-icon {
  background-color: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.sales-icon {
  background-color: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.post-icon {
  background-color: rgba(144, 147, 153, 0.1);
  color: #909399;
}

.report-icon {
  background-color: rgba(245, 108, 108, 0.1);
  color: #F56C6C;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 20px;
}

.chart-card {
  height: 400px;
}

.chart-container {
  height: 350px;
  width: 100%;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .charts-grid {
    grid-template-columns: 1fr;
  }
  
  .chart-card {
    height: 300px;
  }
  
  .chart-container {
    height: 250px;
  }
}
</style>