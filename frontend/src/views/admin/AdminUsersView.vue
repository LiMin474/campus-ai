<!--
管理员用户管理视图
负责成员D：社区频道 + 用户管理
-->
<template>
  <div class="admin-users">
    <el-card shadow="hover" class="users-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-input
            v-model="keyword"
            placeholder="搜索昵称或学号"
            style="width: 300px"
            prefix-icon="el-icon-search"
            @keyup.enter="searchUsers"
          >
            <template #append>
              <el-button @click="searchUsers">搜索</el-button>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="users" style="width: 100%">
        <el-table-column prop="id" label="用户ID" width="80" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="creditScore" label="信用分" width="100" />
        <el-table-column prop="carbonPoints" label="减碳积分" width="120" />
        <el-table-column prop="appealFailCount" label="申诉失败次数" width="150" />
        <el-table-column prop="banned" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.banned ? 'danger' : 'success'">
              {{ scope.row.banned ? '已封禁' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button 
              type="danger" 
              size="small" 
              @click="toggleBan(scope.row)"
              :disabled="scope.row.role === 'ADMIN'"
            >
              {{ scope.row.banned ? '解封' : '封禁' }}
            </el-button>
            <el-button type="primary" size="small" @click="resetCredit(scope.row)">
              重置信用分
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" style="margin-top: 20px">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 重置信用分对话框 -->
    <el-dialog v-model="creditDialogVisible" title="重置信用分" width="400px">
      <el-form :model="creditForm" label-width="100px">
        <el-form-item label="用户昵称">
          <el-input v-model="creditForm.nickname" disabled />
        </el-form-item>
        <el-form-item label="当前信用分">
          <el-input v-model="creditForm.currentScore" disabled />
        </el-form-item>
        <el-form-item label="新信用分">
          <el-input v-model.number="creditForm.newScore" type="number" min="0" max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="creditDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCreditReset">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { http } from '../../api/http';

interface User {
  id: number;
  studentNo: string;
  nickname: string;
  phone: string;
  role: string;
  creditScore: number;
  carbonPoints: number;
  appealFailCount: number;
  banned: boolean;
  createdAt: string;
}

const users = ref<User[]>([]);
const keyword = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 重置信用分对话框
const creditDialogVisible = ref(false);
const creditForm = ref({
  userId: '',
  nickname: '',
  currentScore: '',
  newScore: 100
});

const loadUsers = async () => {
  try {
    const response = await http.get('/api/admin/users', {
      params: { keyword: keyword.value }
    });
    users.value = response.data;
    total.value = response.data.length;
  } catch (error) {
    ElMessage.error('加载用户列表失败');
    console.error('Failed to load users:', error);
  }
};

const searchUsers = () => {
  currentPage.value = 1;
  loadUsers();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  loadUsers();
};

const handleCurrentChange = (current: number) => {
  currentPage.value = current;
  loadUsers();
};

const toggleBan = async (user: User) => {
  try {
    await http.post(`/api/admin/users/${user.id}/ban`, {
      banned: !user.banned
    });
    ElMessage.success(`${user.banned ? '解封' : '封禁'}成功`);
    loadUsers();
  } catch (error) {
    ElMessage.error('操作失败');
    console.error('Failed to toggle ban:', error);
  }
};

const resetCredit = (user: User) => {
  creditForm.value = {
    userId: user.id,
    nickname: user.nickname,
    currentScore: user.creditScore,
    newScore: user.creditScore
  };
  creditDialogVisible.value = true;
};

const submitCreditReset = async () => {
  try {
    await http.post(`/api/admin/users/${creditForm.value.userId}/credit`, {
      score: creditForm.value.newScore
    });
    ElMessage.success('重置信用分成功');
    creditDialogVisible.value = false;
    loadUsers();
  } catch (error) {
    ElMessage.error('操作失败');
    console.error('Failed to reset credit:', error);
  }
};

onMounted(() => {
  loadUsers();
});
</script>

<style scoped>
.admin-users {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.users-card {
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