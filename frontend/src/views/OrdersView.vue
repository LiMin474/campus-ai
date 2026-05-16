<!--
订单评价视图
负责成员A：用户中心 + 订单评价
-->
<template>
  <div>
    <el-radio-group v-model="role" @change="load">
      <el-radio-button label="buyer">我是买家</el-radio-button>
      <el-radio-button label="seller">我是卖家</el-radio-button>
    </el-radio-group>
    <el-select v-model="status" clearable placeholder="订单状态" style="width: 200px; margin-left: 12px" @change="load">
      <el-option label="待确认" value="PENDING_CONFIRM" />
      <el-option label="已完成" value="COMPLETED" />
      <el-option label="已取消" value="CANCELLED" />
    </el-select>
    <el-button style="margin-left: 12px" @click="load">刷新</el-button>

    <el-table :data="rows" style="width: 100%; margin-top: 16px">
      <el-table-column prop="id" label="#" width="70" />
      <el-table-column prop="productTitle" label="商品" min-width="160" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ row.finalPrice }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column :label="role === 'buyer' ? '卖家' : '买家'" width="120">
        <template #default="{ row }">
          {{ role === "buyer" ? row.sellerNickname : row.buyerNickname }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-space wrap>
            <template v-if="role === 'buyer'">
              <el-button v-if="row.status === 'PENDING_CONFIRM'" size="small" type="primary" @click="confirm(row.id)">
                确认收货
              </el-button>
              <el-button v-if="row.status === 'PENDING_CONFIRM'" size="small" @click="cancel(row.id)">取消订单</el-button>
            </template>
            <template v-if="role === 'seller'">
              <el-button v-if="row.status === 'PENDING_CONFIRM'" size="small" @click="openQr(row)">收货确认码</el-button>
            </template>
            <el-button
              v-if="row.status === 'COMPLETED'"
              size="small"
              @click="openReview(row)"
            >
              评价
            </el-button>
          </el-space>
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
import { ElMessage, ElMessageBox } from "element-plus";
import QRCode from "qrcode";
import { http, type ApiResponse } from "../api/http";

type OrderRow = {
  id: number;
  productId: number;
  productTitle: string;
  buyerId: number;
  sellerId: number;
  buyerNickname: string;
  sellerNickname: string;
  status: string;
  finalPrice: number;
  createdAt: string;
};

const role = ref<"buyer" | "seller">("buyer");
const status = ref<string | undefined>();
const rows = ref<OrderRow[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;

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

async function load() {
  const { data } = await http.get<
    ApiResponse<{
      records: OrderRow[];
      total: number;
    }>
  >("/orders", {
    params: { role: role.value, status: status.value, page: page.value, size: pageSize }
  });
  if (data.code !== 200) return;
  rows.value = data.data.records;
  total.value = data.data.total;
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
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
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
