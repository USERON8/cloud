<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { clearPendingAuthorizationState, consumePendingRedirectPath, exchangeAuthorizationCode } from '../../api/auth'
import { setSessionFromTokenResponse } from '../../auth/session'
import { redirectTo } from '../../router/navigation'
import { Routes } from '../../router/routes'
import { toast } from '../../utils/ui'

const processing = ref(true)
const failed = ref(false)
const failureMessage = ref('Unable to complete sign-in. Please try again later.')

async function handleOAuth(query: Record<string, unknown>): Promise<void> {
  const code = typeof query.code === 'string' ? query.code : ''
  const state = typeof query.state === 'string' ? query.state : ''
  const oauthError = typeof query.error === 'string' ? query.error : ''
  const oauthErrorDescription =
    typeof query.error_description === 'string' ? query.error_description : ''

  try {
    if (oauthError) {
      throw new Error(oauthErrorDescription || oauthError)
    }
    if (!code || !state) {
      throw new Error('Missing authorization parameters')
    }

    const tokenResponse = await exchangeAuthorizationCode(code, state)
    setSessionFromTokenResponse(tokenResponse)
    const redirectPath = consumePendingRedirectPath()
    toast('Signed in successfully', 'success')
    uni.reLaunch({ url: redirectPath })
  } catch (error) {
    clearPendingAuthorizationState()
    failed.value = true
    failureMessage.value = error instanceof Error ? error.message : failureMessage.value
    toast(failureMessage.value)
  } finally {
    processing.value = false
  }
}

function backToLogin(): void {
  redirectTo(Routes.login)
}

onLoad((query) => {
  void handleOAuth(query || {})
})
</script>

<template>
  <view class="page">
    <view class="card glass-card">
      <template v-if="processing">
        <text class="eyebrow">OAuth 2.1</text>
        <text class="title">Processing sign-in</text>
        <text class="muted">Please wait while the authorization is being completed.</text>
      </template>

      <template v-else-if="failed">
        <text class="eyebrow">OAuth 2.1</text>
        <text class="title">Sign-in failed</text>
        <text class="muted">{{ failureMessage }}</text>
        <button class="btn-primary" @click="backToLogin">Back to sign in</button>
      </template>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  color: #15171a;
  background:
    linear-gradient(180deg, rgba(248, 249, 251, 0.96), #ffffff 44%),
    #ffffff;
}

.card {
  width: min(420px, 100%);
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid #edf0f4;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 58px rgba(18, 24, 35, 0.05);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.eyebrow {
  font-size: 12px;
  color: #69727d;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title {
  color: #101216;
  font-size: 30px;
  line-height: 1.12;
  font-weight: 900;
}

.muted {
  color: #69727d;
  font-size: 14px;
  line-height: 1.7;
}

.card :deep(.btn-primary) {
  border-radius: 4px;
  background: #111316;
  border: 1px solid #111316;
  color: #ffffff;
  box-shadow: none;
}
</style>
