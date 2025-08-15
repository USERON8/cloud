<template>
  <div class="home-container">
    <div class="header">
      <div class="logo">
        <span class="logo-text">云平台管理系统</span>
      </div>
      <div class="user-info" v-if="isAuthenticated || isAdminAuthenticated">
        <el-dropdown @command="handleUserCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="36" icon="UserFilled" class="user-avatar" />
            <span class="username">{{ currentUserInfo.nickname || currentUserInfo.username }}</span>
          </span>
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
      <div class="nav" v-else>
        <el-button type="primary" @click="goToLogin" round>用户登录</el-button>
        <el-button type="success" @click="goToRegister" round>用户注册</el-button>
        <el-button type="warning" @click="goToAdminLogin" round>管理员登录</el-button>
      </div>
    </div>
    
    <div class="main-content">
      <div class="hero-section">
        <div class="hero-content">
          <h1 class="hero-title">欢迎使用云平台管理系统</h1>
          <p v-if="isAuthenticated" class="hero-subtitle">您好，{{ currentUserInfo.nickname }}！</p>
          <p v-else-if="isAdminAuthenticated" class="hero-subtitle">您好，管理员 {{ currentUserInfo.nickname }}！</p>
          <p v-else class="hero-subtitle">一站式解决方案，助力您的业务发展</p>
          <div class="cta-buttons" v-if="!isAuthenticated && !isAdminAuthenticated">
            <el-button type="primary" size="large" @click="goToRegister" round>立即注册</el-button>
            <el-button type="default" size="large" @click="goToLogin" round>登录账户</el-button>
          </div>
          <div class="user-actions" v-else>
            <el-button v-if="isAuthenticated" type="primary" size="large" @click="goToDashboard" round>用户中心</el-button>
            <el-button v-if="isAdminAuthenticated" type="warning" size="large" @click="goToAdminDashboard" round>管理后台</el-button>
          </div>
        </div>
      </div>
      
      <div class="features-section">
        <div class="container">
          <h2 class="section-title text-center mb-20">核心功能</h2>
          <el-row :gutter="30">
            <el-col :span="8" class="feature-col">
              <div class="feature-card card">
                <div class="feature-icon">
                  <el-icon><Setting /></el-icon>
                </div>
                <h3>高效管理</h3>
                <p>提供直观的管理界面，轻松管理您的业务</p>
              </div>
            </el-col>
            <el-col :span="8" class="feature-col">
              <div class="feature-card card">
                <div class="feature-icon">
                  <el-icon><Lock /></el-icon>
                </div>
                <h3>安全保障</h3>
                <p>多层安全防护，保障您的数据安全</p>
              </div>
            </el-col>
            <el-col :span="8" class="feature-col">
              <div class="feature-card card">
                <div class="feature-icon">
                  <el-icon><DataLine /></el-icon>
                </div>
                <h3>数据分析</h3>
                <p>强大的数据分析功能，助您洞察业务趋势</p>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </div>
    
    <div class="footer">
      <div class="container">
        <p>© 2023 云平台管理系统. 保留所有权利.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Lock, DataLine, UserFilled, User, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 计算属性：是否为认证用户
const isAuthenticated = computed(() => userStore.isUserAuthenticated)

// 计算属性：是否为认证管理员
const isAdminAuthenticated = computed(() => userStore.isUserAdmin)

// 计算属性：当前用户信息
const currentUserInfo = computed(() => {
  return userStore.getUserInfo || userStore.getAdminInfo || {}
})

// 用户下拉菜单处理
const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人资料功能开发中...')
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中...')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/')
      }).catch(() => {
        // 用户取消操作
      })
      break
    default:
      break
  }
}

// 页面跳转函数
const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}

const goToAdminLogin = () => {
  router.push('/admin/login')
}

const goToDashboard = () => {
  router.push('/dashboard')
}

const goToAdminDashboard = () => {
  router.push('/admin/dashboard')
}
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  background-color: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.logo-text {
  font-size: 1.8rem;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-info {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.user-avatar {
  margin-right: 10px;
}

.username {
  font-weight: 500;
  color: var(--primary-text-color);
}

.nav {
  display: flex;
  gap: 15px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.hero-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
}

.hero-content {
  max-width: 800px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.hero-title {
  font-size: 2.5rem;
  margin-bottom: 20px;
  color: var(--primary-text-color);
}

.hero-subtitle {
  font-size: 1.2rem;
  margin-bottom: 30px;
  color: var(--regular-text-color);
}

.cta-buttons, .user-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.features-section {
  padding: 80px 0;
  background-color: var(--background-color);
}

.section-title {
  font-size: 2rem;
  margin-bottom: 50px;
  color: var(--primary-text-color);
}

.feature-col {
  display: flex;
}

.feature-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 30px;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-10px);
}

.feature-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  color: white;
}

.feature-icon .el-icon {
  font-size: 36px;
}

.feature-card h3 {
  font-size: 1.5rem;
  margin-bottom: 15px;
  color: var(--primary-text-color);
}

.feature-card p {
  font-size: 1rem;
  color: var(--regular-text-color);
  line-height: 1.6;
}

.footer {
  padding: 30px 0;
  background-color: rgba(0, 0, 0, 0.8);
  color: white;
  text-align: center;
}

.footer p {
  margin: 0;
  font-size: 1rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 15px 20px;
    flex-direction: column;
    gap: 15px;
  }
  
  .logo-text {
    font-size: 1.5rem;
  }
  
  .nav {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .hero-content {
    padding: 20px;
  }
  
  .hero-title {
    font-size: 1.8rem;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .cta-buttons, .user-actions {
    flex-direction: column;
    align-items: center;
    gap: 15px;
  }
  
  .features-section {
    padding: 40px 0;
  }
  
  .section-title {
    font-size: 1.5rem;
  }
  
  .el-col {
    margin-bottom: 20px;
  }
  
  .feature-card {
    padding: 20px;
  }
  
  .feature-icon {
    width: 60px;
    height: 60px;
  }
  
  .feature-icon .el-icon {
    font-size: 28px;
  }
}
</style>