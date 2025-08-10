<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="container">
        <div class="header-content">
          <!-- Logo -->
          <div class="logo" @click="$router.push('/')">
            <div class="logo-icon">
              <el-icon size="24">
                <Box />
              </el-icon>
            </div>
            <div class="logo-text">
              <h1>云库存管理系统</h1>
              <p class="logo-subtitle">基于Spring Cloud Alibaba的微服务架构</p>
            </div>
          </div>

          <!-- 导航菜单 -->
          <nav v-if="isLoggedIn" class="nav-menu">
            <el-menu
              :default-active="activeMenu"
              mode="horizontal"
              @select="handleMenuSelect"
              background-color="transparent"
              text-color="#fff"
              active-text-color="#409eff"
            >
              <el-menu-item index="home">
                <el-icon><House /></el-icon>
                <span>首页</span>
              </el-menu-item>

              <el-sub-menu v-if="userType === 'MERCHANT' || userType === 'ADMIN'" index="inventory">
                <template #title>
                  <el-icon><Box /></el-icon>
                  <span>库存管理</span>
                </template>
                <el-menu-item v-if="userType === 'MERCHANT'" index="products">产品管理</el-menu-item>
                <el-menu-item index="stock">库存明细</el-menu-item>
              </el-sub-menu>

              <el-sub-menu v-if="userType === 'MERCHANT' || userType === 'ADMIN'" index="analytics">
                <template #title>
                  <el-icon><DataAnalysis /></el-icon>
                  <span>统计报表</span>
                </template>
                <el-menu-item index="statistics">销售统计</el-menu-item>
                <el-menu-item v-if="userType === 'ADMIN'" index="users">用户管理</el-menu-item>
              </el-sub-menu>

              <el-sub-menu v-if="userType === 'USER'" index="user-center">
                <template #title>
                  <el-icon><User /></el-icon>
                  <span>用户中心</span>
                </template>
                <el-menu-item index="user-dashboard">仪表板</el-menu-item>
                <el-menu-item index="user-profile">个人资料</el-menu-item>
              </el-sub-menu>

              <el-sub-menu v-if="userType === 'MERCHANT'" index="merchant-center">
                <template #title>
                  <el-icon><Shop /></el-icon>
                  <span>商家中心</span>
                </template>
                <el-menu-item index="merchant-dashboard">仪表板</el-menu-item>
                <el-menu-item index="merchant-products">产品管理</el-menu-item>
              </el-sub-menu>

              <el-sub-menu v-if="userType === 'ADMIN'" index="admin-center">
                <template #title>
                  <el-icon><Setting /></el-icon>
                  <span>管理中心</span>
                </template>
                <el-menu-item index="admin-dashboard">仪表板</el-menu-item>
                <el-menu-item index="admin-users">用户管理</el-menu-item>
              </el-sub-menu>
            </el-menu>
          </nav>

          <!-- 用户信息 -->
          <div v-if="isLoggedIn" class="user-actions">
            <el-dropdown @command="handleUserCommand">
              <div class="user-info">
                <el-avatar :size="32" icon="UserFilled" />
                <span class="user-name">{{ nickname }}</span>
                <el-icon class="arrow-down">
                  <ArrowDown />
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人资料
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>
                    系统设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <!-- 登录按钮 -->
          <div v-else class="auth-actions">
            <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          </div>
        </div>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="app-main">
      <div class="container">
        <router-view />
      </div>
    </main>

    <!-- 底部 -->
    <footer class="app-footer">
      <div class="container">
        <div class="footer-content">
          <p>&copy; 2023 云库存管理系统. 基于Spring Cloud Alibaba的微服务架构.</p>
          <div class="footer-links">
            <el-link type="info" :underline="false">帮助文档</el-link>
            <el-link type="info" :underline="false">技术支持</el-link>
            <el-link type="info" :underline="false">隐私政策</el-link>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import { 
  Box, 
  House, 
  DataAnalysis, 
  User, 
  Shop, 
  Setting, 
  ArrowDown,
  UserFilled,
  SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const isLoggedIn = computed(() => authStore.isAuthenticated)
const userType = computed(() => authStore.userType)
const nickname = computed(() => authStore.nickname)

// 计算当前激活的菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path === '/') return 'home'
  if (path === '/stock') return 'stock'
  if (path === '/statistics') return 'statistics'
  if (path === '/admin/users') return 'admin-users'
  if (path === '/user') return 'user-dashboard'
  if (path === '/user/profile') return 'user-profile'
  if (path === '/merchant') return 'merchant-dashboard'
  if (path === '/merchant/products') return 'merchant-products'
  if (path === '/admin') return 'admin-dashboard'
  return 'home'
})

// 处理菜单选择
const handleMenuSelect = (index) => {
  const routes = {
    'home': '/',
    'products': '/merchant/products',
    'stock': '/stock',
    'statistics': '/statistics',
    'users': '/admin/users',
    'user-dashboard': '/user',
    'user-profile': '/user/profile',
    'merchant-dashboard': '/merchant',
    'merchant-products': '/merchant/products',
    'admin-dashboard': '/admin',
    'admin-users': '/admin/users'
  }

  if (routes[index]) {
    router.push(routes[index])
  }
}

// 处理用户操作
const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'settings':
      // 跳转到系统设置页面
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 处理登出
const handleLogout = async () => {
  try {
    await authStore.logout()
    router.push('/login')
  } catch (error) {
    console.error('登出失败:', error)
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: var(--header-height);
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: var(--transition);
}

.logo:hover {
  opacity: 0.9;
}

.logo-icon {
  color: white;
  margin-right: 12px;
}

.logo-text h1 {
  margin: 0;
  font-size: 20px;
  color: white;
  font-weight: 500;
}

.logo-subtitle {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.nav-menu {
  flex: 1;
  margin: 0 20px;
}

.nav-menu :deep(.el-menu) {
  background: transparent;
  border: none;
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item) {
  height: var(--header-height);
  line-height: var(--header-height);
  border: none;
  color: rgba(255, 255, 255, 0.85);
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item.is-active) {
  color: white;
  border-bottom: 2px solid white;
}

.nav-menu :deep(.el-sub-menu__title) {
  height: var(--header-height);
  line-height: var(--header-height);
  color: rgba(255, 255, 255, 0.85);
  border: none;
}

.nav-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.user-actions {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 20px;
  transition: var(--transition);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.1);
}

.user-name {
  margin: 0 5px;
  color: white;
  font-size: 14px;
}

.arrow-down {
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

.auth-actions {
  display: flex;
  align-items: center;
}

.app-main {
  flex: 1;
  padding: 20px 0;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.app-footer {
  background: #f5f7fa;
  border-top: 1px solid #ebeef5;
  padding: 20px 0;
  margin-top: auto;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-content p {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 14px;
}

.footer-links {
  display: flex;
  gap: 20px;
}

.footer-links :deep(.el-link) {
  color: var(--text-color-secondary);
}

.footer-links :deep(.el-link:hover) {
  color: var(--primary-color);
}

/* 响应式设计 */
@media (max-width: 992px) {
  .logo-text h1 {
    font-size: 18px;
  }
  
  .logo-subtitle {
    display: none;
  }
  
  .nav-menu {
    margin: 0 10px;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 10px;
  }
  
  .nav-menu {
    display: none;
  }
  
  .footer-content {
    flex-direction: column;
    gap: 10px;
  }
}
</style>