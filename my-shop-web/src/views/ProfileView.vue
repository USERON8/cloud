<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { VueCropper } from 'vue-cropper'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { z } from 'zod'
import { changeCurrentPassword, getCurrentProfile, updateCurrentProfile, uploadCurrentAvatar } from '../api/user'
import { patchSessionUser, sessionState } from '../auth/session'
import type { UserInfo } from '../types/domain'

const loading = ref(false)
const saveLoading = ref(false)
const warning = ref('')
const avatarLoadFailed = ref(false)
const cropperVisible = ref(false)
const cropperImage = ref('')
const cropperUploading = ref(false)
const pendingAvatarName = ref('avatar.png')
const cropperRef = ref<InstanceType<typeof VueCropper> | null>(null)
const passwordSaving = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

const optionalEmail = z.string().trim().email('Invalid email format').or(z.literal(''))
const optionalPhone = z
  .string()
  .trim()
  .regex(/^1[3-9]\d{9}$/, 'Invalid phone format')
  .or(z.literal(''))

const profileSchema = toTypedSchema(
  z.object({
    nickname: z.string().trim().min(1, 'Nickname is required'),
    email: optionalEmail,
    phone: optionalPhone,
    avatarUrl: z.string().trim().max(500, 'Avatar URL is too long')
  })
)

const passwordSchema = toTypedSchema(
  z
    .object({
      oldPassword: z.string().min(1, 'Current password is required'),
      newPassword: z.string().min(6, 'New password must be at least 6 characters'),
      confirmPassword: z.string().min(1, 'Confirm the new password')
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
      message: 'New password does not match confirmation',
      path: ['confirmPassword']
    })
)

const {
  values: profileValues,
  errors: profileErrors,
  handleSubmit: handleProfileSubmit,
  setValues: setProfileValues
} = useForm({
  validationSchema: profileSchema,
  initialValues: {
    nickname: '',
    email: '',
    phone: '',
    avatarUrl: ''
  }
})

const {
  values: passwordValues,
  errors: passwordErrors,
  handleSubmit: handlePasswordSubmit,
  resetForm: resetPasswordForm
} = useForm({
  validationSchema: passwordSchema,
  initialValues: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
})

const accountName = computed(() => sessionState.user?.username || 'Unknown')
const accountType = computed(() => sessionState.user?.roles?.[0] || 'Unknown')
const avatarPreview = computed(() => {
  const avatarUrl = profileValues.avatarUrl ?? ''
  if (avatarUrl.trim() && !avatarLoadFailed.value) {
    return avatarUrl.trim()
  }
  const nickname = profileValues.nickname ?? ''
  const c = (nickname.trim()[0] || accountName.value[0] || 'U').toUpperCase()
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
    <defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#1a3f9e"/><stop offset="100%" stop-color="#58a0ff"/></linearGradient></defs>
    <rect width="120" height="120" rx="30" fill="url(#g)"/>
    <circle cx="60" cy="46" r="18" fill="#fff" fill-opacity=".9"/>
    <path d="M24 99c4-18 20-30 36-30s32 12 36 30" fill="#fff" fill-opacity=".9"/>
    <text x="60" y="109" text-anchor="middle" font-size="18" font-family="Avenir, 'PingFang SC', sans-serif" fill="#eef5ff">${c}</text>
  </svg>`
  return `data:image/svg+xml;base64,${window.btoa(unescape(encodeURIComponent(svg)))}`
})

function patchForm(data?: UserInfo | null): void {
  if (!data) {
    return
  }
  setProfileValues({
    nickname: data.nickname || '',
    email: data.email || '',
    phone: data.phone || '',
    avatarUrl: data.avatarUrl || ''
  })
}

async function loadProfile(): Promise<void> {
  loading.value = true
  warning.value = ''

  patchForm(sessionState.user)

  try {
    const result = await getCurrentProfile()
    patchForm(result)
    patchSessionUser({
      nickname: result.nickname,
      email: result.email,
      phone: result.phone,
      avatarUrl: result.avatarUrl
    })
  } catch (error) {
    warning.value = error instanceof Error ? error.message : 'Profile API is unavailable through gateway.'
  } finally {
    loading.value = false
  }
}

async function saveProfile(): Promise<void> {
  saveLoading.value = true
  try {
    await updateCurrentProfile({
      nickname: profileValues.nickname,
      email: profileValues.email,
      phone: profileValues.phone,
      avatarUrl: profileValues.avatarUrl
    })
    patchSessionUser({
      nickname: profileValues.nickname,
      email: profileValues.email,
      phone: profileValues.phone,
      avatarUrl: profileValues.avatarUrl
    })
    ElMessage.success('Profile updated.')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Profile update failed'
    ElMessage.error(message)
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  void loadProfile()
})

watch(
  () => profileValues.avatarUrl,
  () => {
    avatarLoadFailed.value = false
  }
)

function onAvatarError(): void {
  avatarLoadFailed.value = true
}

function openAvatarPicker(): void {
  fileInputRef.value?.click()
}

async function onAvatarSelected(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) {
    return
  }
  pendingAvatarName.value = file.name || 'avatar.png'
  const reader = new FileReader()
  reader.onload = () => {
    cropperImage.value = typeof reader.result === 'string' ? reader.result : ''
    cropperVisible.value = true
  }
  reader.readAsDataURL(file)
  if (input) {
    input.value = ''
  }
}

function resetCropper(): void {
  cropperImage.value = ''
  pendingAvatarName.value = 'avatar.png'
}

function getCroppedBlob(): Promise<Blob> {
  return new Promise((resolve, reject) => {
    if (!cropperRef.value) {
      reject(new Error('Cropper is not ready'))
      return
    }
    cropperRef.value.getCropBlob((blob: Blob | null) => {
      if (blob) {
        resolve(blob)
        return
      }
      reject(new Error('Failed to crop image'))
    })
  })
}

async function uploadCroppedAvatar(): Promise<void> {
  if (cropperUploading.value) {
    return
  }
  cropperUploading.value = true
  try {
    const blob = await getCroppedBlob()
    const file = new File([blob], pendingAvatarName.value || 'avatar.png', {
      type: blob.type || 'image/png'
    })
    const url = await uploadCurrentAvatar(file)
    profileValues.avatarUrl = url
    patchSessionUser({ avatarUrl: url })
    ElMessage.success('Avatar uploaded.')
    cropperVisible.value = false
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Avatar upload failed'
    ElMessage.error(message)
  } finally {
    cropperUploading.value = false
  }
}

const saveProfileWithValidation = handleProfileSubmit(async () => {
  await saveProfile()
})

const savePasswordWithValidation = handlePasswordSubmit(async (values) => {
  passwordSaving.value = true
  try {
    await changeCurrentPassword({
      oldPassword: values.oldPassword,
      newPassword: values.newPassword
    })
    ElMessage.success('Password updated.')
    resetPasswordForm()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Password update failed'
    ElMessage.error(message)
  } finally {
    passwordSaving.value = false
  }
})
</script>

<template>
  <div class="profile-grid">
    <section class="glass-card card">
      <p class="label">Current account</p>
      <img class="avatar" v-lazy="avatarPreview" alt="Profile avatar" @error="onAvatarError" />
      <h3>{{ accountName }}</h3>
      <p class="muted">Role: {{ accountType }}</p>
      <el-button :loading="loading" round type="primary" @click="loadProfile">Reload Profile</el-button>
    </section>

    <section class="glass-card card wide">
      <p class="label">Profile settings</p>

      <el-alert v-if="warning" :closable="false" show-icon type="warning" :title="warning" />

      <el-form class="form" label-position="top">
        <el-form-item label="Nickname" :error="profileErrors.nickname">
          <el-input v-model="profileValues.nickname" />
        </el-form-item>
        <el-form-item label="Email" :error="profileErrors.email">
          <el-input v-model="profileValues.email" />
        </el-form-item>
        <el-form-item label="Phone" :error="profileErrors.phone">
          <el-input v-model="profileValues.phone" />
        </el-form-item>
        <el-form-item label="Avatar URL" :error="profileErrors.avatarUrl">
          <el-input v-model="profileValues.avatarUrl" />
        </el-form-item>
      </el-form>
      <div class="actions">
        <input ref="fileInputRef" class="hidden-input" type="file" accept="image/*" @change="onAvatarSelected" />
        <el-button round @click="openAvatarPicker">Upload Avatar</el-button>
        <el-button :loading="saveLoading" round type="primary" @click="saveProfileWithValidation">Save Profile</el-button>
      </div>
    </section>

    <section class="glass-card card">
      <p class="label">Security</p>
      <el-form class="form" label-position="top">
        <el-form-item label="Current Password" :error="passwordErrors.oldPassword">
          <el-input v-model="passwordValues.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="New Password" :error="passwordErrors.newPassword">
          <el-input v-model="passwordValues.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="Confirm New Password" :error="passwordErrors.confirmPassword">
          <el-input v-model="passwordValues.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <el-button :loading="passwordSaving" round type="primary" @click="savePasswordWithValidation">
        Update Password
      </el-button>
    </section>

    <section class="glass-card card">
      <p class="label">Platform status</p>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="Web">Ready</el-descriptions-item>
        <el-descriptions-item label="Android">Capacitor Ready</el-descriptions-item>
        <el-descriptions-item label="iOS">Capacitor Ready</el-descriptions-item>
      </el-descriptions>
    </section>

    <el-dialog v-model="cropperVisible" width="640px" align-center @closed="resetCropper">
      <template #header>
        <strong>Crop Avatar</strong>
      </template>
      <div class="cropper-wrapper">
        <VueCropper
          ref="cropperRef"
          :img="cropperImage"
          :auto-crop="true"
          :fixed="true"
          :fixed-number="[1, 1]"
          :can-move-box="true"
          :center-box="true"
          :info="true"
          output-type="png"
        />
      </div>
      <template #footer>
        <el-button round @click="cropperVisible = false">Cancel</el-button>
        <el-button :loading="cropperUploading" round type="primary" @click="uploadCroppedAvatar">Upload</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.card {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.wide {
  grid-column: span 2;
}

.form {
  margin-top: 10px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.hidden-input {
  display: none;
}

.label {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-muted);
}

h3 {
  margin: 8px 0;
}

.muted {
  color: var(--text-muted);
}

.avatar {
  width: 4rem;
  height: 4rem;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 12px 24px rgba(28, 53, 101, 0.2);
  object-fit: cover;
  margin-top: 0.45rem;
}

.cropper-wrapper {
  height: 360px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.35);
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }
}
</style>
