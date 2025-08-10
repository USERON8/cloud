import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../components/layout/AppLayout.vue'

// 首页相关
import HomePage from '../components/HomePage.vue'

// 认证相关
import Login from '../views/auth/Login.vue'
import Register from '../views/auth/Register.vue'

// 用户相关
import UserDashboard from '../views/user/Dashboard.vue'
import UserProfile from '../views/user/Profile.vue'

// 商家相关
import MerchantDashboard from '../views/merchant/Dashboard.vue'
import MerchantProducts from '../views/merchant/Products.vue'

// 管理员相关
import AdminDashboard from '../views/admin/Dashboard.vue'
import AdminUsers from '../views/admin/Users.vue'

// 库存管理
import StockManager from '../components/StockManager.vue'

// 统计报表
import Statistics from '../components/Statistics.vue'

const routes = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: HomePage,
        meta: { requiresAuth: true, role: 'USER' }
      },
      // 用户路由
      {
        path: '/user',
        name: 'UserDashboard',
        component: UserDashboard,
        meta: { requiresAuth: true, role: 'USER' }
      },
      {
        path: '/user/profile',
        name: 'UserProfile',
        component: UserProfile,
        meta: { requiresAuth: true, role: 'USER' }
      },
      // 商家路由
      {
        path: '/merchant',
        name: 'MerchantDashboard',
        component: MerchantDashboard,
        meta: { requiresAuth: true, role: 'MERCHANT' }
      },
      {
        path: '/merchant/products',
        name: 'MerchantProducts',
        component: MerchantProducts,
        meta: { requiresAuth: true, role: 'MERCHANT' }
      },
      // 管理员路由
      {
        path: '/admin',
        name: 'AdminDashboard',
        component: AdminDashboard,
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: '/admin/users',
        name: 'AdminUsers',
        component: AdminUsers,
        meta: { requiresAuth: true, role: 'ADMIN' }
      },
      // 库存管理
      {
        path: '/stock',
        name: 'StockManager',
        component: StockManager,
        meta: { requiresAuth: true }
      },
      // 统计报表
      {
        path: '/statistics',
        name: 'Statistics',
        component: Statistics,
        meta: { requiresAuth: true }
      }
    ]
  },
  // 认证路由
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/auth/register',
    name: 'Register',
    component: Register
  }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    const userType = localStorage.getItem('userType')

    // 检查是否需要认证
    if (to.meta.requiresAuth && !token) {
        next('/login')
    } 
    // 检查角色权限
    else if (to.meta.role && token && to.meta.role !== userType) {
        // 根据用户类型重定向到对应的主页
        switch (userType) {
        case 'USER':
            next('/user')
            break
        case 'MERCHANT':
            next('/merchant')
            break
        case 'ADMIN':
            next('/admin')
            break
        default:
            next('/')
        }
    } 
    // 允许访问
    else {
        next()
    }
})

export default router