<!--
社区列表视图
负责成员D：社区频道 + 用户管理
-->
<template>
  <div class="posts-container">
    <div class="header">
      <el-input
        v-model="keyword"
        placeholder="搜索帖子标题或内容"
        clearable
        class="search-input"
        @keyup.enter="search"
        @clear="search"
      >
        <template #append>
          <el-button @click="search">搜索</el-button>
        </template>
      </el-input>
      <el-button type="success" @click="$router.push('/publish-post')">发布帖子</el-button>
    </div>
    <div class="toolbar">
      <span class="result-count">共找到 <strong>{{ total }}</strong> 条帖子</span>
      <el-button-group>
        <el-button
          :type="sort === 'latest' ? 'primary' : 'default'"
          @click="changeSort('latest')"
        >
          最新发布
        </el-button>
        <el-button
          :type="sort === 'hot' ? 'primary' : 'default'"
          @click="changeSort('hot')"
        >
          最热
        </el-button>
      </el-button-group>
    </div>
    <el-empty v-if="rows.length === 0" description="暂无帖子" />
    <div class="posts-list">
      <el-card v-for="post in rows" :key="post.id" class="post-card">
        <div class="post-header">
          <div class="user-info">
            <el-avatar :size="40" :src="post.avatarUrl || defaultAvatar">
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
          <div
            v-for="comment in flattenComments(post.comments)"
            :key="comment.id"
            class="comment-item"
            :style="{ marginLeft: `${comment.depth * 24}px` }"
          >
            <el-avatar :size="32" :src="comment.avatarUrl || defaultAvatar">
              {{ comment.nickname?.charAt(0) || "用" }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-header">
                <span class="comment-author">{{ comment.nickname }}</span>
                <div class="comment-actions">
                  <span class="comment-time">{{ comment.createdAt }}</span>
                  <el-button type="text" size="small" @click="startReply(post, comment)">回复</el-button>
                  <el-button
                    v-if="comment.isOwner"
                    type="text"
                    size="small"
                    @click="deleteComment(post, comment)"
                    style="color: #f56c6c"
                  >
                    <el-icon><Delete /></el-icon>
                    <span>删除</span>
                  </el-button>
                </div>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
            </div>
          </div>
          <div v-if="post.replyTo" class="reply-hint">
            回复 @{{ post.replyTo.nickname }}
            <el-button type="text" size="small" @click="cancelReply(post)">取消</el-button>
          </div>
          <el-input
            v-model="post.commentDraft"
            :placeholder="post.replyTo ? `回复 @${post.replyTo.nickname}` : '写下你的评论...'"
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
  userId: number;
  parentId: number | null;
  content: string;
  nickname: string;
  avatarUrl: string;
  createdAt: string;
  isOwner: boolean;
  children: Comment[];
};

type FlatComment = Comment & { depth: number };

type ReplyTarget = {
  id: number;
  nickname: string;
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
  commentDraft: string;
  replyTo: ReplyTarget | null;
  isOwner: boolean;
};

const rows = ref<Post[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;
const keyword = ref("");
const sort = ref<"latest" | "hot">("latest");
const auth = useAuthStore();
const defaultAvatar = "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

function mapComment(raw: any): Comment {
  return {
    id: raw.id,
    userId: Number(raw.userId),
    parentId: raw.parentId ?? null,
    content: raw.content,
    nickname: raw.userNickname || "匿名用户",
    avatarUrl: raw.userAvatarUrl || "",
    createdAt: raw.createdAt,
    isOwner: auth.user?.id === Number(raw.userId),
    children: (raw.children || []).map(mapComment)
  };
}

function flattenComments(comments: Comment[], depth = 0): FlatComment[] {
  const result: FlatComment[] = [];
  for (const comment of comments) {
    result.push({ ...comment, depth });
    if (comment.children.length > 0) {
      result.push(...flattenComments(comment.children, depth + 1));
    }
  }
  return result;
}

function startReply(post: Post, comment: Comment) {
  post.replyTo = { id: comment.id, nickname: comment.nickname };
}

function cancelReply(post: Post) {
  post.replyTo = null;
}

function search() {
  page.value = 1;
  load();
}

function changeSort(next: "latest" | "hot") {
  if (sort.value === next) return;
  sort.value = next;
  page.value = 1;
  load();
}

async function load() {
  const params: Record<string, string | number> = {
    page: page.value,
    size: pageSize,
    sort: sort.value
  };
  if (keyword.value.trim()) {
    params.keyword = keyword.value.trim();
  }
  const { data } = await http.get<
    ApiResponse<{
      records: any[];
      total: number;
    }>
  >("/posts", { params });
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
      avatarUrl: detailData.data?.userAvatarUrl || '',
      liked: record.liked || false,
      showComments: false,
      comments: [],
      commentDraft: "",
      replyTo: null,
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

async function loadComments(post: Post) {
  const { data } = await http.get<ApiResponse<any[]>>(`/posts/${post.id}/comments`);
  if (data.code === 200) {
    post.comments = data.data.map(mapComment);
  }
}

async function showComments(post: Post) {
  post.showComments = !post.showComments;
  if (post.showComments) {
    await loadComments(post);
  } else {
    post.replyTo = null;
    post.commentDraft = "";
  }
}

async function addComment(post: Post) {
  const content = post.commentDraft.trim();
  if (!content) return;

  const payload: { postId: number; content: string; parentId?: number } = {
    postId: post.id,
    content
  };
  if (post.replyTo) {
    payload.parentId = post.replyTo.id;
  }

  const { data } = await http.post<ApiResponse<number>>("/posts/comments", payload);

  if (data.code === 200) {
    post.commentDraft = "";
    post.replyTo = null;
    await loadComments(post);
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
      await loadComments(post);
      const { data: detailData } = await http.get<ApiResponse<{ commentCount: number }>>(`/posts/${post.id}`);
      if (detailData.code === 200) {
        post.commentCount = detailData.data.commentCount;
      }
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
  gap: 16px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-count {
  font-size: 14px;
  color: #606266;
}

.result-count strong {
  color: #409eff;
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
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.comment-body {
  flex: 1;
  min-width: 0;
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

.reply-hint {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  margin-bottom: 32px;
}
</style>
