<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { redirectTo } from '../../router/navigation'
import { Routes } from '../../router/routes'

const message = ref('µÇÂ¼Ê§°Ü£¬ÇëÉÔºóÖØÊÔ¡£')

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
    <view class="card glass-card">
      <text class="title">ÈÏÖ¤Ê§°Ü</text>
      <text class="muted">{{ message }}</text>
      <button class="btn-primary" @click="backToLogin">·µ»ØµÇÂ¼</button>
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
  text-align: center;
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
