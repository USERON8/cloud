<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const errorMessage = computed(() => {
  const raw = route.query.message
  if (typeof raw !== 'string' || raw.length === 0) {
    return 'GitHub authentication failed.'
  }
  try {
    return decodeURIComponent(raw.replace(/\+/g, '%20'))
  } catch {
    return raw
  }
})

async function backToLogin(): Promise<void> {
  await router.replace('/login')
}
</script>

<template>
  <div class="oauth-page">
    <section class="glass-card oauth-card">
      <p class="eyebrow">GitHub</p>
      <h1>Authentication failed</h1>
      <p class="muted">{{ errorMessage }}</p>
      <el-button round type="primary" @click="backToLogin">Back to Sign In</el-button>
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
