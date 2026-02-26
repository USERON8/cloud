<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

async function goHome(): Promise<void> {
  await router.replace('/market')
}
</script>

<template>
  <div class="forbidden-page">
    <section class="glass-card forbidden-card">
      <p class="eyebrow">403</p>
      <h1>Access denied</h1>
      <p class="muted">Your role does not have permission to access this page.</p>
      <el-button round type="primary" @click="goHome">Back to Home</el-button>
    </section>
  </div>
</template>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
}

.forbidden-card {
  width: min(440px, 100%);
  padding: 24px;
}

.eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: 0.82rem;
  font-weight: 700;
}

h1 {
  margin: 8px 0;
}

.muted {
  margin: 0 0 18px 0;
  color: var(--text-muted);
}
</style>
