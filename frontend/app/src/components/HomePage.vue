<template>
  <div class="home-page">
    <!-- 主要横幅区域 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">云库存管理系统</h1>
        <p class="hero-subtitle">基于Spring Cloud Alibaba的微服务架构，为您提供高效、稳定的库存管理解决方案</p>
        <div class="cta-buttons">
          <el-button size="large" type="primary" @click="goToLogin" round>
            <el-icon><User /></el-icon>
            用户登录
          </el-button>
          <el-button size="large" type="success" @click="goToMerchantLogin" round>
            <el-icon><Shop /></el-icon>
            商家登录
          </el-button>
        </div>
      </div>
    </div>

    <!-- 系统特性 -->
    <div class="section features-section">
      <div class="section-header">
        <h2 class="section-title">核心功能</h2>
        <p class="section-description">为企业提供全方位的库存管理解决方案</p>
      </div>
      
      <el-row :gutter="30" justify="center">
        <el-col v-for="feature in features" :key="feature.title" :xs="24" :sm="12" :md="8">
          <div class="feature-card">
            <div class="feature-icon">
              <el-icon :size="40" :color="feature.color">
                <component :is="feature.icon" />
              </el-icon>
            </div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 技术架构 -->
    <div class="section tech-stack">
      <div class="section-header">
        <h2 class="section-title">技术架构</h2>
        <p class="section-description">采用现代化技术栈，确保系统高性能和可扩展性</p>
      </div>
      
      <el-row :gutter="30" justify="center">
        <el-col v-for="tech in techStack" :key="tech.name" :xs="12" :sm="6" :md="6">
          <div class="tech-card">
            <div class="tech-icon">
              <el-icon :size="36" :color="tech.color">
                <component :is="tech.icon" />
              </el-icon>
            </div>
            <h4 class="tech-name">{{ tech.name }}</h4>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 系统优势 -->
    <div class="section advantages-section">
      <div class="section-header">
        <h2 class="section-title">系统优势</h2>
        <p class="section-description">为什么选择我们的库存管理系统</p>
      </div>
      
      <el-row :gutter="30" justify="center">
        <el-col v-for="advantage in advantages" :key="advantage.title" :xs="24" :sm="12" :md="6">
          <div class="advantage-card">
            <div class="advantage-icon">
              <el-icon :size="24" color="#409EFF">
                <Check />
              </el-icon>
            </div>
            <h4 class="advantage-title">{{ advantage.title }}</h4>
            <p class="advantage-description">{{ advantage.description }}</p>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { 
  Box, 
  DataAnalysis, 
  Setting, 
  User, 
  Shop,
  Check,
  Cloudy,
  Monitor
} from '@element-plus/icons-vue'

const router = useRouter()

const goToLogin = () => {
  router.push('/login')
}

const goToMerchantLogin = () => {
  router.push('/merchant/login')
}

const features = [
  {
    icon: 'Box',
    title: '库存管理',
    description: '实时监控库存状态，支持多仓库管理，自动预警低库存商品',
    color: '#409EFF'
  },
  {
    icon: 'DataAnalysis',
    title: '数据分析',
    description: '提供多维度数据统计和分析报表，助力业务决策',
    color: '#67C23A'
  },
  {
    icon: 'Setting',
    title: '系统配置',
    description: '灵活的系统配置，支持多角色权限管理，保障数据安全',
    color: '#E6A23C'
  }
]

const techStack = [
  { name: 'Spring Boot', icon: 'Box', color: '#6DB33F' },
  { name: 'Spring Cloud', icon: 'Cloudy', color: '#59B3A9' },
  { name: 'Nacos', icon: 'Setting', color: '#00BCD4' },
  { name: 'Vue 3', icon: 'Monitor', color: '#42B883' }
]

const advantages = [
  {
    title: '高并发处理',
    description: '支持高并发场景下的库存操作，提升响应性能'
  },
  {
    title: '安全保障',
    description: '使用JWT进行身份认证，保障系统安全性'
  },
  {
    title: '微服务架构',
    description: '基于Spring Cloud Alibaba构建，服务间通过Feign通信'
  },
  {
    title: '实时监控',
    description: '集成Redis缓存提升库存查询性能'
  }
]
</script>

<style scoped>
.home-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 主横幅区域样式 */
.hero-section {
  text-align: center;
  padding: 80px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
  margin-bottom: 40px;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 70%);
  transform: rotate(30deg);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 20px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.hero-subtitle {
  font-size: 20px;
  margin-bottom: 40px;
  opacity: 0.95;
  font-weight: 300;
  line-height: 1.6;
}

.cta-buttons .el-button {
  margin: 0 15px;
  padding: 15px 30px;
  font-size: 16px;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  transition: all 0.3s ease;
}

.cta-buttons .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.3);
}

/* 通用区域样式 */
.section {
  margin-bottom: 60px;
}

.section-header {
  text-align: center;
  margin-bottom: 50px;
}

.section-title {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
}

.section-description {
  font-size: 18px;
  color: #666;
  max-width: 600px;
  margin: 0 auto;
}

/* 功能卡片样式 */
.feature-card {
  text-align: center;
  padding: 40px 25px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
}

.feature-icon {
  margin-bottom: 25px;
  display: flex;
  justify-content: center;
}

.feature-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 15px 0;
  color: #333;
}

.feature-description {
  color: #666;
  line-height: 1.7;
  flex-grow: 1;
}

/* 技术栈卡片样式 */
.tech-card {
  text-align: center;
  padding: 30px 20px;
  background: #f8f9fa;
  border-radius: 10px;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.tech-card:hover {
  background: #e9ecef;
  transform: translateY(-5px);
}

.tech-icon {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.tech-name {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #495057;
}

/* 优势卡片样式 */
.advantage-card {
  text-align: center;
  padding: 30px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.advantage-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.advantage-icon {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ecf5ff;
  border-radius: 50%;
  margin: 0 auto 20px;
}

.advantage-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 10px 0;
  color: #333;
}

.advantage-description {
  color: #666;
  line-height: 1.6;
  margin: 0;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }
  
  .hero-subtitle {
    font-size: 16px;
  }
  
  .section-title {
    font-size: 26px;
  }
  
  .section-description {
    font-size: 16px;
  }
  
  .cta-buttons .el-button {
    display: block;
    width: 80%;
    margin: 10px auto;
  }
  
  .feature-card {
    margin-bottom: 20px;
  }
}
</style>