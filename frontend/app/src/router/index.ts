import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import OAuth2Login from '@/views/auth/OAuth2Login.vue';
import OAuth2Callback from '@/views/auth/OAuth2Callback.vue';
import Register from '@/views/auth/Register.vue';
import MerchantRegister from '@/views/auth/MerchantRegister.vue';
import Home from '@/views/Home.vue';
import AdminLogin from '@/views/admin/Login.vue';
import AdminDashboard from '@/views/admin/Dashboard.vue';
import UserManagement from '@/views/admin/UserManagement.vue';
import { useUserStore } from '@/stores/user';

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
    component: OAuth2Login
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
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: UserManagement,
    meta: { requiresAuth: true, role: 'ADMIN' }
  }
];

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    const userStore = useUserStore();
    const token = userStore.getToken;
    
    if (token) {
      // 验证token是否有效
      try {
        // 如果用户信息不存在，尝试从后端获取
        if (!userStore.getUserInfo && !userStore.getAdminInfo) {
          // 这里可以调用API获取用户信息
          // await userStore.fetchUserInfo()
        }
        
        // 检查角色权限
        if (to.meta.role) {
          const requiredRole = to.meta.role as string;
          if (requiredRole === 'ADMIN' && userStore.isUserAdmin) {
            next();
          } else if (requiredRole === 'USER' && userStore.isUserAuthenticated) {
            next();
          } else {
            next('/login');
          }
        } else {
          // 仅需要认证，不需要特定角色
          next();
        }
      } catch (error) {
        // token无效，清除并重定向到登录页
        userStore.clearUserInfo();
        next('/login');
      }
    } else {
      // 如果没有token，重定向到登录页
      next('/login');
    }
  } else {
    // 不需要认证的页面，直接访问
    next();
  }
});

export default router;