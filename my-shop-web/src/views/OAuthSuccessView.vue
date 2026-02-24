<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { setSessionFromLogin } from '../auth/session'
import type { LoginResponse } from '../types/domain'

const route = useRoute()
const router = useRouter()
const processing = ref(true)
const failed = ref(false)

function decodePayload(raw: string): LoginResponse {
  const normalized = raw.replace(/\+/g, '%20')
  const decoded = decodeURIComponent(normalized)
  return JSON.parse(decoded) as LoginResponse
}

onMounted(async () => {
  const raw = route.query.data
  if (typeof raw !== 'string' || raw.length === 0) {
    failed.value = true
    processing.value = false
    return
  }

  try {
    const payload = decodePayload(raw)
    setSessionFromLogin(payload)
    ElMessage.success('GitHub authentication succeeded.')
    await router.replace('/home')
  } catch (error) {
    failed.value = true
    const message = error instanceof Error ? error.message : 'Failed to process GitHub login result'
    ElMessage.error(message)
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
        <p class="eyebrow">GitHub</p>
        <h1>Processing sign-in</h1>
        <p class="muted">Please wait while we finish your authentication.</p>
      </template>

      <template v-else-if="failed">
        <p class="eyebrow">GitHub</p>
        <h1>Authentication failed</h1>
        <p class="muted">Unable to read GitHub login result. Please try again.</p>
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
