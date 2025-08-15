import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import Login from '@/views/auth/Login.vue';
import Register from '@/views/auth/Register.vue';
import MerchantRegister from '@/views/auth/MerchantRegister.vue';
import OAuth2Login from '@/views/auth/OAuth2Login.vue';
import OAuth2Callback from '@/views/auth/OAuth2Callback.vue';
import Home from '@/views/Home.vue';
import AdminLogin from '@/views/admin/Login.vue';
import AdminDashboard from '@/views/admin/Dashboard.vue';
import ComponentDemo from '@/views/ComponentDemo.vue';
import UserManagement from '@/views/admin/UserManagement.vue';

// 定义路由
const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/oauth2/login',
    name: 'OAuth2Login',
    component: OAuth2Login
  },
  {
    path: '/oauth2/callback',
    name: 'OAuth2Callback',
    component: OAuth2Callback
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/merchant-register',
    name: 'MerchantRegister',
    component: MerchantRegister
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: AdminLogin
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: AdminDashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/components',
    name: 'ComponentDemo',
    component: ComponentDemo
  },
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: UserManagement,
    meta: { requiresAuth: true }
  }
];

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫
router.beforeEach((to, from, next) => {
  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token');
    if (token) {
      // 如果有token，允许访问
      next();
    } else {
      // 如果没有token，重定向到管理员登录页
      next('/admin/login');
    }
  } else {
    // 不需要认证的页面，直接访问
    next();
  }
});

export default router;