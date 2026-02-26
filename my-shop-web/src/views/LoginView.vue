<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { setSessionFromLogin } from '../auth/session'
import type { LoginRequest } from '../types/domain'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const form = reactive<LoginRequest>({
  username: '',
  password: '',
  userType: 'USER'
})

const entryType = typeof route.query.entry === 'string' ? route.query.entry.toLowerCase() : ''
if (entryType === 'merchant') {
  form.userType = 'MERCHANT'
}

const userTypeOptions: Array<{ label: string; value: LoginRequest['userType'] }> = [
  { label: 'Consumer', value: 'USER' },
  { label: 'Merchant', value: 'MERCHANT' }
]

const githubAuthorizePath = import.meta.env.VITE_GITHUB_AUTHORIZE_URL || '/oauth2/authorization/github'

async function submitLogin(): Promise<void> {
  if (!form.username || !form.password) {
    ElMessage.warning('Please input username and password.')
    return
  }

  if (form.userType === 'ADMIN') {
    form.userType = 'USER'
  }

  loading.value = true
  try {
    const result = await login(form)
    setSessionFromLogin(result)
    ElMessage.success('Signed in successfully.')

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/app/home'
    await router.replace(redirect)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Sign in failed'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function signInWithGitHub(): void {
  localStorage.setItem('shop.oauth.intent', 'auto')
  window.location.href = githubAuthorizePath
}
</script>

<template>
  <div class="signin-page">
    <section class="signin-card glass-card">
      <header>
        <p class="eyebrow">My Shop</p>
        <h1>Sign In</h1>
        <p class="muted">One account for Web, Android, and iOS.</p>
      </header>

      <el-form label-position="top" @submit.prevent="submitLogin">
        <el-form-item label="Username">
          <el-input v-model="form.username" autocomplete="username" placeholder="4-20 characters" />
        </el-form-item>

        <el-form-item label="Password">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            show-password
            type="password"
            placeholder="8-20 characters"
          />
        </el-form-item>

        <el-form-item label="Account Type">
          <el-select v-model="form.userType" class="full-width">
            <el-option
              v-for="item in userTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-button :loading="loading" class="full-width" round type="primary" @click="submitLogin">
          Continue
        </el-button>

        <div class="oauth-divider-line">
          <span>其他方式登录</span>
        </div>

        <div class="oauth-icon-row">
          <button class="oauth-icon-btn" type="button" @click="signInWithGitHub">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M12 2C6.48 2 2 6.58 2 12.22c0 4.5 2.87 8.3 6.84 9.65.5.1.68-.22.68-.5 0-.25-.01-1.08-.01-1.96-2.78.62-3.37-1.21-3.37-1.21-.45-1.19-1.1-1.5-1.1-1.5-.9-.63.07-.62.07-.62 1 .07 1.52 1.05 1.52 1.05.88 1.56 2.3 1.11 2.86.85.09-.66.35-1.11.63-1.37-2.22-.26-4.55-1.14-4.55-5.09 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.31.1-2.73 0 0 .84-.28 2.75 1.05A9.3 9.3 0 0 1 12 6.9c.85 0 1.7.12 2.5.36 1.9-1.33 2.74-1.05 2.74-1.05.55 1.42.2 2.47.1 2.73.64.72 1.03 1.64 1.03 2.76 0 3.96-2.33 4.82-4.56 5.08.36.32.68.94.68 1.9 0 1.37-.01 2.47-.01 2.8 0 .28.18.6.69.5A10.23 10.23 0 0 0 22 12.22C22 6.58 17.52 2 12 2Z"
              />
            </svg>
          </button>
        </div>
        <p class="oauth-entry-tip">GitHub 登录/注册</p>
        <p class="oauth-note">
          First GitHub login will create a new account automatically.
        </p>

        <el-button class="full-width back-market" plain round @click="$router.push('/market')">
          返回访客主页
        </el-button>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.signin-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
}

.signin-card {
  width: min(420px, 100%);
  padding: 22px;
}

header h1 {
  margin: 6px 0;
}

.eyebrow {
  margin: 0;
  font-size: 0.82rem;
  color: var(--accent);
  font-weight: 600;
}

.muted {
  margin: 0 0 18px 0;
  color: var(--text-muted);
}

.full-width {
  width: 100%;
}

.oauth-divider-line {
  margin: 12px 0 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 0.82rem;
}

.oauth-divider-line::before,
.oauth-divider-line::after {
  content: '';
  height: 1px;
  background: rgba(41, 54, 82, 0.18);
  flex: 1;
}

.oauth-icon-row {
  display: flex;
  justify-content: center;
}

.oauth-icon-btn {
  width: 52px;
  height: 52px;
  border: 1px solid rgba(33, 41, 58, 0.2);
  border-radius: 50%;
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.oauth-icon-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(33, 41, 58, 0.16);
}

.oauth-icon-btn svg {
  width: 28px;
  height: 28px;
  fill: #111111;
}

.oauth-entry-tip {
  margin: 8px 0 0;
  text-align: center;
  color: var(--text-main);
  font-size: 0.9rem;
}

.oauth-note {
  margin: 12px 0 0;
  color: var(--text-muted);
  font-size: 0.8rem;
}

.back-market {
  margin-top: 10px;
}
</style>
