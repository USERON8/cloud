<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { redirectTo } from '../../router/navigation'
import { Routes } from '../../router/routes'

const message = ref('Sign-in failed. Please try again later.')

onLoad((query) => {
  if (typeof query.message === 'string') {
    message.value = query.message
  }
})

function backToLogin(): void {
  redirectTo(Routes.login)
}
</script>

<template>
  <view class="page">
    <view class="error-card glass-card">
      <view class="status-mark">
        <text class="status-code">!</text>
      </view>
      <text class="hero-eyebrow">Authorization failed</text>
      <text class="title">The sign-in flow could not be completed.</text>
      <text class="description">{{ message }}</text>
      <view class="actions">
        <button class="btn-primary" @click="backToLogin">Back to sign in</button>
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

.error-card {
  width: min(420px, 100%);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.status-mark {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--highlight), var(--accent));
  color: #07131f;
  box-shadow: 0 18px 34px rgba(1, 7, 14, 0.28);
}

.status-code {
  font-size: 18px;
  font-weight: 800;
}

.title {
  font-size: 30px;
  line-height: 1.12;
  font-weight: 800;
}

.description {
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.actions {
  display: flex;
  width: 100%;
  padding-top: 4px;
}

.actions button {
  flex: 1;
}

@media (max-width: 520px) {
  .error-card {
    padding: 22px;
  }

  .title {
    font-size: 24px;
  }
}
</style>
