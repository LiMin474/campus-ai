import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: () => import("../views/HomeView.vue") },
    { path: "/login", name: "login", component: () => import("../views/LoginView.vue") },
    { path: "/register", name: "register", component: () => import("../views/RegisterView.vue") },
    { path: "/products", name: "products", component: () => import("../views/ProductsView.vue") },
    { path: "/products/:id", name: "product-detail", component: () => import("../views/ProductDetailView.vue") },
    { path: "/profile", name: "profile", component: () => import("../views/ProfileView.vue"), meta: { auth: true } },
    { path: "/messages", name: "messages", component: () => import("../views/MessagesView.vue"), meta: { auth: true } },
    { path: "/orders", name: "orders", component: () => import("../views/OrdersView.vue"), meta: { auth: true } },
    { path: "/orders/confirm", name: "order-confirm", component: () => import("../views/OrderConfirmView.vue"), meta: { auth: true } },
    { path: "/publish", name: "publish", component: () => import("../views/PublishProductView.vue"), meta: { auth: true } },
    { path: "/publish-post", name: "publish-post", component: () => import("../views/PublishPostView.vue"), meta: { auth: true } },
    { path: "/my-products", name: "my-products", component: () => import("../views/MyProductsView.vue"), meta: { auth: true } },
    { path: "/wanted", name: "wanted", component: () => import("../views/WantedListView.vue") },
    { path: "/wanted/:id", name: "wanted-detail", component: () => import("../views/WantedDetailView.vue") },
    { path: "/posts", name: "posts", component: () => import("../views/PostsView.vue") },
    // 管理端路由
    { path: "/admin/dashboard", name: "admin-dashboard", component: () => import("../views/admin/AdminDashboardView.vue"), meta: { auth: true, admin: true } },
    { path: "/admin/users", name: "admin-users", component: () => import("../views/admin/AdminUsersView.vue"), meta: { auth: true, admin: true } },
    { path: "/admin/reports", name: "admin-reports", component: () => import("../views/admin/AdminReportsView.vue"), meta: { auth: true, admin: true } }
  ]
});

router.beforeEach((to) => {
  if (to.meta.auth) {
    const auth = useAuthStore();
    if (!auth.token) {
      return { name: "login", query: { redirect: to.fullPath } };
    }
    // 检查是否是管理员路由
    if (to.meta.admin && auth.user?.role !== 'ADMIN') {
      return { name: "home" };
    }
  }
  return true;
});

export default router;
