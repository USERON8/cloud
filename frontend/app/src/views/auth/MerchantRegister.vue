<template>
  <div class="merchant-register-page">
    <div class="merchant-register-container">
      <div class="merchant-register-box">
        <div class="merchant-register-header">
          <h2>商家注册</h2>
          <p>注册成为云库存管理系统商家用户</p>
        </div>

        <el-steps :active="stepActive" finish-status="success" align-center class="register-steps">
          <el-step title="基本信息"></el-step>
          <el-step title="认证材料"></el-step>
          <el-step title="完成"></el-step>
        </el-steps>

        <!-- 第一步：基本信息 -->
        <el-form
            v-show="stepActive === 0"
            ref="basicFormRef"
            :model="basicForm"
            :rules="basicRules"
            class="register-form"
            label-width="100px"
            @submit.prevent
        >
          <el-form-item label="用户名" prop="username">
            <el-input
                v-model="basicForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
                v-model="basicForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
                v-model="basicForm.confirmPassword"
                type="password"
                placeholder="请确认密码"
                :prefix-icon="Lock"
                size="large"
                show-password
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
                v-model="basicForm.email"
                placeholder="请输入邮箱"
                :prefix-icon="Message"
                size="large"
            />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input
                v-model="basicForm.phone"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
                size="large"
            />
          </el-form-item>

          <el-form-item label="店铺名称" prop="nickname">
            <el-input
                v-model="basicForm.nickname"
                placeholder="请输入店铺名称"
                :prefix-icon="Shop"
                size="large"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button @click="goToLogin">返回登录</el-button>
            <el-button type="primary" @click="nextStep(0)">下一步</el-button>
          </div>
        </el-form>

        <!-- 第二步：认证材料 -->
        <el-form
            v-show="stepActive === 1"
            ref="authFormRef"
            :model="authForm"
            :rules="authRules"
            class="register-form"
            label-width="120px"
            @submit.prevent
        >
          <el-form-item label="营业执照号码" prop="businessLicenseNumber">
            <el-input
                v-model="authForm.businessLicenseNumber"
                placeholder="请输入营业执照号码"
                size="large"
            />
          </el-form-item>

          <el-form-item label="营业执照图片" prop="businessLicenseFile">
            <el-upload
                class="upload-demo"
                drag
                :auto-upload="false"
                :on-change="handleBusinessLicenseChange"
                :show-file-list="false"
            >
              <el-icon class="el-icon--upload">
                <UploadFilled/>
              </el-icon>
              <div class="el-upload__text">
                将营业执照图片拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  请上传清晰的营业执照图片，支持jpg、png格式，大小不超过5MB
                </div>
              </template>
            </el-upload>
            <div v-if="businessLicensePreview" class="image-preview">
              <img :src="businessLicensePreview" alt="营业执照预览"/>
            </div>
          </el-form-item>

          <el-form-item label="身份证正面" prop="idCardFrontFile">
            <el-upload
                class="upload-demo"
                drag
                :auto-upload="false"
                :on-change="handleIdCardFrontChange"
                :show-file-list="false"
            >
              <el-icon class="el-icon--upload">
                <UploadFilled/>
              </el-icon>
              <div class="el-upload__text">
                将身份证正面图片拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  请上传清晰的身份证正面图片，支持jpg、png格式，大小不超过5MB
                </div>
              </template>
            </el-upload>
            <div v-if="idCardFrontPreview" class="image-preview">
              <img :src="idCardFrontPreview" alt="身份证正面预览"/>
            </div>
          </el-form-item>

          <el-form-item label="身份证反面" prop="idCardBackFile">
            <el-upload
                class="upload-demo"
                drag
                :auto-upload="false"
                :on-change="handleIdCardBackChange"
                :show-file-list="false"
            >
              <el-icon class="el-icon--upload">
                <UploadFilled/>
              </el-icon>
              <div class="el-upload__text">
                将身份证反面图片拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  请上传清晰的身份证反面图片，支持jpg、png格式，大小不超过5MB
                </div>
              </template>
            </el-upload>
            <div v-if="idCardBackPreview" class="image-preview">
              <img :src="idCardBackPreview" alt="身份证反面预览"/>
            </div>
          </el-form-item>

          <el-form-item label="联系电话" prop="contactPhone">
            <el-input
                v-model="authForm.contactPhone"
                placeholder="请输入联系电话"
                :prefix-icon="Phone"
                size="large"
            />
          </el-form-item>

          <el-form-item label="联系地址" prop="contactAddress">
            <el-input
                v-model="authForm.contactAddress"
                placeholder="请输入联系地址"
                type="textarea"
                :rows="3"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button @click="prevStep(1)">上一步</el-button>
            <el-button type="primary" @click="submitRegister">提交注册</el-button>
          </div>
        </el-form>

        <!-- 第三步：完成 -->
        <div v-show="stepActive === 2" class="register-success">
          <el-result
              icon="success"
              title="注册成功"
              subTitle="您的注册申请已提交，请等待管理员审核"
          >
            <template #extra>
              <el-button type="primary" @click="goToLogin">返回登录</el-button>
            </template>
          </el-result>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {
  User,
  Lock,
  Message,
  Phone,
  Shop,
  UploadFilled
} from '@element-plus/icons-vue'
import { merchantRegister } from '@/api/auth'
import { applyMerchantAuth } from '@/api/merchantAuth'

// 路由和引用
const router = useRouter()
const basicFormRef = ref()
const authFormRef = ref()

// 步骤控制
const stepActive = ref(0)

// 基本信息表单
const basicForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  nickname: ''
})

// 认证材料表单
const authForm = reactive({
  businessLicenseNumber: '',
  businessLicenseFile: null,
  idCardFrontFile: null,
  idCardBackFile: null,
  contactPhone: '',
  contactAddress: ''
})

// 图片预览
const businessLicensePreview = ref('')
const idCardFrontPreview = ref('')
const idCardBackPreview = ref('')

// 基本信息验证规则
const basicRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 8, message: '密码长度至少8位', trigger: 'blur'}
  ],
  confirmPassword: [
    {required: true, message: '请确认密码', trigger: 'blur'},
    {
      validator: (rule, value, callback) => {
        if (value !== basicForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
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
    {required: true, message: '请输入店铺名称', trigger: 'blur'}
  ]
}

// 认证材料验证规则
const authRules = {
  businessLicenseNumber: [
    {required: true, message: '请输入营业执照号码', trigger: 'blur'}
  ],
  businessLicenseFile: [
    {required: true, message: '请上传营业执照图片', trigger: 'change'}
  ],
  idCardFrontFile: [
    {required: true, message: '请上传身份证正面图片', trigger: 'change'}
  ],
  idCardBackFile: [
    {required: true, message: '请上传身份证反面图片', trigger: 'change'}
  ],
  contactPhone: [
    {required: true, message: '请输入联系电话', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur'}
  ],
  contactAddress: [
    {required: true, message: '请输入联系地址', trigger: 'blur'}
  ]
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 下一步
const nextStep = async (formIndex) => {
  let formRef
  if (formIndex === 0) {
    formRef = basicFormRef
  }

  if (!formRef.value) return

  try {
    await formRef.value.validate()
    stepActive.value++
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 上一步
const prevStep = (formIndex) => {
  stepActive.value--
}

// 处理营业执照上传
const handleBusinessLicenseChange = (file) => {
  authForm.businessLicenseFile = file.raw
  // 预览图片
  const reader = new FileReader()
  reader.onload = (e) => {
    businessLicensePreview.value = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 处理身份证正面上传
const handleIdCardFrontChange = (file) => {
  authForm.idCardFrontFile = file.raw
  // 预览图片
  const reader = new FileReader()
  reader.onload = (e) => {
    idCardFrontPreview.value = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 处理身份证反面上传
const handleIdCardBackChange = (file) => {
  authForm.idCardBackFile = file.raw
  // 预览图片
  const reader = new FileReader()
  reader.onload = (e) => {
    idCardBackPreview.value = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 提交注册
const submitRegister = async () => {
  if (!authFormRef.value) return

  try {
    await authFormRef.value.validate()
    
    // 创建FormData对象
    const formData = new FormData()
    formData.append('username', basicForm.username)
    formData.append('password', basicForm.password)
    formData.append('email', basicForm.email)
    formData.append('phone', basicForm.phone)
    formData.append('nickname', basicForm.nickname)
    formData.append('businessLicenseNumber', authForm.businessLicenseNumber)
    formData.append('businessLicenseFile', authForm.businessLicenseFile)
    formData.append('idCardFrontFile', authForm.idCardFrontFile)
    formData.append('idCardBackFile', authForm.idCardBackFile)
    formData.append('contactPhone', authForm.contactPhone)
    formData.append('contactAddress', authForm.contactAddress)

    // 提交商家注册
    await applyMerchantAuth(formData)
    
    // 进入完成步骤
    stepActive.value = 2
    
    ElMessage.success('注册成功，请等待管理员审核')
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败，请稍后重试')
  }
}
</script>

<style scoped>
.merchant-register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.merchant-register-container {
  width: 100%;
  max-width: 600px;
}

.merchant-register-box {
  background: rgba(255, 255, 255, 0.95);
  padding: 40px 30px;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.merchant-register-header {
  text-align: center;
  margin-bottom: 30px;
}

.merchant-register-header h2 {
  margin: 0 0 10px;
  font-size: 24px;
  color: var(--primary-text-color);
}

.merchant-register-header p {
  margin: 0;
  color: var(--secondary-text-color);
}

.register-steps {
  margin-bottom: 30px;
}

.register-form {
  margin-bottom: 20px;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
}

.upload-demo {
  width: 100%;
}

.image-preview {
  margin-top: 15px;
}

.image-preview img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.register-success {
  text-align: center;
}
</style>