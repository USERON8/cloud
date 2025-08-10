<template>
  <div class="user-dashboard">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>用户仪表板</span>
        </div>
      </template>
      <div class="welcome-content">
        <h2>欢迎，{{ nickname }}！</h2>
        <p>您已成功登录云库存管理系统。</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF">
              <Box/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ orderCount }}</div>
              <div class="stat-label">我的订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67C23A">
              <DataAnalysis/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ productCount }}</div>
              <div class="stat-label">关注商品</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C">
              <User/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ userInfo ? 'VIP' : '普通' }}</div>
              <div class="stat-label">用户等级</div>
            </div>
          </div>
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
import {onMounted, ref} from 'vue'
import {Box, DataAnalysis, User} from '@element-plus/icons-vue'
import {useAuthStore} from '../../store/modules/auth'
import {useUserStore} from '../../store/modules/user'
import {useOrderStore} from '../../store/modules/order'

const authStore = useAuthStore()
const userStore = useUserStore()
const orderStore = useOrderStore()

const nickname = ref(authStore.getNickname)
const userInfo = ref(null)
const orderCount = ref(0)
const productCount = ref(0)
const recentOrders = ref([])

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

// 加载用户信息
const loadUserInfo = async () => {
  try {
    await userStore.fetchUserInfo()
    userInfo.value = userStore.getUserInfo
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 加载订单统计
const loadOrderStats = async () => {
  try {
    // 这里应该调用API获取实际统计数据
    orderCount.value = 12
    productCount.value = 5
  } catch (error) {
    console.error('加载订单统计失败:', error)
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

onMounted(() => {
  loadUserInfo()
  loadOrderStats()
  loadRecentOrders()
})
</script>

<style scoped>
.user-dashboard {
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

.recent-orders {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>