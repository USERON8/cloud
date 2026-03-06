<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  clearPendingAuthorizationState,
  consumePendingRedirectPath,
  exchangeAuthorizationCode
} from '../api/auth'
import { setSessionFromTokenResponse } from '../auth/session'

const route = useRoute()
const router = useRouter()
const processing = ref(true)
const failed = ref(false)
const failureMessage = ref('Unable to complete OAuth sign-in. Please try again.')

onMounted(async () => {
  const code = typeof route.query.code === 'string' ? route.query.code : ''
  const state = typeof route.query.state === 'string' ? route.query.state : ''
  const oauthError = typeof route.query.error === 'string' ? route.query.error : ''
  const oauthErrorDescription =
    typeof route.query.error_description === 'string' ? route.query.error_description : ''

  try {
    if (oauthError) {
      throw new Error(oauthErrorDescription || oauthError)
    }
    if (!code || !state) {
      throw new Error('Missing authorization code or state')
    }

    const tokenResponse = await exchangeAuthorizationCode(code, state)
    setSessionFromTokenResponse(tokenResponse)
    const redirectPath = consumePendingRedirectPath()
    ElMessage.success('Sign in completed.')
    await router.replace(redirectPath)
  } catch (error) {
    clearPendingAuthorizationState()
    failed.value = true
    failureMessage.value = error instanceof Error ? error.message : failureMessage.value
    ElMessage.error(failureMessage.value)
  } finally {
    processing.value = false
  }
})

async function backToLogin(): Promise<void> {
  await router.replace('/login')
}
</script>

<template>
  <div class="oauth-page">
    <section class="glass-card oauth-card">
      <template v-if="processing">
        <p class="eyebrow">OAuth 2.1</p>
        <h1>Processing sign-in</h1>
        <p class="muted">Exchanging the authorization code and establishing your application session.</p>
      </template>

      <template v-else-if="failed">
        <p class="eyebrow">OAuth 2.1</p>
        <h1>Authentication failed</h1>
        <p class="muted">{{ failureMessage }}</p>
        <el-button round type="primary" @click="backToLogin">Back to Sign In</el-button>
      </template>
    </section>
  </div>
</template>

<style scoped>
.oauth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
}

.oauth-card {
  width: min(440px, 100%);
  padding: 24px;
}

.eyebrow {
  margin: 0;
  font-size: 0.82rem;
  color: var(--accent);
  font-weight: 600;
}

h1 {
  margin: 8px 0;
}

.muted {
  margin: 0 0 14px;
  color: var(--text-muted);
}
</style>