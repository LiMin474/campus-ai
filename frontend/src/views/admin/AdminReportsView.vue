<!--
管理员报表视图
负责成员E：管理员管理模块
-->
<template>
  <div class="admin-reports">
    <el-card shadow="hover" class="reports-card">
      <template #header>
        <div class="card-header">
          <span>举报与申诉管理</span>
          <el-button type="primary" @click="refreshData">
            <el-icon><Refresh /></el-icon> 刷新数据
          </el-button>
        </div>
      </template>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待处理举报" name="pending-reports">
          <el-table :data="pendingReports" style="width: 100%">
            <el-table-column prop="id" label="举报ID" width="80" />
            <el-table-column prop="reporterId" label="举报人ID" width="120" />
            <el-table-column prop="targetType" label="举报对象类型" width="150" />
            <el-table-column prop="targetId" label="举报对象ID" width="120" />
            <el-table-column prop="reason" label="举报原因" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="举报时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button type="primary" size="small" @click="handleReport(scope.row)">
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="所有举报" name="all-reports">
          <el-table :data="allReports" style="width: 100%">
            <el-table-column prop="id" label="举报ID" width="80" />
            <el-table-column prop="reporterId" label="举报人ID" width="120" />
            <el-table-column prop="targetType" label="举报对象类型" width="150" />
            <el-table-column prop="targetId" label="举报对象ID" width="120" />
            <el-table-column prop="reason" label="举报原因" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="adminRemark" label="处理意见" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="举报时间" width="180" />
            <el-table-column prop="updatedAt" label="处理时间" width="180" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="待处理申诉" name="pending-appeals">
          <el-table :data="pendingAppeals" style="width: 100%">
            <el-table-column prop="id" label="申诉ID" width="80" />
            <el-table-column prop="reviewId" label="评价ID" width="120" />
            <el-table-column prop="userId" label="申诉用户ID" width="120" />
            <el-table-column prop="reason" label="申诉原因" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申诉时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button type="primary" size="small" @click="handleAppeal(scope.row)">
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="所有申诉" name="all-appeals">
          <el-table :data="allAppeals" style="width: 100%">
            <el-table-column prop="id" label="申诉ID" width="80" />
            <el-table-column prop="reviewId" label="评价ID" width="120" />
            <el-table-column prop="userId" label="申诉用户ID" width="120" />
            <el-table-column prop="reason" label="申诉原因" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="adminOpinion" label="处理意见" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申诉时间" width="180" />
            <el-table-column prop="updatedAt" label="处理时间" width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 处理举报对话框 -->
    <el-dialog v-model="reportDialogVisible" title="处理举报" width="500px">
      <el-form :model="reportForm" label-width="80px">
        <el-form-item label="举报ID">
          <el-input v-model="reportForm.id" disabled />
        </el-form-item>
        <el-form-item label="举报原因">
          <el-input v-model="reportForm.reason" type="textarea" rows="3" disabled />
        </el-form-item>
        <el-form-item label="证据链接">
          <el-input v-model="reportForm.evidenceUrl" disabled />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="reportForm.approve">
            <el-radio label="true">批准举报</el-radio>
            <el-radio label="false">驳回举报</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="reportForm.adminRemark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reportDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReportHandle">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 处理申诉对话框 -->
    <el-dialog v-model="appealDialogVisible" title="处理申诉" width="500px">
      <el-form :model="appealForm" label-width="80px">
        <el-form-item label="申诉ID">
          <el-input v-model="appealForm.id" disabled />
        </el-form-item>
        <el-form-item label="评价ID">
          <el-input v-model="appealForm.reviewId" disabled />
        </el-form-item>
        <el-form-item label="申诉原因">
          <el-input v-model="appealForm.reason" type="textarea" rows="3" disabled />
        </el-form-item>
        <el-form-item label="证据链接">
          <el-input v-model="appealForm.evidenceUrl" disabled />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="appealForm.approve">
            <el-radio label="true">批准申诉</el-radio>
            <el-radio label="false">驳回申诉</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="appealForm.adminOpinion" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="appealDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAppealHandle">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { http } from '../../api/http';

interface Report {
  id: number;
  reporterId: number;
  targetType: string;
  targetId: number;
  reason: string;
  evidenceUrl: string;
  status: string;
  adminRemark: string;
  createdAt: string;
  updatedAt: string;
}

interface Appeal {
  id: number;
  reviewId: number;
  userId: number;
  reason: string;
  evidenceUrl: string;
  status: string;
  adminOpinion: string;
  createdAt: string;
  updatedAt: string;
}

const activeTab = ref('pending-reports');
const pendingReports = ref<Report[]>([]);
const allReports = ref<Report[]>([]);
const pendingAppeals = ref<Appeal[]>([]);
const allAppeals = ref<Appeal[]>([]);

// 处理举报对话框
const reportDialogVisible = ref(false);
const reportForm = ref({
  id: '',
  reason: '',
  evidenceUrl: '',
  approve: true,
  adminRemark: ''
});

// 处理申诉对话框
const appealDialogVisible = ref(false);
const appealForm = ref({
  id: '',
  reviewId: '',
  reason: '',
  evidenceUrl: '',
  approve: true,
  adminOpinion: ''
});

const loadPendingReports = async () => {
  try {
    const response = await http.get('/api/reports/admin/pending');
    pendingReports.value = response.data;
  } catch (error) {
    ElMessage.error('加载待处理举报失败');
    console.error('Failed to load pending reports:', error);
  }
};

const loadAllReports = async () => {
  try {
    const response = await http.get('/api/reports/admin/all');
    allReports.value = response.data;
  } catch (error) {
    ElMessage.error('加载所有举报失败');
    console.error('Failed to load all reports:', error);
  }
};

const loadPendingAppeals = async () => {
  try {
    const response = await http.get('/api/reports/appeals/admin/pending');
    pendingAppeals.value = response.data;
  } catch (error) {
    ElMessage.error('加载待处理申诉失败');
    console.error('Failed to load pending appeals:', error);
  }
};

const loadAllAppeals = async () => {
  try {
    const response = await http.get('/api/reports/appeals/admin/all');
    allAppeals.value = response.data;
  } catch (error) {
    ElMessage.error('加载所有申诉失败');
    console.error('Failed to load all appeals:', error);
  }
};

const refreshData = () => {
  switch (activeTab.value) {
    case 'pending-reports':
      loadPendingReports();
      break;
    case 'all-reports':
      loadAllReports();
      break;
    case 'pending-appeals':
      loadPendingAppeals();
      break;
    case 'all-appeals':
      loadAllAppeals();
      break;
  }
};

const handleTabChange = (tab: string) => {
  refreshData();
};

const handleReport = (report: Report) => {
  reportForm.value = {
    id: report.id,
    reason: report.reason,
    evidenceUrl: report.evidenceUrl,
    approve: true,
    adminRemark: ''
  };
  reportDialogVisible.value = true;
};

const handleAppeal = (appeal: Appeal) => {
  appealForm.value = {
    id: appeal.id,
    reviewId: appeal.reviewId,
    reason: appeal.reason,
    evidenceUrl: appeal.evidenceUrl,
    approve: true,
    adminOpinion: ''
  };
  appealDialogVisible.value = true;
};

const submitReportHandle = async () => {
  try {
    await http.post(`/api/reports/admin/${reportForm.value.id}/handle`, {
      approve: reportForm.value.approve,
      adminRemark: reportForm.value.adminRemark
    });
    ElMessage.success('处理成功');
    reportDialogVisible.value = false;
    loadPendingReports();
  } catch (error) {
    ElMessage.error('处理失败');
    console.error('Failed to handle report:', error);
  }
};

const submitAppealHandle = async () => {
  try {
    await http.post(`/api/reports/appeals/admin/${appealForm.value.id}/handle`, {
      approve: appealForm.value.approve,
      adminOpinion: appealForm.value.adminOpinion
    });
    ElMessage.success('处理成功');
    appealDialogVisible.value = false;
    loadPendingAppeals();
  } catch (error) {
    ElMessage.error('处理失败');
    console.error('Failed to handle appeal:', error);
  }
};

onMounted(() => {
  loadPendingReports();
});
</script>

<style scoped>
.admin-reports {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.reports-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>