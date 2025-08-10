<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :span="16">
        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
            </div>
          </template>

          <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="100px"
          >
            <el-row>
              <el-col :span="12">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="profileForm.username" disabled/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="用户类型" prop="userType">
                  <el-input v-model="getUserTypeName(profileForm.userType)" disabled/>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row>
              <el-col :span="12">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="状态" prop="status">
                  <el-tag :type="profileForm.status === 1 ? 'success' : 'danger'">
                    {{ profileForm.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row>
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone"/>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="创建时间" prop="createTime">
              <el-input v-model="profileForm.createTime" disabled/>
            </el-form-item>

            <el-form-item>
              <el-button
                  :loading="submitLoading"
                  type="primary"
                  @click="submitProfileForm"
              >
                保存信息
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 修改密码卡片 -->
      <el-col :span="8">
        <el-card class="password-card">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>

          <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="80px"
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input
                  v-model="passwordForm.oldPassword"
                  placeholder="请输入旧密码"
                  show-password
                  type="password"
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                  v-model="passwordForm.newPassword"
                  placeholder="请输入新密码"
                  show-password
                  type="password"
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                  v-model="passwordForm.confirmPassword"
                  placeholder="请再次输入新密码"
                  show-password
                  type="password"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                  :loading="passwordLoading"
                  type="primary"
                  @click="submitPasswordForm"
              >
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {useUserStore} from '@/store/modules/user'
import {useAuthStore} from '@/store/modules/auth'

// 状态管理
const userStore = useUserStore()
const authStore = useAuthStore()

// 响应式数据
const profileForm = reactive({
  id: null,
  username: '',
  nickname: '',
  userType: '',
  email: '',
  phone: '',
  status: 1,
  createTime: ''
})

const profileRules = {
  nickname: [
    {required: true, message: '请输入昵称', trigger: 'blur'}
  ],
  email: [
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ]
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [
    {required: true, message: '请输入旧密码', trigger: 'blur'}
  ],
  newPassword: [
    {required: true, message: '请输入新密码', trigger: 'blur'},
    {min: 6, message: '密码长度不能少于6位', trigger: 'blur'}
  ],
  confirmPassword: [
    {required: true, message: '请再次输入新密码', trigger: 'blur'},
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const submitLoading = ref(false)
const passwordLoading = ref(false)

// 计算属性
const userInfo = computed(() => userStore.userInfo)

// 方法
const submitProfileForm = async () => {
  try {
    await profileFormRef.value.validate()
    submitLoading.value = true

    // 更新用户信息
    await userStore.updateUserInfo(profileForm)
    ElMessage.success('信息保存成功')
  } catch (error) {
    ElMessage.error(error.message || '信息保存失败')
  } finally {
    submitLoading.value = false
  }
}

const submitPasswordForm = async () => {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true

    // 修改密码
    await userStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码修改成功')

    // 重置密码表单
    Object.assign(passwordForm, {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

// 获取用户类型名称
const getUserTypeName = (userType) => {
  switch (userType) {
    case 'admin':
      return '管理员'
    case 'merchant':
      return '商户'
    case 'user':
      return '普通用户'
    default:
      return '未知'
  }
}

// 初始化用户信息
const initUserInfo = () => {
  if (userInfo.value) {
    Object.assign(profileForm, userInfo.value)
  }
}

// 组件挂载时获取用户信息
onMounted(() => {
  initUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

.card-header {
  font-weight: bold;
}

.profile-card,
.password-card {
  margin-bottom: 20px;
}
</style>