<template>
  <el-container class="admin-dashboard">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
      <div class="logo" :class="{ 'collapsed': isCollapse }">
        <transition name="fade" mode="out-in">
          <span v-if="!isCollapse" key="full">管理系统</span>
          <span v-else key="mini">管</span>
        </transition>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapse"
        :collapse-transition="false"
        @select="handleMenuSelect"
      >
        <el-menu-item index="dashboard">
          <el-icon><House /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="merchants">
          <el-icon><Shop /></el-icon>
          <template #title>商家审核</template>
        </el-menu-item>
        <el-menu-item index="products">
          <el-icon><Goods /></el-icon>
          <template #title>商品管理</template>
        </el-menu-item>
        <el-menu-item index="orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="statistics">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据统计</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 头部 -->
      <el-header class="header">
        <div class="header-left">
          <el-button link @click="toggleCollapse" class="collapse-btn">
            <el-icon><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
          </el-button>
          <h2 class="page-title">{{ menuTitles[activeMenu] }}</h2>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleUserCommand">
            <span class="user-info">
              <el-avatar :size="36" icon="UserFilled" class="user-avatar" />
              <span v-if="!isCollapse" class="username">{{ adminInfo.nickname || adminInfo.username }}</span>
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
      </el-header>

      <!-- 主体内容 -->
      <el-main class="main-content">
        <transition name="slide" mode="out-in">
          <div v-if="activeMenu === 'dashboard'" class="dashboard-content" key="dashboard">
            <el-row :gutter="20" class="stats-row">
              <el-col :span="6" v-for="stat in statsData" :key="stat.id">
                <div class="stat-card card">
                  <div class="stat-header">
                    <el-icon class="stat-icon" :style="{ color: stat.color }"><component :is="stat.icon" /></el-icon>
                    <div class="stat-value">{{ stat.value }}</div>
                  </div>
                  <div class="stat-footer">{{ stat.title }}</div>
                </div>
              </el-col>
            </el-row>
            
            <el-row :gutter="20" class="mt-20">
              <el-col :span="16">
                <div class="chart-card card">
                  <div class="chart-header">
                    <h3>用户增长趋势</h3>
                  </div>
                  <div class="chart-container">
                    <div class="chart-placeholder">
                      <el-icon size="3em"><DataLine /></el-icon>
                      <p>图表占位符</p>
                    </div>
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="recent-activity card">
                  <div class="activity-header">
                    <h3>最近活动</h3>
                  </div>
                  <div class="activity-list">
                    <div class="activity-item" v-for="activity in recentActivities" :key="activity.id">
                      <div class="activity-icon">
                        <el-icon><component :is="activity.icon" /></el-icon>
                      </div>
                      <div class="activity-content">
                        <div class="activity-title">{{ activity.title }}</div>
                        <div class="activity-time">{{ activity.time }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
          
          <div v-else-if="activeMenu === 'users'" class="users-content" key="users">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>用户管理</span>
                  <el-button type="primary" size="small">添加用户</el-button>
                </div>
              </template>
              <div class="content-placeholder">
                <el-icon size="3em"><User /></el-icon>
                <p>用户管理功能开发中...</p>
              </div>
            </el-card>
          </div>
          
          <div v-else-if="activeMenu === 'merchants'" class="merchants-content" key="merchants">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>商家审核</span>
                  <el-button type="primary" size="small">审核列表</el-button>
                </div>
              </template>
              <div class="content-placeholder">
                <el-icon size="3em"><Shop /></el-icon>
                <p>商家审核功能开发中...</p>
              </div>
            </el-card>
          </div>
          
          <div v-else class="other-content" :key="activeMenu">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>{{ menuTitles[activeMenu] }}</span>
                </div>
              </template>
              <div class="content-placeholder">
                <el-icon size="3em"><component :is="getCurrentIcon()" /></el-icon>
                <p>{{ menuTitles[activeMenu] }}功能开发中...</p>
              </div>
            </el-card>
          </div>
        </transition>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  House, User, Shop, Goods, Document, DataAnalysis, 
  UserFilled, Setting, SwitchButton, Expand, Fold,
  DataLine, Bell, Check, Warning
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapse = ref(false)

// 当前激活的菜单项
const activeMenu = ref('dashboard')

// 管理员信息
const adminInfo = computed(() => userStore.getAdminInfo || {})

// 菜单标题映射
const menuTitles = {
  dashboard: '仪表盘',
  users: '用户管理',
  merchants: '商家审核',
  products: '商品管理',
  orders: '订单管理',
  statistics: '数据统计'
}

// 统计数据
const statsData = reactive([
  { id: 1, title: '用户总数', value: '1,234', icon: 'User', color: '#409eff' },
  { id: 2, title: '商家数量', value: '56', icon: 'Shop', color: '#67c23a' },
  { id: 3, title: '商品数量', value: '5,678', icon: 'Goods', color: '#e6a23c' },
  { id: 4, title: '订单总数', value: '12,345', icon: 'Document', color: '#f56c6c' }
])

// 最近活动
const recentActivities = reactive([
  { id: 1, title: '新用户注册', time: '5分钟前', icon: 'User' },
  { id: 2, title: '订单已确认', time: '15分钟前', icon: 'Check' },
  { id: 3, title: '商家申请', time: '30分钟前', icon: 'Shop' },
  { id: 4, title: '系统通知', time: '1小时前', icon: 'Bell' },
  { id: 5, title: '安全警告', time: '2小时前', icon: 'Warning' }
])

// 切换侧边栏折叠状态
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 处理菜单选择
const handleMenuSelect = (index: string) => {
  activeMenu.value = index
}

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
        router.push('/admin/login')
      }).catch(() => {
        // 用户取消操作
      })
      break
    default:
      break
  }
}

// 获取当前菜单项对应的图标
const getCurrentIcon = () => {
  const iconMap = {
    products: Goods,
    orders: Document,
    statistics: DataAnalysis
  }
  return iconMap[activeMenu.value as keyof typeof iconMap] || House
}
</script>

<style scoped>
.admin-dashboard {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s ease;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2c3e50;
  color: white;
  font-size: 1.2rem;
  font-weight: bold;
  transition: all 0.3s ease;
}

.logo.collapsed {
  justify-content: center;
}

.sidebar-menu {
  border-right: none;
  background-color: #304156;
}

.sidebar-menu :deep(.el-menu-item) {
  background-color: #304156;
  color: #bfcbd9;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #2c3e50;
  color: #fff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #409eff;
  color: #fff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: var(--card-background);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-bottom: 1px solid var(--border-color);
}

.collapse-btn {
  font-size: 1.2rem;
  margin-right: 20px;
}

.page-title {
  margin: 0;
  font-size: 1.2rem;
}

.user-info {
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

.main-content {
  background-color: var(--background-color);
  padding: 20px;
  overflow-y: auto;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: var(--card-background);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.stat-icon {
  font-size: 2rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: bold;
  color: var(--primary-text-color);
}

.stat-footer {
  font-size: 0.9rem;
  color: var(--secondary-text-color);
}

.chart-card, .recent-activity {
  padding: 20px;
}

.chart-header, .activity-header {
  margin-bottom: 20px;
}

.chart-header h3, .activity-header h3 {
  margin: 0;
  color: var(--primary-text-color);
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-placeholder {
  text-align: center;
  color: var(--secondary-text-color);
}

.activity-list {
  max-height: 300px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  padding: 15px 0;
  border-bottom: 1px solid var(--border-color);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ecf5ff;
  border-radius: 50%;
  color: #409eff;
  margin-right: 15px;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-weight: 500;
  margin-bottom: 5px;
  color: var(--primary-text-color);
}

.activity-time {
  font-size: 0.85rem;
  color: var(--secondary-text-color);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-placeholder {
  text-align: center;
  padding: 50px 0;
  color: var(--secondary-text-color);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .el-col {
    margin-bottom: 20px;
  }
  
  .el-col-6 {
    width: 50%;
  }
  
  .el-col-16, .el-col-8 {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .el-col-6 {
    width: 100%;
  }
  
  .header {
    padding: 0 10px;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .username {
    display: none;
  }
}
</style>