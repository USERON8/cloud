<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h2>用户注册</h2>
        <p>创建您的云库存管理系统账户</p>
      </div>

      <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
              v-model="registerForm.username"
              :prefix-icon="User"
              placeholder="请输入用户名"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
              v-model="registerForm.email"
              :prefix-icon="Message"
              placeholder="请输入邮箱"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
              v-model="registerForm.phone"
              :prefix-icon="Phone"
              placeholder="请输入手机号"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
              v-model="registerForm.nickname"
              :prefix-icon="UserFilled"
              placeholder="请输入昵称"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="registerForm.password"
              :prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              size="large"
              type="password"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
              v-model="registerForm.confirmPassword"
              :prefix-icon="Lock"
              placeholder="请确认密码"
              show-password
              size="large"
              type="password"
          />
        </el-form-item>

        <el-form-item prop="userType">
          <el-select
              v-model="registerForm.userType"
              placeholder="请选择用户类型"
              size="large"
              style="width: 100%"
          >
            <el-option label="普通用户" value="USER"/>
            <el-option label="商户" value="MERCHANT"/>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
              :loading="loading"
              class="register-button"
              size="large"
              type="primary"
              @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <el-link type="primary" @click="goToLogin">已有账号？立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Lock, Message, Phone, User, UserFilled} from '@element-plus/icons-vue'
import {useAuthStore} from '../../store/modules/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const registerFormRef = ref()

// 注册表单
const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  userType: 'USER'
})

// 注册表单验证规则
const registerRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur'}
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ],
  phone: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur'}
  ],
  nickname: [
    {required: true, message: '请输入昵称', trigger: 'blur'},
    {min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码长度至少6位', trigger: 'blur'}
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
  ],
  userType: [
    {required: true, message: '请选择用户类型', trigger: 'change'}
  ]
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    await registerFormRef.value.validate()
    loading.value = true

    await authStore.register(registerForm)

    // 注册成功后跳转到登录页面
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-box {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 24px;
}

.register-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.register-form {
  margin-bottom: 20px;
}

.register-button {
  width: 100%;
}

.register-footer {
  text-align: center;
}
</style>