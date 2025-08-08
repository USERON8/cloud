import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/user/Dashboard.vue'),
    meta: {
      title: '首页',
      requiresAuth: true,
      role: 'USER'
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: {
      title: '用户登录'
    }
  },
  {
    path: '/auth/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue'),
    meta: {
      title: '用户注册'
    }
  },
  {
    path: '/user',
    name: 'UserHome',
    component: () => import('../views/user/Dashboard.vue'),
    meta: {
      title: '用户主页',
      requiresAuth: true,
      role: 'USER'
    }
  },
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('../views/user/Profile.vue'),
    meta: {
      title: '个人资料',
      requiresAuth: true,
      role: 'USER'
    }
  },
  {
    path: '/merchant',
    name: 'MerchantHome',
    component: () => import('../views/merchant/Dashboard.vue'),
    meta: {
      title: '商户主页',
      requiresAuth: true,
      role: 'MERCHANT'
    }
  },
  {
    path: '/merchant/products',
    name: 'MerchantProducts',
    component: () => import('../views/merchant/Products.vue'),
    meta: {
      title: '产品管理',
      requiresAuth: true,
      role: 'MERCHANT'
    }
  },
  {
    path: '/admin',
    name: 'AdminHome',
    component: () => import('../views/admin/Dashboard.vue'),
    meta: {
      title: '管理后台',
      requiresAuth: true,
      role: 'ADMIN'
    }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../views/admin/Users.vue'),
    meta: {
      title: '用户管理',
      requiresAuth: true,
      role: 'ADMIN'
    }
  },
  {
    path: '/stock',
    name: 'StockManager',
    component: () => import('../components/StockManager.vue'),
    meta: {
      title: '库存管理',
      requiresAuth: true,
      role: ['MERCHANT', 'ADMIN']
    }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('../components/Statistics.vue'),
    meta: {
      title: '数据统计',
      requiresAuth: true,
      role: ['MERCHANT', 'ADMIN']
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - 云库存管理系统'
  }

  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    const userType = localStorage.getItem('userType')
    
    // 如果没有token，重定向到登录页
    if (!token) {
      next('/login')
      return
    }
    
    // 检查角色权限
    const requiredRoles = to.meta.role
    if (requiredRoles) {
      const roles = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles]
      if (!roles.includes(userType)) {
        // 如果没有权限，重定向到对应的主页
        if (userType === 'USER') {
          next('/user')
        } else if (userType === 'MERCHANT') {
          next('/merchant')
        } else if (userType === 'ADMIN') {
          next('/admin')
        } else {
          next('/login')
        }
        return
      }
    }
  }
  
  next()
})

export default router