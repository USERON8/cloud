<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>云库存管理系统</h2>
        <p>基于Spring Cloud Alibaba的微服务架构</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="用户登录" name="user">
          <el-form
              ref="userLoginFormRef"
              :model="userLoginForm"
              :rules="loginRules"
              class="login-form"
              @submit.prevent="handleUserLogin"
          >
            <el-form-item prop="username">
              <el-input
                  v-model="userLoginForm.username"
                  :prefix-icon="User"
                  placeholder="请输入用户名"
                  size="large"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                  v-model="userLoginForm.password"
                  :prefix-icon="Lock"
                  placeholder="请输入密码"
                  show-password
                  size="large"
                  type="password"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                  :loading="loading"
                  class="login-button"
                  size="large"
                  type="primary"
                  @click="handleUserLogin"
              >
                登录
              </el-button>
            </el-form-item>

            <div class="login-footer">
              <el-link type="primary" @click="switchToRegister">还没有账号？立即注册</el-link>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="商户登录" name="merchant">
          <el-form
              ref="merchantLoginFormRef"
              :model="merchantLoginForm"
              :rules="loginRules"
              class="login-form"
              @submit.prevent="handleMerchantLogin"
          >
            <el-form-item prop="username">
              <el-input
                  v-model="merchantLoginForm.username"
                  :prefix-icon="Shop"
                  placeholder="请输入商户名"
                  size="large"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                  v-model="merchantLoginForm.password"
                  :prefix-icon="Lock"
                  placeholder="请输入密码"
                  show-password
                  size="large"
                  type="password"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                  :loading="loading"
                  class="login-button"
                  size="large"
                  type="success"
                  @click="handleMerchantLogin"
              >
                商户登录
              </el-button>
            </el-form-item>

            <div class="login-footer">
              <el-link type="primary" @click="switchToRegister">还没有账号？立即注册</el-link>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="管理员登录" name="admin">
          <el-form
              ref="adminLoginFormRef"
              :model="adminLoginForm"
              :rules="loginRules"
              class="login-form"
              @submit.prevent="handleAdminLogin"
          >
            <el-form-item prop="username">
              <el-input
                  v-model="adminLoginForm.username"
                  :prefix-icon="Setting"
                  placeholder="请输入管理员账号"
                  size="large"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                  v-model="adminLoginForm.password"
                  :prefix-icon="Lock"
                  placeholder="请输入密码"
                  show-password
                  size="large"
                  type="password"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                  :loading="loading"
                  class="login-button"
                  size="large"
                  type="danger"
                  @click="handleAdminLogin"
              >
                管理员登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div v-if="activeTab === 'user' || activeTab === 'merchant'" class="register-section">
        <p>还没有账号？</p>
        <el-button link type="primary" @click="switchToRegister">立即注册</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Lock, Setting, Shop, User} from '@element-plus/icons-vue'
import {useAuthStore} from '../../store/modules/auth'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('user')
const loading = ref(false)

// 表单引用
const userLoginFormRef = ref()
const merchantLoginFormRef = ref()
const adminLoginFormRef = ref()

// 用户登录表单
const userLoginForm = reactive({
  username: '',
  password: ''
})

// 商户登录表单
const merchantLoginForm = reactive({
  username: '',
  password: ''
})

// 管理员登录表单
const adminLoginForm = reactive({
  username: '',
  password: ''
})

// 登录表单验证规则
const loginRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码长度至少6位', trigger: 'blur'}
  ]
}

// 切换到注册页面
const switchToRegister = () => {
  router.push('/auth/register')
}

// 处理用户登录
const handleUserLogin = async () => {
  if (!userLoginFormRef.value) return

  try {
    await userLoginFormRef.value.validate()
    loading.value = true

    // 设置用户类型
    const loginData = {
      ...userLoginForm,
      userType: 'USER'
    }

    await authStore.login(loginData)
  } catch (error) {
    console.error('用户登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理商户登录
const handleMerchantLogin = async () => {
  if (!merchantLoginFormRef.value) return

  try {
    await merchantLoginFormRef.value.validate()
    loading.value = true

    // 设置用户类型
    const loginData = {
      ...merchantLoginForm,
      userType: 'MERCHANT'
    }

    await authStore.login(loginData)
  } catch (error) {
    console.error('商户登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理管理员登录
const handleAdminLogin = async () => {
  if (!adminLoginFormRef.value) return

  try {
    await adminLoginFormRef.value.validate()
    loading.value = true

    // 设置用户类型
    const loginData = {
      ...adminLoginForm,
      userType: 'ADMIN'
    }

    await authStore.login(loginData)
  } catch (error) {
    console.error('管理员登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 24px;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.login-tabs {
  margin-bottom: 20px;
}

.login-form {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
}

.login-footer {
  text-align: center;
}

.register-section {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.register-section p {
  margin: 0 0 10px 0;
  color: #606266;
}
</style>