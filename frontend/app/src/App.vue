<script setup>
import {useRoute, useRouter} from 'vue-router'
import {Box, DataAnalysis, House} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const handleMenuSelect = (index) => {
  const routes = {
    '1': '/',
    '2': '/stock',
    '3': '/statistics'
  }
  router.push(routes[index])
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
          <nav class="nav">
            <el-menu
                mode="horizontal"
                :default-active="route.path === '/' ? '1' : route.path === '/stock' ? '2' : '3'"
                class="nav-menu"
                @select="handleMenuSelect"
            >
              <el-menu-item index="1">
                <el-icon>
                  <House/>
                </el-icon>
                首页
              </el-menu-item>
              <el-menu-item index="2">
                <el-icon>
                  <Box/>
                </el-icon>
                库存管理
              </el-menu-item>
              <el-menu-item index="3">
                <el-icon>
                  <DataAnalysis/>
                </el-icon>
                统计报表
              </el-menu-item>
            </el-menu>
          </nav>
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
        <p>© 2025 云库存管理系统 - 专业的库存管理解决方案</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
}

.logo h1 {
  margin: 0;
  font-size: 28px;
  color: #fff;
  font-weight: 600;
}

.subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-left: 10px;
}

.nav-menu {
  background: transparent;
  border: none;
}

.nav-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.9);
  border-bottom: 2px solid transparent;
}

.nav-menu .el-menu-item:hover,
.nav-menu .el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border-bottom-color: #fff;
}

.main {
  flex: 1;
  padding: 0;
}

.footer {
  background-color: #2c3e50;
  color: #ecf0f1;
  text-align: center;
  padding: 20px 0;
  margin-top: 40px;
}

.footer p {
  margin: 0;
  font-size: 14px;
}
</style>
