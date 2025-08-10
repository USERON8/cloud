<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-box">
        <div class="login-header">
          <h2>商家登录</h2>
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
                placeholder="请输入商家账号"
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
                type="success"
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
                placeholder="请输入商家账号"
                prefix-icon="User"
                size="large"
            />
          </el-form-item>

          <el-form-item prop="nickname">
            <el-input
                v-model="registerForm.nickname"
                placeholder="请输入店铺名称"
                prefix-icon="Shop"
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
                placeholder="请输入联系电话"
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
                type="success"
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
            普通用户？
            <el-link type="primary" @click="goToUserLogin">用户登录</el-link>
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
    {required: true, message: '请输入商家账号', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码长度至少6位', trigger: 'blur'}
  ]
}

const registerRules = {
  username: [
    {required: true, message: '请输入商家账号', trigger: 'blur'}
  ],
  nickname: [
    {required: true, message: '请输入店铺名称', trigger: 'blur'}
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ],
  phone: [
    {required: true, message: '请输入联系电话', trigger: 'blur'},
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

  await loginFormRef.value.validate((valid) => {
    if (valid) {
      // 模拟登录请求
      loading.value = true

      // 模拟API调用
      setTimeout(() => {
        // 模拟登录成功
        localStorage.setItem('token', 'fake-jwt-token')
        localStorage.setItem('userType', 'MERCHANT')
        localStorage.setItem('nickname', loginForm.username)

        ElMessage.success('商家登录成功')
        router.push('/merchant')
        loading.value = false
      }, 1000)
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true

      // 模拟注册并登录请求
      setTimeout(() => {
        // 模拟注册并登录成功
        localStorage.setItem('token', 'fake-jwt-token')
        localStorage.setItem('userType', 'MERCHANT')
        localStorage.setItem('nickname', registerForm.username)

        ElMessage.success('商家注册并登录成功')
        router.push('/merchant')
        loading.value = false
      }, 1500)
    }
  })
}

const goToUserLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 120px);
  background: linear-gradient(135deg, #f0f9ff 0%, #c5e3f6 100%);
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