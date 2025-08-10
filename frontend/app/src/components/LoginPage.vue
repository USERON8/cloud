<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-box">
        <div class="login-header">
          <h2>用户登录</h2>
          <p>欢迎使用云库存管理系统</p>
        </div>

        <!-- 登录表单 -->
        <el-form
            v-if="!showRegister"
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
                v-model="loginForm.password"
                placeholder="请输入密码"
                prefix-icon="Lock"
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
                @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 注册表单 -->
        <el-form
            v-else
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="register-form"
            @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
            />
          </el-form-item>

          <el-form-item prop="nickname">
            <el-input
                v-model="registerForm.nickname"
                placeholder="请输入昵称"
                prefix-icon="User"
                size="large"
            />
          </el-form-item>

          <el-form-item prop="email">
            <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                prefix-icon="Message"
                size="large"
            />
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号"
                prefix-icon="Phone"
                size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
                v-model="registerForm.password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
                size="large"
                type="password"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
                v-model="registerForm.confirmPassword"
                placeholder="请确认密码"
                prefix-icon="Lock"
                show-password
                size="large"
                type="password"
            />
          </el-form-item>

          <el-form-item>
            <el-button
                :loading="loading"
                class="register-button"
                size="large"
                type="primary"
                @click="handleRegister"
            >
              注册并登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <p v-if="!showRegister">
            还没有账号？
            <el-link type="primary" @click="showRegister = true">立即注册</el-link>
          </p>
          <p v-else>
            已有账号？
            <el-link type="primary" @click="showRegister = false">返回登录</el-link>
          </p>
          <p>
            商家用户？
            <el-link type="success" @click="goToMerchantLogin">商家登录</el-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {authApi} from '../api/auth'

const router = useRouter()
const loginFormRef = ref()
const registerFormRef = ref()
const showRegister = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)

const loginRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码长度至少6位', trigger: 'blur'}
  ]
}

const registerRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'}
  ],
  nickname: [
    {required: true, message: '请输入昵称', trigger: 'blur'}
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ],
  phone: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 8, message: '密码长度至少8位', trigger: 'blur'}
  ],
  confirmPassword: [
    {required: true, message: '请确认密码', trigger: 'blur'},
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        // 调用登录API
        const response = await authApi.login({
          username: loginForm.username,
          password: loginForm.password
        })

        // 保存认证信息到本地存储
        localStorage.setItem('token', response.token)
        localStorage.setItem('userType', response.userType)
        localStorage.setItem('nickname', response.nickname)

        ElMessage.success('登录成功')
        // 根据用户类型跳转到相应页面
        if (response.userType === 'ADMIN') {
          router.push('/admin')
        } else if (response.userType === 'MERCHANT') {
          router.push('/merchant')
        } else {
          router.push('/user')
        }
      } catch (error) {
        console.error('登录失败:', error)
        ElMessage.error(error.message || '登录失败，请检查用户名和密码')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        // 调用注册并登录API
        const response = await authApi.registerAndLogin({
          username: registerForm.username,
          nickname: registerForm.nickname,
          email: registerForm.email,
          phone: registerForm.phone,
          password: registerForm.password
        })

        // 保存认证信息到本地存储
        localStorage.setItem('token', response.token)
        localStorage.setItem('userType', response.userType)
        localStorage.setItem('nickname', response.nickname)

        ElMessage.success('注册并登录成功')
        // 根据用户类型跳转到相应页面
        if (response.userType === 'ADMIN') {
          router.push('/admin')
        } else if (response.userType === 'MERCHANT') {
          router.push('/merchant')
        } else {
          router.push('/user')
        }
      } catch (error) {
        console.error('注册失败:', error)
        ElMessage.error(error.message || '注册失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToMerchantLogin = () => {
  router.push('/merchant/login')
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 120px);
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-box {
  background: white;
  padding: 40px 30px;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(50, 50, 93, 0.1), 0 5px 15px rgba(0, 0, 0, 0.07);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0 0 10px;
  font-size: 24px;
  color: #333;
}

.login-header p {
  margin: 0;
  color: #666;
}

.login-form,
.register-form {
  margin-bottom: 20px;
}

.login-button,
.register-button {
  width: 100%;
}

.login-footer {
  text-align: center;
  color: #666;
}

.login-footer p {
  margin: 10px 0;
}

.login-footer .el-link {
  margin-left: 5px;
}
</style>