# Campus Trade
执行步骤：
- 克隆项目 ： git clone https://gitee.com/bcnlsn/compus-second-hand-trading-platform.git
- 创建数据库 ：在MySQL中创建 campus_trade 数据库
- 配置数据库连接 ：修改 application.yml 中的数据库配置
- 启动项目 ：后端会自动创建表结构
- 测试访问 ：访问 http://localhost:5173

## 目录结构

- `backend`: Spring Boot 3 + MyBatis-Plus + Flyway + JWT
- `frontend`: Vue3 + TypeScript + Element Plus + Vite

## 本地环境

- JDK 17（配置 `JAVA_HOME`）
- Maven 3.9+
- Node.js 20+
- MySQL 8
- Redis 7（已配置，可按需使用）
- Docker Desktop（可选，用于一键起 MySQL/Redis）

## 后端启动

1. 启动基础服务（可选）:

```bash
docker compose up -d
```

2. 修改 `backend/src/main/resources/application.yml` 中的数据库账号密码（默认 `root/root`）。
3. 在 `backend` 目录运行:

```bash
mvn spring-boot:run
```

4. 健康检查: `GET http://localhost:8080/api/health`

### 默认管理员账号（首次启动自动写入）

- 学号: `admin`
- 密码: `Admin123456`
- 登录时模式选择 **管理员**，账号填 `admin` 或 `10000000000`

### DeepSeek（AI 润色，可选）

在环境变量中设置 `DEEPSEEK_API_KEY`，或在 `application.yml` 的 `app.deepseek.api-key` 中填写。未配置时接口返回演示文案，不调用外部 API。

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`（已配置 `/api`、`/files` 代理到 `http://127.0.0.1:8080`）。

### 前端页面（部分）

- `/orders` — 我的订单（买家确认/取消、卖家生成收货二维码、双方评价）
- `/orders/confirm?token=` — 买家扫码打开后确认收货（需登录）
- `/publish` — 发布商品（含图片上传）
- `/my-products` — 我的商品列表与下架

## 已实现接口（摘要）

**认证与健康**

- `POST /api/auth/register` — 学生注册
- `POST /api/auth/login` — 登录（`mode`: `student` / `admin`）
- `GET /api/health`

**用户中心（需登录，请求头 `Authorization: Bearer <token>`）**

- `GET /api/user/me` — 个人资料
- `PUT /api/user/me` — 修改资料
- `POST /api/user/sign-in` — 每日签到（+1 减碳积分）

**分类与商品**

- `GET /api/categories` — 分类列表
- `GET /api/products` — 商品分页（`sort`: `latest` / `hot`；`mine=true` 时需登录，仅返回当前用户发布的商品，含各状态）
- `GET /api/products/{id}` — 商品详情（浏览量 +1）
- `POST /api/products` — 发布
- `PUT /api/products/{id}` — 编辑
- `POST /api/products/{id}/off-shelf` — 下架

**文件上传（需登录）**

- `POST /api/files/upload` — `multipart/form-data`，字段名 `file`；允许 jpg/jpeg/png/gif/webp/pdf；返回 `{ "url": "/files/xxx" }`  
- 静态访问：`GET /files/**`（无需登录，开发环境通过 Vite 代理到后端）

**订单与评价**

- `POST /api/orders` — 创建订单（商品变为锁定）
- `GET /api/orders` — 订单列表（`role`: `buyer` / `seller`，可选 `status`）
- `GET /api/orders/{id}` — 订单详情
- `POST /api/orders/{id}/cancel` — 买家取消
- `POST /api/orders/{id}/confirm` — 买家确认收货（完成交易、发放减碳积分）
- `POST /api/orders/{id}/confirm-token` — 卖家生成「扫码确认」链接与令牌（24 小时有效，仅待确认订单）
- `POST /api/orders/confirm-with-token` — 买家使用扫码页提交 body `{ "token": "..." }` 确认收货（与上者二选一）
- `POST /api/orders/reviews` — 提交评价（完成后）

**求购**

- `GET /api/wanted` — 求购列表
- `GET /api/wanted/{id}` — 详情
- `POST /api/wanted` — 发布
- `PUT /api/wanted/{id}` — 编辑
- `POST /api/wanted/{id}/close` — 关闭

**聊天与消息（需登录）**

- `POST /api/chat/conversations/start` — 发起或获取会话  
  body: `{ "peerUserId": number, "contextType": "GENERAL" | "PRODUCT" | "WANTED", "contextId": number }`  
  - 商品沟通：`PRODUCT` + 商品 `id`，`peerUserId` 为卖家  
  - 求购「我有这个」：`WANTED` + 求购 `id`，`peerUserId` 为求购发布者  
  - 普通私聊：`GENERAL`，`contextId` 省略或 `0`
- `GET /api/chat/conversations` — 会话列表（含最后一条预览、未读数、关联商品/求购标题与封面）
- `GET /api/chat/conversations/{id}/messages` — 分页消息（按页倒序取再正序展示）
- `GET /api/chat/conversations/{id}/messages/latest` — 最近约 50 条（正序，供消息页轮询）
- `POST /api/chat/conversations/{id}/messages` — 发送消息 body: `{ "content": "..." }`
- `POST /api/chat/conversations/{id}/read` — 标记已读（对方发来的未读消息）

前端：`/messages` 消息页（短轮询刷新列表与当前会话）；商品详情「联系卖家」、求购详情「我有这个」会跳转并带上 `?c=会话ID`。

**社区**

- `GET /api/posts` — 帖子分页（`sort`: `latest` / `hot`）
- `GET /api/posts/{id}` — 帖子详情
- `POST /api/posts` — 发帖
- `DELETE /api/posts/{id}` — 删帖（本人或管理员）
- `GET /api/posts/{id}/comments` — 评论树
- `POST /api/posts/comments` — 评论
- `POST /api/posts/{id}/like` — 帖子点赞/取消

**举报与 AI**

- `POST /api/reports` — 提交举报
- `POST /api/ai/polish` — 描述润色（需登录，DeepSeek 可选）

**管理后台（需管理员 JWT）**

- `GET /api/admin/dashboard`
- `GET /api/admin/users`
- `POST /api/admin/users/{id}/ban` — body: `{ "banned": true }`
- `POST /api/admin/users/{id}/credit` — body: `{ "score": 100 }`

## 数据库迁移协作规范

1. 只通过 Flyway 脚本改表结构，禁止手工改共享库。
2. 新脚本命名: `V{版本号}__{描述}.sql`。
3. 已合并的历史脚本不要改，只追加更高版本。

## 五人分工建议

- 成员 A: 用户中心与认证
- 成员 B: 商品与求购
- 成员 C: 订单与评价
- 成员 D: 社区频道
- 成员 E: 管理后台与治理

## 说明

当前代码覆盖核心流程与模块边界，便于并行开发。聊天为 **HTTP + 前端轮询**（未接 WebSocket）；二维码收货、申诉审核全流程等可在现有表与接口上继续扩展。
