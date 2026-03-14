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
const failureMessage = ref('无法完成登录，请稍后重试。')

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
      throw new Error('缺少授权参数')
    }

    const tokenResponse = await exchangeAuthorizationCode(code, state)
    setSessionFromTokenResponse(tokenResponse)
    const redirectPath = consumePendingRedirectPath()
    toast('登录成功', 'success')
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
        <text class="title">正在处理登录</text>
        <text class="muted">请稍候，正在完成授权。</text>
      </template>

      <template v-else-if="failed">
        <text class="eyebrow">OAuth 2.1</text>
        <text class="title">登录失败</text>
        <text class="muted">{{ failureMessage }}</text>
        <button class="btn-primary" @click="backToLogin">返回登录</button>
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
  padding: 16px;
}

.card {
  width: min(420px, 100%);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.eyebrow {
  font-size: 12px;
  color: var(--accent);
  font-weight: 600;
}

.title {
  font-size: 20px;
  font-weight: 600;
}

.muted {
  color: var(--text-muted);
  font-size: 12px;
}
</style>
