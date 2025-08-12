<template>
  <div class="admin-dashboard">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>管理仪表板</span>
        </div>
      </template>
      <div class="welcome-content">
        <h2>欢迎，{{ nickname }}！</h2>
        <p>您已成功登录管理系统。</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF">
              <User/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67C23A">
              <Shop/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ merchantCount }}</div>
              <div class="stat-label">商户数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C">
              <Box/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ productCount }}</div>
              <div class="stat-label">商品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#F56C6C">
              <DataAnalysis/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">¥{{ totalRevenue }}</div>
              <div class="stat-label">平台总营收</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户增长趋势</span>
            </div>
          </template>
          <div ref="userGrowthChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户类型分布</span>
            </div>
          </template>
          <div ref="userTypeChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="tables-row">
      <el-col :span="12">
        <el-card class="recent-users">
          <template #header>
            <div class="card-header">
              <span>新增用户</span>
              <el-button link @click="viewAllUsers">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentUsers" style="width: 100%">
            <el-table-column label="用户ID" prop="id" width="80"/>
            <el-table-column label="用户名" prop="username"/>
            <el-table-column label="用户类型" prop="userType" width="100">
              <template #default="scope">
                <el-tag :type="getUserTypeTag(scope.row.userType)">
                  {{ scope.row.userType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" prop="createTime" width="180"/>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="system-info">
          <template #header>
            <div class="card-header">
              <span>系统信息</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
            <el-descriptions-item label="Vue版本">3.5.17</el-descriptions-item>
            <el-descriptions-item label="Element Plus版本">2.8.10</el-descriptions-item>
            <el-descriptions-item label="运行环境">Node.js v18.17.0</el-descriptions-item>
            <el-descriptions-item label="数据库">MySQL 8.0</el-descriptions-item>
            <el-descriptions-item label="缓存">Redis 6.2.7</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted, ref} from 'vue'
import * as echarts from 'echarts'
import {Box, DataAnalysis, Shop, User} from '@element-plus/icons-vue'
import {useAuthStore} from '@/store/index.js'

const authStore = useAuthStore()

const nickname = ref(authStore.getNickname)
const userCount = ref(0)
const merchantCount = ref(0)
const productCount = ref(0)
const totalRevenue = ref(0)
const recentUsers = ref([])

// 图表实例
const userGrowthChart = ref(null)
const userTypeChart = ref(null)
let userGrowthChartInstance = null
let userTypeChartInstance = null

// 获取用户类型标签类型
const getUserTypeTag = (userType) => {
  const typeMap = {
    'USER': 'primary',
    'MERCHANT': 'success',
    'ADMIN': 'danger'
  }
  return typeMap[userType] || 'info'
}

// 查看全部用户
const viewAllUsers = () => {
  // 跳转到用户管理页面
  console.log('查看全部用户')
}

// 初始化用户增长趋势图表
const initUserGrowthChart = () => {
  if (userGrowthChart.value) {
    userGrowthChartInstance = echarts.init(userGrowthChart.value)
    const option = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月']
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          data: [120, 200, 150, 80, 70, 110],
          type: 'line',
          smooth: true,
          areaStyle: {}
        }
      ]
    }
    userGrowthChartInstance.setOption(option)
  }
}

// 初始化用户类型分布图表
const initUserTypeChart = () => {
  if (userTypeChart.value) {
    userTypeChartInstance = echarts.init(userTypeChart.value)
    const option = {
      tooltip: {
        trigger: 'item'
      },
      legend: {
        top: '5%',
        left: 'center'
      },
      series: [
        {
          name: '用户类型',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '18',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            {value: 1048, name: '普通用户'},
            {value: 735, name: '商户'},
            {value: 580, name: '管理员'}
          ]
        }
      ]
    }
    userTypeChartInstance.setOption(option)
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    // 模拟统计数据
    userCount.value = 1248
    merchantCount.value = 86
    productCount.value = 542
    totalRevenue.value = 1286400
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载最近用户
const loadRecentUsers = async () => {
  try {
    // 模拟最近用户数据
    recentUsers.value = [
      {
        id: 1001,
        username: 'user001',
        userType: 'USER',
        createTime: '2023-10-01 14:30:00'
      },
      {
        id: 1002,
        username: 'merchant001',
        userType: 'MERCHANT',
        createTime: '2023-10-05 09:15:00'
      },
      {
        id: 1003,
        username: 'user002',
        userType: 'USER',
        createTime: '2023-10-10 16:45:00'
      }
    ]
  } catch (error) {
    console.error('加载最近用户失败:', error)
  }
}

// 窗口大小改变时重置图表
const handleResize = () => {
  if (userGrowthChartInstance) {
    userGrowthChartInstance.resize()
  }
  if (userTypeChartInstance) {
    userTypeChartInstance.resize()
  }
}

onMounted(() => {
  loadStats()
  loadRecentUsers()
  initUserGrowthChart()
  initUserTypeChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (userGrowthChartInstance) {
    userGrowthChartInstance.dispose()
  }
  if (userTypeChartInstance) {
    userTypeChartInstance.dispose()
  }
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.welcome-card {
  margin-bottom: 20px;
}

.welcome-content h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.welcome-content p {
  margin: 0;
  color: #606266;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 40px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 8px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.tables-row {
  margin-bottom: 20px;
}

.recent-users,
.system-info {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>