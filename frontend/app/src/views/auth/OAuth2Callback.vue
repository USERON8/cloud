<template>
  <div class="oauth2-callback-container">
    <div class="oauth2-callback-card card">
      <div class="oauth2-callback-content">
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else class="callback-result">
          <el-result 
            v-if="success" 
            icon="success" 
            title="登录成功" 
            sub-title="正在跳转到系统首页..."
          >
            <template #extra>
              <el-button type="primary" @click="goToHome">立即跳转</el-button>
            </template>
          </el-result>
          <el-result 
            v-else 
            icon="error" 
            title="登录失败" 
            :sub-title="errorMessage || 'OAuth2授权失败，请重试'"
          >
            <template #extra>
              <el-button type="primary" @click="retryLogin">重新登录</el-button>
            </template>
          </el-result>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { oauth2Token } from '@/api/oauth2'
import { useUserStore } from '@/stores/user'

// 路由实例
const router = useRouter()
const route = useRoute()

// 用户store
const userStore = useUserStore()

// 状态变量
const loading = ref(true)
const success = ref(false)
const errorMessage = ref('')

// 跳转到首页
const goToHome = () => {
  router.push('/dashboard')
}

// 重新登录
const retryLogin = () => {
  router.push('/oauth2/login')
}

// 处理OAuth2回调
const handleOAuth2Callback = async () => {
  try {
    const code = route.query.code as string
    const state = route.query.state as string
    
    if (!code) {
      errorMessage.value = '缺少授权码参数'
      success.value = false
      loading.value = false
      return
    }
    
    // 使用授权码获取访问令牌
    const tokenResponse = await oauth2Token({
      grant_type: 'authorization_code',
      code: code,
      redirect_uri: window.location.origin + '/oauth2/callback',
      client_id: 'web-client',
      client_secret: 'web-client-secret'
    })
    
    if (tokenResponse.access_token) {
      // 保存OAuth2访问令牌
      userStore.setOAuth2Token(tokenResponse.access_token)
      
      success.value = true
      ElMessage.success('登录成功')
      
      // 3秒后自动跳转到首页
      setTimeout(() => {
        goToHome()
      }, 3000)
    } else {
      errorMessage.value = '获取访问令牌失败'
      success.value = false
    }
  } catch (err: any) {
    errorMessage.value = err.message || '处理OAuth2回调时发生错误'
    success.value = false
    ElMessage.error('OAuth2回调处理失败: ' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 页面加载时处理OAuth2回调
onMounted(() => {
  handleOAuth2Callback()
})
</script>

<style scoped>
.oauth2-callback-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.oauth2-callback-card {
  width: 100%;
  max-width: 500px;
  padding: 40px 30px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.oauth2-callback-content {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.callback-result {
  width: 100%;
  text-align: center;
}
</style>