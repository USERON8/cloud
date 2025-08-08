<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Box, DataAnalysis, House, Setting, Shop, User } from '@element-plus/icons-vue'
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from './store/modules/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoggedIn = ref(false)
const userType = ref('')
const nickname = ref('')

// 检查登录状态
const checkLoginStatus = () => {
  isLoggedIn.value = authStore.isLogged
  userType.value = authStore.getUserType
  nickname.value = authStore.getNickname
}

// 监听路由变化
watch(route, () => {
  checkLoginStatus()
})

// 组件挂载时检查登录状态
onMounted(() => {
  checkLoginStatus()
})

const handleMenuSelect = (index) => {
  const routes = {
    '1': '/',
    '2-1': '/merchant/products',
    '2-2': '/stock',
    '3-1': '/statistics',
    '3-2': '/admin/users',
    '4-1': '/user',
    '4-2': '/user/profile',
    '5-1': '/merchant',
    '5-2': '/merchant/products',
    '6-1': '/admin',
    '6-2': '/admin/users'
  }

  if (routes[index]) {
    router.push(routes[index])
  }
}

const logout = async () => {
  try {
    // 使用Pinia状态管理登出
    await authStore.logout()
  } catch (error) {
    console.error('登出失败:', error)
  }
}
</script>

<template>
  <div class="app">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <div class="logo">
            <h1>云库存管理系统</h1>
            <span class="subtitle">基于Spring Cloud Alibaba的微服务架构</span>
          </div>

          <nav class="nav" v-if="isLoggedIn">
            <el-menu
                mode="horizontal"
                :default-active="route.path === '/' ? '1' : 
                                 route.path === '/user' ? '4-1' :
                                 route.path === '/user/profile' ? '4-2' :
                                 route.path === '/merchant' ? '5-1' :
                                 route.path === '/merchant/products' ? '2-1' :
                                 route.path === '/admin' ? '6-1' :
                                 route.path === '/admin/users' ? '3-2' : '1'"
                class="nav-menu"
                @select="handleMenuSelect"
            >
              <el-menu-item index="1">
                <el-icon>
                  <House/>
                </el-icon>
                首页
              </el-menu-item>

              <el-sub-menu index="2" v-if="userType === 'MERCHANT' || userType === 'ADMIN'">
                <template #title>
                  <el-icon>
                    <Box/>
                  </el-icon>
                  库存管理
                </template>
                <el-menu-item index="2-1" v-if="userType === 'MERCHANT'">产品管理</el-menu-item>
                <el-menu-item index="2-2">库存明细</el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="3" v-if="userType === 'MERCHANT' || userType === 'ADMIN'">
                <template #title>
                  <el-icon>
                    <DataAnalysis/>
                  </el-icon>
                  统计报表
                </template>
                <el-menu-item index="3-1">销售统计</el-menu-item>
                <el-menu-item index="3-2" v-if="userType === 'ADMIN'">用户管理</el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="4" v-if="userType === 'USER'">
                <template #title>
                  <el-icon>
                    <User/>
                  </el-icon>
                  用户中心
                </template>
                <el-menu-item index="4-1">仪表板</el-menu-item>
                <el-menu-item index="4-2">个人资料</el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="5" v-if="userType === 'MERCHANT'">
                <template #title>
                  <el-icon>
                    <Shop/>
                  </el-icon>
                  商家中心
                </template>
                <el-menu-item index="5-1">仪表板</el-menu-item>
                <el-menu-item index="5-2">产品管理</el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="6" v-if="userType === 'ADMIN'">
                <template #title>
                  <el-icon>
                    <Setting/>
                  </el-icon>
                  管理中心
                </template>
                <el-menu-item index="6-1">仪表板</el-menu-item>
                <el-menu-item index="6-2">用户管理</el-menu-item>
              </el-sub-menu>
            </el-menu>
          </nav>

          <div class="user-info" v-if="isLoggedIn">
            <el-dropdown>
              <div class="user-dropdown">
                <span class="user-name">{{ nickname }}</span>
                <el-icon>
                  <ArrowDown/>
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <div class="auth-links" v-else>
            <el-button type="primary" @click="router.push('/login')">登录</el-button>
          </div>
        </div>
      </div>
    </header>

    <main class="main">
      <div class="container">
        <router-view/>
      </div>
    </main>

    <footer class="footer">
      <div class="container">
        <p>&copy; 2023 云库存管理系统. 基于Spring Cloud Alibaba的微服务架构.</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
}

.logo h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.logo .subtitle {
  font-size: 12px;
  color: #909399;
}

.nav-menu {
  border: none !important;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-dropdown:hover {
  background-color: #f5f7fa;
}

.user-name {
  margin-right: 5px;
  font-size: 14px;
}

.auth-links {
  display: flex;
  align-items: center;
}

.main {
  flex: 1;
  padding: 20px 0;
}

.footer {
  background: #f5f7fa;
  padding: 20px 0;
  text-align: center;
  margin-top: auto;
}

.footer p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
</style>