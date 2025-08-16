<template>
  <div class="oauth2-login-container">
    <div class="oauth2-login-card card">
      <div class="oauth2-login-header">
        <h2>OAuth2 登录</h2>
        <p>使用OAuth2授权码模式登录系统</p>
      </div>
      
      <el-alert 
        v-if="authError" 
        :title="authError" 
        type="error" 
        show-icon 
        closable 
        @close="authError = ''"
        style="margin-bottom: 20px;"
      />
      
      <el-form 
        ref="oauth2FormRef" 
        :model="oauth2Form" 
        :rules="oauth2Rules" 
        @submit.prevent="handleOAuth2Login"
        class="oauth2-form"
      >
        <el-form-item prop="clientId">
          <el-input 
            v-model="oauth2Form.clientId" 
            placeholder="请输入客户端ID"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><IEpKey /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="redirectUri">
          <el-input 
            v-model="oauth2Form.redirectUri" 
            placeholder="请输入重定向URI"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><IEpLink /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="scope">
          <el-input 
            v-model="oauth2Form.scope" 
            placeholder="请输入权限范围"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><IEpLock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleOAuth2Login" 
            :loading="loading"
            size="large"
            class="oauth2-button"
            style="width: 100%"
            round
          >
            OAuth2 登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="oauth2-footer">
        <div class="oauth2-links">
          <el-link type="primary" @click="switchToRegister" :underline="false">注册账号</el-link>
          <el-link type="success" style="margin-left: 10px;" @click="switchToAdminLogin" :underline="false">管理员登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { IEpKey, IEpLink, IEpLock } from '@element-plus/icons-vue'
import { oauth2Login } from '@/api/oauth2'
import { useUserStore } from '@/stores/user'

// 定义表单引用
const oauth2FormRef = ref<FormInstance>()

// 路由实例
const router = useRouter()
const route = useRoute()

// 用户store
const userStore = useUserStore()

// 加载状态
const loading = ref(false)

// 认证错误信息
const authError = ref('')

// OAuth2表单数据
const oauth2Form = reactive({
  clientId: 'web-client',
  redirectUri: window.location.origin + '/oauth2/callback',
  scope: 'read write'
})

// OAuth2表单验证规则
const oauth2Rules = reactive<FormRules>({
  clientId: [
    { required: true, message: '请输入客户端ID', trigger: 'blur' }
  ],
  redirectUri: [
    { required: true, message: '请输入重定向URI', trigger: 'blur' }
  ],
  scope: [
    { required: true, message: '请输入权限范围', trigger: 'blur' }
  ]
})

// OAuth2登录处理函数
const handleOAuth2Login = async () => {
  if (!oauth2FormRef.value) return
  
  await oauth2FormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      authError.value = ''
      try {
        // 发起OAuth2授权请求
        oauth2Login(
          oauth2Form.clientId,
          oauth2Form.redirectUri,
          oauth2Form.scope
        )
      } catch (err: any) {
        authError.value = err.message || 'OAuth2登录请求失败，请稍后重试'
        ElMessage.error('OAuth2登录请求失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}

// 页面跳转函数
const switchToLogin = () => {
  router.push('/login')
}

const switchToRegister = () => {
  router.push('/register')
}

const switchToAdminLogin = () => {
  router.push('/admin/login')
}

// 处理OAuth2回调错误
const handleOAuth2CallbackError = () => {
  const error = route.query.error as string
  const errorDescription = route.query.error_description as string
  
  if (error) {
    authError.value = `OAuth2授权错误: ${errorDescription || error}`
  }
}

// 页面加载时检查是否有OAuth2回调错误参数
onMounted(() => {
  handleOAuth2CallbackError()
})
</script>

<style scoped>
.oauth2-login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.oauth2-login-card {
  width: 100%;
  max-width: 450px;
  padding: 40px 30px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.oauth2-login-header {
  text-align: center;
  margin-bottom: 30px;
}

.oauth2-login-header h2 {
  font-size: 2rem;
  margin-bottom: 10px;
  color: var(--primary-text-color);
}

.oauth2-login-header p {
  font-size: 1rem;
  color: var(--secondary-text-color);
}

.oauth2-form {
  margin-bottom: 20px;
}

.oauth2-button {
  margin-top: 10px;
  font-size: 1.1rem;
  padding: 12px;
}

.oauth2-footer {
  text-align: center;
  margin-top: 30px;
}

.oauth2-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 5px;
  font-size: 0.9rem;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .oauth2-login-card {
    padding: 30px 20px;
  }
  
  .oauth2-login-header h2 {
    font-size: 1.5rem;
  }
  
  .oauth2-links {
    flex-direction: column;
    gap: 10px;
  }
}
</style>