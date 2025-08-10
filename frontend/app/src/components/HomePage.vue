<template>
  <div class="home-page">
    <div class="hero-section">
      <h1>欢迎使用云库存管理系统</h1>
      <p>基于Spring Cloud Alibaba的微服务架构，为您提供高效、稳定的库存管理解决方案</p>
      <div class="cta-buttons">
        <el-button size="large" type="primary" @click="goToLogin">用户登录</el-button>
        <el-button size="large" type="success" @click="goToMerchantLogin">商家登录</el-button>
      </div>
    </div>

    <div class="features-section">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="feature-card">
            <el-icon color="#409EFF" size="48">
              <Box/>
            </el-icon>
            <h3>库存管理</h3>
            <p>实时监控库存状态，支持多仓库管理，自动预警低库存商品</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card">
            <el-icon color="#67C23A" size="48">
              <DataAnalysis/>
            </el-icon>
            <h3>数据分析</h3>
            <p>提供多维度数据统计和分析报表，助力业务决策</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card">
            <el-icon color="#E6A23C" size="48">
              <Setting/>
            </el-icon>
            <h3>系统配置</h3>
            <p>灵活的系统配置，支持多角色权限管理，保障数据安全</p>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="tech-stack">
      <h2>技术架构</h2>
      <el-row :gutter="20">
        <el-col v-for="tech in techStack" :key="tech.name" :span="6">
          <div class="tech-card">
            <el-icon :color="tech.color" size="32">
              <component :is="tech.icon"/>
            </el-icon>
            <h4>{{ tech.name }}</h4>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import {useRouter} from 'vue-router'
import {Box, DataAnalysis, Setting} from '@element-plus/icons-vue'

const router = useRouter()

const goToLogin = () => {
  router.push('/login')
}

const goToMerchantLogin = () => {
  router.push('/merchant/login')
}

const techStack = [
  {name: 'Spring Boot', icon: 'Box', color: '#6DB33F'},
  {name: 'Spring Cloud', icon: 'DataAnalysis', color: '#59B3A9'},
  {name: 'Nacos', icon: 'Setting', color: '#00BCD4'},
  {name: 'Vue 3', icon: 'Box', color: '#42B883'}
]
</script>

<style scoped>
.home-page {
  padding: 20px;
}

.hero-section {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  color: white;
  margin-bottom: 30px;
}

.hero-section h1 {
  font-size: 36px;
  margin-bottom: 20px;
}

.hero-section p {
  font-size: 18px;
  margin-bottom: 30px;
  opacity: 0.9;
}

.cta-buttons .el-button {
  margin: 0 10px;
}

.features-section {
  margin-bottom: 40px;
}

.feature-card {
  text-align: center;
  padding: 30px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
}

.feature-card h3 {
  margin: 20px 0 10px;
  color: #333;
}

.feature-card p {
  color: #666;
  line-height: 1.6;
}

.tech-stack {
  text-align: center;
  padding: 30px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.tech-stack h2 {
  margin-bottom: 30px;
  color: #333;
}

.tech-card {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.tech-card:hover {
  background: #e9ecef;
  transform: scale(1.05);
}

.tech-card h4 {
  margin: 15px 0 0;
  color: #495057;
}
</style>