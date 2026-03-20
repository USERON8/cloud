<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { startAuthorization, startGitHubAuthorization } from '../../api/auth'
import { navigateTo } from '../../router/navigation'
import { Routes } from '../../router/routes'
import { toast } from '../../utils/ui'

const redirectPath = ref<string>(Routes.appHome)
const entryType = ref('')
const startingProvider = ref<'password' | 'github' | ''>('')

const entryLabel = computed(() => (entryType.value === 'merchant' ? 'merchant' : 'customer'))

onLoad((query) => {
  if (typeof query.redirect === 'string') {
    try {
      redirectPath.value = decodeURIComponent(query.redirect)
    } catch {
      redirectPath.value = query.redirect
    }
  }
  if (typeof query.entry === 'string') {
    entryType.value = query.entry.toLowerCase()
  }
})

async function handleAuthorizationStart(provider: 'password' | 'github'): Promise<void> {
  startingProvider.value = provider
  try {
    if (provider === 'github') {
      await startGitHubAuthorization(redirectPath.value)
      return
    }
    await startAuthorization(redirectPath.value)
  } catch (error) {
    toast(error instanceof Error ? error.message : '无法发起登录')
    startingProvider.value = ''
  }
}

function backToMarket(): void {
  navigateTo(Routes.market)
}
</script>

<template>
  <view class="page">
    <view class="signin-card glass-card">
      <view class="header">
        <text class="eyebrow">My Shop</text>
        <text class="title">登录</text>
        <text class="muted">统一 OAuth 2.1 授权入口</text>
      </view>

      <view class="signin-content">
        <view class="signin-hint">
          <text class="hint-title">统一入口</text>
          <text class="hint-copy">
            {{ entryLabel === 'merchant' ? '商家' : '用户' }} 登录后角色由授权服务器返回。
          </text>
        </view>

        <button
          class="btn-primary full-width"
          :loading="startingProvider === 'password'"
          @click="handleAuthorizationStart('password')"
        >
          通过授权服务器继续
        </button>

        <view class="divider">
          <text>或者</text>
        </view>

        <button
          class="btn-outline full-width"
          :loading="startingProvider === 'github'"
          @click="handleAuthorizationStart('github')"
        >
          GitHub 登录
        </button>

        <button class="btn-outline full-width" @click="backToMarket">返回商城</button>
      </view>
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

.signin-card {
  width: min(420px, 100%);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header {
  display: flex;
  flex-direction: column;
  gap: 6px;
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

.signin-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.signin-hint {
  border: 1px solid rgba(27, 44, 74, 0.08);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 18px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hint-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
}

.hint-copy {
  color: var(--text-muted);
  font-size: 12px;
}

.full-width {
  width: 100%;
}

.divider {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 12px;
}
</style>
