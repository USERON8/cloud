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

const userTypeOptions: Array<{ label: string; value: LoginRequest['userType'] }> = [
  { label: 'Consumer', value: 'USER' },
  { label: 'Merchant', value: 'MERCHANT' },
  { label: 'Administrator', value: 'ADMIN' }
]

const githubAuthorizePath = import.meta.env.VITE_GITHUB_AUTHORIZE_URL || '/oauth2/authorization/github'

async function submitLogin(): Promise<void> {
  if (!form.username || !form.password) {
    ElMessage.warning('Please input username and password.')
    return
  }

  loading.value = true
  try {
    const result = await login(form)
    setSessionFromLogin(result)
    ElMessage.success('Signed in successfully.')

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    await router.replace(redirect)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Sign in failed'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function signInWithGitHub(mode: 'login' | 'register'): void {
  localStorage.setItem('shop.oauth.intent', mode)
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

        <div class="oauth-divider">or</div>

        <el-button class="full-width" plain round @click="signInWithGitHub('login')">
          Continue with GitHub
        </el-button>
        <el-button class="full-width" plain round type="success" @click="signInWithGitHub('register')">
          Register with GitHub
        </el-button>
        <p class="oauth-note">
          First GitHub login will create a new account automatically.
        </p>
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

.oauth-divider {
  text-align: center;
  color: var(--text-muted);
  margin: 12px 0 10px;
  font-size: 0.82rem;
}

.oauth-note {
  margin: 12px 0 0;
  color: var(--text-muted);
  font-size: 0.8rem;
}
</style>
