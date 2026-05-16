# 小组分工文档

## 项目结构说明

本项目采用前后端分离架构，后端使用 Spring Boot 3，前端使用 Vue 3 + TypeScript + Vite。

---

## 成员分工总览

| 成员 | 负责模块 | 后端路径 | 前端路径 |
|------|----------|----------|----------|
| 成员A | 用户中心 + 订单评价 | `backend/src/main/java/com/campus/user/`<br>`backend/src/main/java/com/campus/order/` | `frontend/src/views/ProfileView.vue`<br>`frontend/src/views/OrdersView.vue` |
| 成员B | 商品交易 + 求购专区 | `backend/src/main/java/com/campus/product/`<br>`backend/src/main/java/com/campus/wanted/` | `frontend/src/views/ProductsView.vue`<br>`frontend/src/views/ProductDetailView.vue`<br>`frontend/src/views/PublishProductView.vue`<br>`frontend/src/views/WantedListView.vue`<br>`frontend/src/views/WantedDetailView.vue` |
| 成员C | 聊天系统 | `backend/src/main/java/com/campus/chat/` | `frontend/src/views/MessagesView.vue` |
| 成员D | 社区频道 + 用户管理 | `backend/src/main/java/com/campus/community/` | `frontend/src/views/PostsView.vue`<br>`frontend/src/views/PublishPostView.vue`<br>`frontend/src/views/admin/AdminUsersView.vue` |
| 成员E | 管理员管理模块 | `backend/src/main/java/com/campus/admin/` | `frontend/src/views/admin/AdminDashboardView.vue`<br>`frontend/src/views/admin/AdminReportsView.vue` |

---

## 后端详细分工

### 成员A：用户中心 + 订单评价

**后端文件：**
```
backend/src/main/java/com/campus/user/
├── controller/
│   └── UserController.java      # 用户中心API
│   └── AuthController.java      # 用户认证API
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   ├── UserProfileResponse.java
│   └── UserUpdateRequest.java
├── entity/
│   └── User.java
├── mapper/
│   └── UserMapper.java
├── service/
│   ├── AuthService.java
│   └── UserService.java
└── util/
    └── CreditRules.java

backend/src/main/java/com/campus/order/
├── controller/
│   └── OrderController.java      # 订单评价API
├── dto/
│   ├── OrderCreateRequest.java
│   ├── OrderResponse.java
│   └── ReviewCreateRequest.java
├── entity/
│   ├── Review.java
│   └── TradeOrder.java
├── mapper/
│   ├── ReviewMapper.java
│   └── TradeOrderMapper.java
└── service/
    ├── OrderService.java
    └── ReviewService.java
```

**前端文件：**
```
frontend/src/views/ProfileView.vue      # 用户中心页面
frontend/src/views/OrdersView.vue       # 订单评价页面
```

---

### 成员B：商品交易 + 求购专区

**后端文件：**
```
backend/src/main/java/com/campus/product/
├── controller/
│   └── ProductController.java          # 商品交易API
├── dto/
│   ├── ProductCreateRequest.java
│   ├── ProductDetailResponse.java
│   ├── ProductListItemResponse.java
│   └── ProductUpdateRequest.java
├── entity/
│   ├── Product.java
│   ├── ProductAttachment.java
│   └── ProductImage.java
├── mapper/
│   ├── ProductAttachmentMapper.java
│   ├── ProductImageMapper.java
│   └── ProductMapper.java
└── service/
    └── ProductService.java

backend/src/main/java/com/campus/wanted/
├── controller/
│   └── WantedController.java            # 求购专区API
├── dto/
│   ├── WantedRequest.java
│   └── WantedResponse.java
├── entity/
│   └── Wanted.java
├── mapper/
│   └── WantedMapper.java
└── service/
    └── WantedService.java
```

**前端文件：**
```
frontend/src/views/ProductsView.vue           # 商品列表页面
frontend/src/views/ProductDetailView.vue     # 商品详情页面
frontend/src/views/PublishProductView.vue    # 发布商品页面
frontend/src/views/WantedListView.vue         # 求购列表页面
frontend/src/views/WantedDetailView.vue       # 求购详情页面
```

---

### 成员C：聊天系统

**后端文件：**
```
backend/src/main/java/com/campus/chat/
├── controller/
│   ├── ChatController.java                # 聊天API
│   └── WebSocketController.java          # WebSocket实时通信
├── dto/
│   ├── ChatMessageResponse.java
│   ├── ConversationListItemResponse.java
│   ├── SendMessageRequest.java
│   └── StartConversationRequest.java
├── entity/
│   ├── ChatMessage.java
│   └── Conversation.java
├── mapper/
│   ├── ChatMessageMapper.java
│   └── ConversationMapper.java
└── service/
    └── ChatService.java
```

**前端文件：**
```
frontend/src/views/MessagesView.vue          # 消息聊天页面
```

---

### 成员D：社区频道 + 用户管理

**后端文件：**
```
backend/src/main/java/com/campus/community/
├── controller/
│   └── CommunityController.java           # 社区频道API
├── dto/
│   ├── CommentCreateRequest.java
│   ├── CommentNodeResponse.java
│   ├── PostCreateRequest.java
│   └── PostDetailResponse.java
├── entity/
│   ├── CommunityComment.java
│   ├── CommunityPost.java
│   ├── CommunityPostImage.java
│   └── LikeRecord.java
├── mapper/
│   ├── CommunityCommentMapper.java
│   ├── CommunityPostImageMapper.java
│   ├── CommunityPostMapper.java
│   └── LikeRecordMapper.java
└── service/
    └── CommunityService.java
```

**前端文件：**
```
frontend/src/views/PostsView.vue              # 社区列表页面
frontend/src/views/PublishPostView.vue        # 发布帖子页面
frontend/src/views/admin/AdminUsersView.vue   # 用户管理页面
```

---

### 成员E：管理员管理模块

**后端文件：**
```
backend/src/main/java/com/campus/admin/
├── controller/
│   └── AdminController.java                  # 管理员API
├── dto/
│   └── AdminDashboardResponse.java
└── service/
    └── AdminService.java
```

**前端文件：**
```
frontend/src/views/admin/AdminDashboardView.vue   # 管理员仪表盘
frontend/src/views/admin/AdminReportsView.vue     # 管理员报表
```

---

## 数据库说明

数据库使用 Flyway 进行版本管理，SQL 脚本位于：

```
backend/src/main/resources/db/migration/
├── V1__init_schema.sql      # 初始数据库结构
├── V2__extend_features.sql  # 扩展功能
├── V3__chat.sql            # 聊天功能（成员C）
├── V4__order_confirm_token.sql  # 订单确认Token
└── V5__*.sql               # 后续由各成员添加
```

**各成员在修改数据库时：**
- 成员A：如需修改用户表或订单表，创建 `V5__user_order_extend.sql`
- 成员B：如需修改商品表或求购表，创建 `V6__product_wanted_extend.sql`
- 成员C：如需修改聊天相关表，创建 `V7__chat_extend.sql`
- 成员D：如需修改社区相关表，创建 `V8__community_extend.sql`
- 成员E：如需修改管理员相关表，创建 `V9__admin_extend.sql`

---

## Git 协作建议

### 分支命名规范
```
feature/user-center       # 成员A
feature/product-trade     # 成员B
feature/chat-system       # 成员C
feature/community         # 成员D
feature/admin-module      # 成员E
```

### 提交信息规范
```
[成员A] 添加用户头像上传功能
[成员B] 优化商品搜索性能
[成员C] 修复消息推送延迟问题
[成员D] 添加评论嵌套显示
[成员E] 添加数据统计报表
```

### 代码审查
- 各成员完成功能后，提交 Pull Request
- 由项目经理或指定成员进行代码审查
- 审查通过后合并到主分支

---

## 联系方式

| 成员 | 模块 | 职责说明 |
|------|------|----------|
| 成员A | 用户中心 + 订单评价 | 负责用户注册、登录、个人信息、订单创建与评价 |
| 成员B | 商品交易 + 求购专区 | 负责商品发布、浏览、求购信息管理 |
| 成员C | 聊天系统 | 负责买卖双方实时沟通功能 |
| 成员D | 社区频道 + 用户管理 | 负责社区发帖、评论、用户列表管理 |
| 成员E | 管理员管理模块 | 负责系统仪表盘、数据统计、报表功能 |

---

## 开发注意事项

1. **跨模块调用**：如需调用其他成员的接口，请通过 API 进行，避免直接修改他人代码
2. **数据库修改**：修改数据库结构前，请先在团队群中通知其他成员
3. **接口变更**：如需修改接口定义，请提前与相关成员沟通
4. **环境配置**：本地开发请参考 `.env.example` 配置环境变量
5. **依赖安装**：后端运行 `mvn install`，前端运行 `npm install`

---

最后更新日期：2026-04-22
