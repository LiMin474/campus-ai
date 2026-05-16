<!--
社区列表视图
负责成员D：社区频道 + 用户管理
-->
<template>
  <div class="posts-container">
    <div class="header">
      <el-button type="primary" @click="load">刷新</el-button>
      <el-button type="success" @click="$router.push('/publish-post')">发布帖子</el-button>
    </div>
    <div class="posts-list">
      <el-card v-for="post in rows" :key="post.id" class="post-card">
        <div class="post-header">
          <div class="user-info">
            <el-avatar :size="40" :src="post.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'">
              {{ post.nickname?.charAt(0) || '用' }}
            </el-avatar>
            <div class="user-details">
              <div class="nickname">{{ post.nickname || '匿名用户' }}</div>
              <div class="post-time">{{ post.createdAt }}</div>
            </div>
          </div>
        </div>
        <div class="post-content">
          <h3 class="post-title">{{ post.title }}</h3>
          <div class="post-body">{{ post.content }}</div>
          <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-images">
            <el-image
              v-for="(imageUrl, index) in post.imageUrls"
              :key="index"
              :src="imageUrl"
              fit="cover"
              class="post-image"
              :preview-src-list="post.imageUrls"
            />
          </div>
        </div>
        <div class="post-actions">
          <el-button
            type="text"
            @click="toggleLike(post)"
            :class="{ 'liked': post.liked }"
          >
            <el-icon><Star /></el-icon>
            <span>{{ post.likeCount }}</span>
          </el-button>
          <el-button type="text" @click="showComments(post)">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ post.commentCount }}</span>
          </el-button>
          <el-button
            v-if="post.isOwner"
            type="text"
            @click="deletePost(post)"
            style="color: #f56c6c"
          >
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </el-button>
        </div>
        <div v-if="post.showComments" class="post-comments">
          <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <span class="comment-author">{{ comment.nickname }}</span>
              <div class="comment-actions">
                <span class="comment-time">{{ comment.createdAt }}</span>
                <el-button
                  v-if="comment.isOwner"
                  type="text"
                  size="small"
                  @click="deleteComment(post, comment)"
                  style="color: #f56c6c; margin-left: 10px"
                >
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-button>
              </div>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
          </div>
          <el-input
            v-model="commentContent"
            placeholder="写下你的评论..."
            @keyup.enter="addComment(post)"
          >
            <template #append>
              <el-button @click="addComment(post)">发送</el-button>
            </template>
          </el-input>
        </div>
      </el-card>
    </div>
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
import { ElMessageBox } from "element-plus";
import { http, type ApiResponse } from "../api/http";
import { Star, ChatDotRound, Delete } from "@element-plus/icons-vue";
import { useAuthStore } from "../stores/auth";

type Comment = {
  id: number;
  content: string;
  nickname: string;
  createdAt: string;
  isOwner: boolean;
};

type Post = {
  id: number;
  title: string;
  content: string;
  imageUrls: string[];
  likeCount: number;
  commentCount: number;
  createdAt: string;
  nickname: string;
  avatarUrl: string;
  liked: boolean;
  showComments: boolean;
  comments: Comment[];
  isOwner: boolean;
};

const rows = ref<Post[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;
const commentContent = ref("");
const auth = useAuthStore();

async function load() {
  const { data } = await http.get<
    ApiResponse<{
      records: any[];
      total: number;
    }>
  >("/posts", { params: { page: page.value, size: pageSize, sort: "latest" } });
  if (data.code !== 200) return;
  
  // 转换数据格式
  const posts = await Promise.all(data.data.records.map(async (record) => {
    // 获取帖子详情，包括图片URL
    const { data: detailData } = await http.get<ApiResponse<any>>(`/posts/${record.id}`);
    return {
      id: record.id,
      title: record.title,
      content: record.content,
      imageUrls: detailData.data?.imageUrls || [],
      likeCount: record.likeCount || 0,
      commentCount: record.commentCount || 0,
      createdAt: record.createdAt,
      nickname: detailData.data?.userNickname || '匿名用户',
      avatarUrl: '', // 暂时使用默认头像
      liked: record.liked || false,
      showComments: false,
      comments: [],
      isOwner: auth.user?.id === Number(detailData.data?.userId)
    };
  }));
  
  rows.value = posts;
  total.value = data.data.total;
}

async function toggleLike(post: Post) {
  const { data } = await http.post<ApiResponse<boolean>>(`/posts/${post.id}/like`);
  if (data.code === 200) {
    post.liked = data.data;
    post.likeCount += data.data ? 1 : -1;
  }
}

async function showComments(post: Post) {
  post.showComments = !post.showComments;
  if (post.showComments && post.comments.length === 0) {
    const { data } = await http.get<ApiResponse<any[]>>(`/posts/${post.id}/comments`);
    if (data.code === 200) {
      // 转换评论数据，确保昵称和isOwner正确显示
      post.comments = data.data.map(comment => ({
        id: comment.id,
        content: comment.content,
        nickname: comment.userNickname || '匿名用户',
        createdAt: comment.createdAt,
        isOwner: auth.user?.id === Number(comment.userId)
      }));
    }
  }
}

async function addComment(post: Post) {
  if (!commentContent.value) return;
  
  const { data } = await http.post<ApiResponse<number>>(`/posts/comments`, {
    postId: post.id,
    content: commentContent.value
  });
  
  if (data.code === 200) {
    commentContent.value = "";
    await showComments(post);
    post.commentCount++;
  }
}

async function deletePost(post: Post) {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条帖子吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    const { data } = await http.delete<ApiResponse<boolean>>(`/posts/${post.id}`);
    if (data.code === 200) {
      rows.value = rows.value.filter(p => p.id !== post.id);
      total.value--;
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除帖子失败', error);
    }
  }
}

async function deleteComment(post: Post, comment: Comment) {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评论吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    const { data } = await http.delete<ApiResponse<boolean>>(`/posts/comments/${comment.id}`);
    if (data.code === 200) {
      post.comments = post.comments.filter(c => c.id !== comment.id);
      post.commentCount--;
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除评论失败', error);
    }
  }
}

onMounted(async () => {
  await auth.fetchUser();
  load();
});
</script>

<style scoped>
.posts-container {
  max-width: 800px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-weight: 500;
  font-size: 16px;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

.post-content {
  margin-bottom: 16px;
}

.post-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.post-body {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 16px;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.post-image {
  width: 100%;
  height: 150px;
  border-radius: 8px;
}

.post-actions {
  display: flex;
  gap: 20px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.post-actions .el-button {
  font-size: 14px;
}

.post-actions .el-button.liked {
  color: #f56c6c;
}

.post-comments {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.comment-item {
  margin-bottom: 12px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.comment-actions {
  display: flex;
  align-items: center;
}

.comment-author {
  font-weight: 500;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-content {
  font-size: 14px;
  line-height: 1.5;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  margin-bottom: 32px;
}
</style>
