<template>
  <div class="user-home">
    <el-card class="welcome-card">
      <div class="welcome-content">
        <h2>欢迎，{{ nickname }}！</h2>
        <p>您已成功登录云库存管理系统</p>
        <el-tag type="success">普通用户</el-tag>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon color="#409EFF" size="32">
              <Box/>
            </el-icon>
            <div class="stat-info">
              <h3>我的库存</h3>
              <p>查看个人库存信息</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon color="#67C23A" size="32">
              <DataAnalysis/>
            </el-icon>
            <div class="stat-info">
              <h3>数据分析</h3>
              <p>查看个人使用数据</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon color="#E6A23C" size="32">
              <Setting/>
            </el-icon>
            <div class="stat-info">
              <h3>个人设置</h3>
              <p>管理个人账户信息</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>功能介绍</span>
        </div>
      </template>
      <div class="content-body">
        <p>作为普通用户，您可以：</p>
        <ul>
          <li>查看个人库存信息</li>
          <li>查看个人使用数据和统计</li>
          <li>管理个人账户设置</li>
          <li>查看系统公告和消息</li>
        </ul>
        <p>请注意，您无法访问商家或管理员专属功能。</p>
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
import {Box, DataAnalysis, Setting} from '@element-plus/icons-vue'

const router = useRouter()
const nickname = ref('')

onMounted(() => {
  nickname.value = localStorage.getItem('nickname') || '用户'
})

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
.user-home {
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

.content-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.content-body ul {
  padding-left: 20px;
}

.content-body li {
  margin: 10px 0;
  line-height: 1.6;
}

.actions {
  text-align: center;
}
</style>