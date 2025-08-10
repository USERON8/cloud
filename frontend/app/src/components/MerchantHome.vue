<template>
  <div class="merchant-home">
    <el-card class="welcome-card">
      <div class="welcome-content">
        <h2>欢迎，{{ nickname }}！</h2>
        <p>您已成功登录云库存管理系统</p>
        <el-tag type="success">商家用户</el-tag>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" @click="goToStock">
          <div class="stat-content">
            <el-icon color="#409EFF" size="32">
              <Box/>
            </el-icon>
            <div class="stat-info">
              <h3>库存管理</h3>
              <p>管理商品库存</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" @click="goToOrders">
          <div class="stat-content">
            <el-icon color="#67C23A" size="32">
              <DataAnalysis/>
            </el-icon>
            <div class="stat-info">
              <h3>订单管理</h3>
              <p>查看处理订单</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" @click="goToProducts">
          <div class="stat-content">
            <el-icon color="#E6A23C" size="32">
              <Setting/>
            </el-icon>
            <div class="stat-info">
              <h3>商品管理</h3>
              <p>管理商品信息</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" @click="goToReports">
          <div class="stat-content">
            <el-icon color="#F56C6C" size="32">
              <TrendCharts/>
            </el-icon>
            <div class="stat-info">
              <h3>销售报表</h3>
              <p>查看销售数据</p>
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
              <span>最近活动</span>
            </div>
          </template>
          <div class="activity-list">
            <el-timeline>
              <el-timeline-item
                  v-for="(activity, index) in activities"
                  :key="index"
                  :timestamp="activity.timestamp"
                  placement="top"
              >
                <el-card>
                  <h4>{{ activity.title }}</h4>
                  <p>{{ activity.content }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="summary-card">
          <template #header>
            <div class="card-header">
              <span>数据概览</span>
            </div>
          </template>
          <div class="summary-content">
            <div class="summary-item">
              <span>商品总数</span>
              <strong>128</strong>
            </div>
            <div class="summary-item">
              <span>库存总量</span>
              <strong>5,420</strong>
            </div>
            <div class="summary-item">
              <span>今日订单</span>
              <strong>24</strong>
            </div>
            <div class="summary-item">
              <span>本月销售额</span>
              <strong>¥128,450</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="actions">
      <el-button type="primary" @click="logout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Box, DataAnalysis, Setting, TrendCharts} from '@element-plus/icons-vue'

const router = useRouter()
const nickname = ref('')

const activities = ref([
  {
    title: '新订单通知',
    content: '您有一笔新的订单等待处理',
    timestamp: '2025-08-07 10:30'
  },
  {
    title: '库存预警',
    content: '商品"A001"库存低于安全线，请及时补货',
    timestamp: '2025-08-07 09:15'
  },
  {
    title: '系统更新',
    content: '系统已完成版本更新，新增报表功能',
    timestamp: '2025-08-06 18:00'
  }
])

onMounted(() => {
  nickname.value = localStorage.getItem('nickname') || '商家'
})

const goToStock = () => {
  router.push('/stock')
}

const goToOrders = () => {
  ElMessage.info('订单管理功能开发中')
}

const goToProducts = () => {
  ElMessage.info('商品管理功能开发中')
}

const goToReports = () => {
  router.push('/statistics')
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
    router.push('/merchant/login')
  }).catch(() => {
    // 用户取消退出
  })
}
</script>

<style scoped>
.merchant-home {
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

.content-card, .summary-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-item strong {
  font-size: 18px;
  color: #409EFF;
}

.actions {
  text-align: center;
  margin-top: 20px;
}
</style>