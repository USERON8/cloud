<template>
  <div class="merchant-dashboard">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>商户仪表板</span>
        </div>
      </template>
      <div class="welcome-content">
        <h2>欢迎，{{ nickname }}！</h2>
        <p>您已成功登录商户管理系统。</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF">
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
            <el-icon class="stat-icon" color="#67C23A">
              <DataAnalysis/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ orderCount }}</div>
              <div class="stat-label">订单总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C">
              <User/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ customerCount }}</div>
              <div class="stat-label">客户数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#F56C6C">
              <Shop/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">¥{{ totalRevenue }}</div>
              <div class="stat-label">总营收</div>
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
              <span>销售趋势</span>
            </div>
          </template>
          <div ref="salesChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>商品分类占比</span>
            </div>
          </template>
          <div ref="categoryChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="recent-orders">
      <template #header>
        <div class="card-header">
          <span>最近订单</span>
          <el-button link @click="viewAllOrders">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentOrders" style="width: 100%">
        <el-table-column label="订单号" prop="id" width="180"/>
        <el-table-column label="商品名称" prop="productName"/>
        <el-table-column label="数量" prop="quantity" width="100"/>
        <el-table-column label="总价" prop="totalPrice" width="100"/>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="getOrderStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted, ref} from 'vue'
import * as echarts from 'echarts'
import {Box, DataAnalysis, Shop, User} from '@element-plus/icons-vue'
import {useAuthStore} from '../../store/modules/auth'

const authStore = useAuthStore()

const nickname = ref(authStore.getNickname)
const productCount = ref(0)
const orderCount = ref(0)
const customerCount = ref(0)
const totalRevenue = ref(0)
const recentOrders = ref([])

// 图表实例
const salesChart = ref(null)
const categoryChart = ref(null)
let salesChartInstance = null
let categoryChartInstance = null

// 获取订单状态标签类型
const getOrderStatusType = (status) => {
  const statusMap = {
    '待支付': 'warning',
    '已支付': 'success',
    '已发货': 'primary',
    '已完成': 'success',
    '已取消': 'danger'
  }
  return statusMap[status] || 'info'
}

// 查看全部订单
const viewAllOrders = () => {
  // 跳转到订单页面
  console.log('查看全部订单')
}

// 初始化销售趋势图表
const initSalesChart = () => {
  if (salesChart.value) {
    salesChartInstance = echarts.init(salesChart.value)
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
          smooth: true
        }
      ]
    }
    salesChartInstance.setOption(option)
  }
}

// 初始化分类占比图表
const initCategoryChart = () => {
  if (categoryChart.value) {
    categoryChartInstance = echarts.init(categoryChart.value)
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
          name: '商品分类',
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
            {value: 1048, name: '电子产品'},
            {value: 735, name: '服装'},
            {value: 580, name: '家居'},
            {value: 484, name: '食品'},
            {value: 300, name: '图书'}
          ]
        }
      ]
    }
    categoryChartInstance.setOption(option)
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    // 模拟统计数据
    productCount.value = 42
    orderCount.value = 128
    customerCount.value = 86
    totalRevenue.value = 128640
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载最近订单
const loadRecentOrders = async () => {
  try {
    // 模拟最近订单数据
    recentOrders.value = [
      {
        id: 'ORD20230001',
        productName: 'iPhone 15 Pro',
        quantity: 1,
        totalPrice: '8999.00',
        status: '已完成',
        createTime: '2023-10-01 14:30:00'
      },
      {
        id: 'ORD20230002',
        productName: 'MacBook Air M2',
        quantity: 1,
        totalPrice: '9499.00',
        status: '已发货',
        createTime: '2023-10-05 09:15:00'
      },
      {
        id: 'ORD20230003',
        productName: 'AirPods Pro',
        quantity: 2,
        totalPrice: '3498.00',
        status: '待支付',
        createTime: '2023-10-10 16:45:00'
      }
    ]
  } catch (error) {
    console.error('加载最近订单失败:', error)
  }
}

// 窗口大小改变时重置图表
const handleResize = () => {
  if (salesChartInstance) {
    salesChartInstance.resize()
  }
  if (categoryChartInstance) {
    categoryChartInstance.resize()
  }
}

onMounted(() => {
  loadStats()
  loadRecentOrders()
  initSalesChart()
  initCategoryChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (salesChartInstance) {
    salesChartInstance.dispose()
  }
  if (categoryChartInstance) {
    categoryChartInstance.dispose()
  }
})
</script>

<style scoped>
.merchant-dashboard {
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

.recent-orders {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>