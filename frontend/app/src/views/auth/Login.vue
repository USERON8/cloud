<template>
  <div class="login-container">
    <div class="login-card card">
      <div class="login-header">
        <h2>用户登录</h2>
        <p>欢迎使用云平台管理系统</p>
      </div>
      
      <el-form 
        ref="loginFormRef" 
        :model="loginForm" 
        :rules="loginRules" 
        @submit.prevent="handleLogin"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名/邮箱/手机号"
            size="large"
            clearable
            autofocus
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item class="login-options">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <el-link type="primary" class="forgot-password" :underline="false">忘记密码？</el-link>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleLogin" 
            :loading="loading"
            size="large"
            class="login-button"
            style="width: 100%"
            round
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p class="switch-login">其他登录方式</p>
        <div class="login-providers">
          <el-button type="primary" link @click="switchToOAuth2Login">
            <el-icon size="24"><IEpKey /></el-icon>
          </el-button>
          <el-button type="success" link>
            <el-icon size="24"><IEpShop /></el-icon>
          </el-button>
          <el-button type="warning" link>
            <el-icon size="24"><IEpSetting /></el-icon>
          </el-button>
        </div>
        <div class="login-links">
          <el-link type="primary" @click="switchToRegister" :underline="false">还没有账号？立即注册</el-link>
          <el-link type="success" style="margin-left: 10px;" @click="switchToMerchantRegister" :underline="false">商家注册</el-link>
          <el-link type="warning" style="margin-left: 10px;" @click="switchToAdminLogin" :underline="false">管理员登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { User, Lock, IEpKey } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

// 定义表单引用
const loginFormRef = ref<FormInstance>()

// 路由实例
const router = useRouter()

// 用户store
const userStore = useUserStore()

// 加载状态
const loading = ref(false)

// 记住我
const rememberMe = ref(false)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 登录表单验证规则
const loginRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应在3-20个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在6-20个字符之间', trigger: 'blur' }
  ]
})

// 登录处理函数
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login({
          username: loginForm.username,
          password: loginForm.password
        })
        
        if (res.code === 200) {
          // 保存用户信息
          userStore.setUserInfo(res.data.user)
          userStore.setToken(res.data.token)
          
          ElMessage.success('登录成功')
          router.push('/dashboard')
        } else {
          ElMessage.error(res.message || '登录失败')
        }
      } catch (err) {
        ElMessage.error('登录请求失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}

// 页面跳转函数
const switchToRegister = () => {
  router.push('/register')
}

const switchToMerchantRegister = () => {
  router.push('/merchant-register')
}

const switchToAdminLogin = () => {
  router.push('/admin/login')
}

const switchToOAuth2Login = () => {
  router.push('/oauth2/login')
}

// 页面加载时自动聚焦到用户名输入框
onMounted(() => {
  // 用户名输入框自动聚焦已在模板中通过autofocus属性实现
})
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 450px;
  padding: 40px 30px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 2rem;
  margin-bottom: 10px;
  color: var(--primary-text-color);
}

.login-header p {
  font-size: 1rem;
  color: var(--secondary-text-color);
}

.login-form {
  margin-bottom: 20px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-password {
  font-size: 0.9rem;
}

.login-button {
  margin-top: 10px;
  font-size: 1.1rem;
  padding: 12px;
}

.login-footer {
  text-align: center;
  margin-top: 30px;
}

.switch-login {
  position: relative;
  margin-bottom: 20px;
  color: var(--secondary-text-color);
}

.switch-login::before,
.switch-login::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background-color: var(--border-color);
}

.switch-login::before {
  left: 0;
}

.switch-login::after {
  right: 0;
}

.login-providers {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
}

.login-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 5px;
  font-size: 0.9rem;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }
  
  .login-header h2 {
    font-size: 1.5rem;
  }
  
  .login-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .login-links {
    flex-direction: column;
    gap: 10px;
  }
}
</style>