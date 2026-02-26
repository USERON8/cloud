<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentProfile, updateCurrentProfile } from '../api/user'
import { patchSessionUser, sessionState } from '../auth/session'
import type { UserInfo } from '../types/domain'

const loading = ref(false)
const saveLoading = ref(false)
const warning = ref('')
const avatarLoadFailed = ref(false)

const form = reactive<Required<Pick<UserInfo, 'nickname' | 'email' | 'phone' | 'avatarUrl'>>>(
  {
    nickname: '',
    email: '',
    phone: '',
    avatarUrl: ''
  }
)

const accountName = computed(() => sessionState.user?.username || 'Unknown')
const accountType = computed(() => sessionState.user?.userType || 'Unknown')
const avatarPreview = computed(() => {
  if (form.avatarUrl.trim() && !avatarLoadFailed.value) {
    return form.avatarUrl.trim()
  }
  const c = (form.nickname.trim()[0] || accountName.value[0] || 'U').toUpperCase()
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
  form.nickname = data.nickname || ''
  form.email = data.email || ''
  form.phone = data.phone || ''
  form.avatarUrl = data.avatarUrl || ''
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
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
      avatarUrl: form.avatarUrl
    })
    patchSessionUser({
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
      avatarUrl: form.avatarUrl
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
  () => form.avatarUrl,
  () => {
    avatarLoadFailed.value = false
  }
)

function onAvatarError(): void {
  avatarLoadFailed.value = true
}
</script>

<template>
  <div class="profile-grid">
    <section class="glass-card card">
      <p class="label">Current account</p>
      <img class="avatar" :src="avatarPreview" alt="Profile avatar" @error="onAvatarError" />
      <h3>{{ accountName }}</h3>
      <p class="muted">Role: {{ accountType }}</p>
      <el-button :loading="loading" round type="primary" @click="loadProfile">Reload Profile</el-button>
    </section>

    <section class="glass-card card wide">
      <p class="label">Profile settings</p>

      <el-alert v-if="warning" :closable="false" show-icon type="warning" :title="warning" />

      <el-form class="form" label-position="top">
        <el-form-item label="Nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="Avatar URL">
          <el-input v-model="form.avatarUrl" />
        </el-form-item>
      </el-form>

      <el-button :loading="saveLoading" round type="primary" @click="saveProfile">Save Profile</el-button>
    </section>

    <section class="glass-card card">
      <p class="label">Platform status</p>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="Web">Ready</el-descriptions-item>
        <el-descriptions-item label="Android">Capacitor Ready</el-descriptions-item>
        <el-descriptions-item label="iOS">Capacitor Ready</el-descriptions-item>
      </el-descriptions>
    </section>
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

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }
}
</style>
