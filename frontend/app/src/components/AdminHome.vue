<template>
  <div class="admin-home">
    <el-card class="welcome-card">
      <div class="welcome-content">
        <h2>欢迎，{{ nickname }}！</h2>
        <p>您已成功登录云库存管理系统</p>
        <el-tag type="danger">系统管理员</el-tag>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon color="#409EFF" size="32">
              <User/>
            </el-icon>
            <div class="stat-info">
              <h3>用户管理</h3>
              <p>管理所有用户</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon color="#67C23A" size="32">
              <Shop/>
            </el-icon>
            <div class="stat-info">
              <h3>商家管理</h3>
              <p>管理商家账户</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon color="#E6A23C" size="32">
              <DataAnalysis/>
            </el-icon>
            <div class="stat-info">
              <h3>系统监控</h3>
              <p>监控系统状态</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon color="#F56C6C" size="32">
              <Setting/>
            </el-icon>
            <div class="stat-info">
              <h3>系统配置</h3>
              <p>配置系统参数</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span>系统概览</span>
            </div>
          </template>
          <div class="system-overview">
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="overview-item">
                  <h4>用户总数</h4>
                  <p class="overview-value">1,248</p>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-item">
                  <h4>商家总数</h4>
                  <p class="overview-value">156</p>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-item">
                  <h4>商品总数</h4>
                  <p class="overview-value">8,742</p>
                </div>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="overview-item">
                  <h4>今日订单</h4>
                  <p class="overview-value">1,204</p>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-item">
                  <h4>系统状态</h4>
                  <p class="overview-status">
                    <el-tag type="success">正常运行</el-tag>
                  </p>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-item">
                  <h4>在线用户</h4>
                  <p class="overview-value">243</p>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="logs-card">
          <template #header>
            <div class="card-header">
              <span>最近操作</span>
            </div>
          </template>
          <div class="logs-content">
            <el-timeline>
              <el-timeline-item
                  v-for="(log, index) in logs"
                  :key="index"
                  :timestamp="log.timestamp"
                  placement="top"
              >
                <p>{{ log.content }}</p>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="admin-functions">
      <template #header>
        <div class="card-header">
          <span>管理功能</span>
        </div>
      </template>
      <div class="functions-grid">
        <el-row :gutter="20">
          <el-col v-for="func in adminFunctions" :key="func.name" :span="6">
            <div class="function-item" @click="handleFunctionClick(func)">
              <el-icon size="24">{{ func.icon }}</el-icon>
              <span>{{ func.name }}</span>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <div class="actions">
      <el-button type="primary" @click="logout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Bell, Box, DataAnalysis, Document, Setting, Shop, TrendCharts, User} from '@element-plus/icons-vue'

const router = useRouter()
const nickname = ref('')

const logs = ref([
  {
    content: '用户"张三"登录系统',
    timestamp: '2025-08-07 10:30'
  },
  {
    content: '商家"李四商店"更新了商品信息',
    timestamp: '2025-08-07 09:45'
  },
  {
    content: '系统执行了每日数据备份',
    timestamp: '2025-08-07 02:00'
  },
  {
    content: '管理员"王五"修改了系统配置',
    timestamp: '2025-08-06 16:20'
  }
])

const adminFunctions = ref([
  {name: '用户管理', icon: User, path: '/admin/users'},
  {name: '商家管理', icon: Shop, path: '/admin/merchants'},
  {name: '商品管理', icon: Box, path: '/admin/products'},
  {name: '订单管理', icon: Document, path: '/admin/orders'},
  {name: '数据统计', icon: TrendCharts, path: '/admin/statistics'},
  {name: '系统日志', icon: Bell, path: '/admin/logs'},
  {name: '权限管理', icon: Setting, path: '/admin/permissions'},
  {name: '系统设置', icon: Setting, path: '/admin/settings'}
])

onMounted(() => {
  nickname.value = localStorage.getItem('nickname') || '管理员'
})

const handleFunctionClick = (func) => {
  ElMessage.info(`${func.name}功能开发中`)
}

const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 清除本地存储的用户信息
    localStorage.removeItem('token')
    localStorage.removeItem('userType')
    localStorage.removeItem('nickname')

    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {
    // 用户取消退出
  })
}
</script>

<style scoped>
.admin-home {
  padding: 20px;
}

.welcome-card {
  margin-bottom: 20px;
}

.welcome-content h2 {
  margin: 0 0 10px;
  color: #333;
}

.welcome-content p {
  margin: 0 0 15px;
  color: #666;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-info {
  margin-left: 15px;
}

.stat-info h3 {
  margin: 0 0 5px;
  color: #333;
}

.stat-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.content-card, .logs-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.system-overview .el-row {
  margin-bottom: 20px;
}

.overview-item {
  text-align: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.overview-item h4 {
  margin: 0 0 10px;
  color: #666;
  font-size: 14px;
}

.overview-value {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.overview-status {
  margin: 0;
}

.logs-content {
  max-height: 300px;
  overflow-y: auto;
}

.admin-functions {
  margin-bottom: 20px;
}

.functions-grid {
  padding: 10px 0;
}

.function-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 10px;
  background: #f8f9fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 15px;
}

.function-item:hover {
  background: #e9ecef;
  transform: translateY(-2px);
}

.function-item .el-icon {
  margin-bottom: 8px;
}

.function-item span {
  font-size: 14px;
  color: #333;
}

.actions {
  text-align: center;
  margin-top: 20px;
}
</style>