<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { startAuthorization, startGitHubAuthorization } from '../api/auth'

const router = useRouter()
const route = useRoute()

const startingProvider = ref<'password' | 'github' | ''>('')
const redirectPath = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : '/app/home'))
const entryType = computed(() => (typeof route.query.entry === 'string' ? route.query.entry.toLowerCase() : ''))
const entryLabel = computed(() => (entryType.value === 'merchant' ? 'merchant' : 'customer'))

async function handleAuthorizationStart(provider: 'password' | 'github'): Promise<void> {
  startingProvider.value = provider
  try {
    if (provider === 'github') {
      await startGitHubAuthorization(redirectPath.value)
      return
    }
    await startAuthorization(redirectPath.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to start sign in'
    ElMessage.error(message)
    startingProvider.value = ''
  }
}

async function backToMarket(): Promise<void> {
  await router.push('/market')
}
</script>

<template>
  <div class="signin-page">
    <section class="signin-card glass-card">
      <header>
        <p class="eyebrow">My Shop</p>
        <h1>Sign In</h1>
        <p class="muted">The web client now uses OAuth 2.1 authorization code with PKCE only.</p>
      </header>

      <div class="signin-content">
        <div class="signin-hint">
          <p class="hint-title">Unified entry</p>
          <p class="hint-copy">
            {{ entryLabel === 'merchant' ? 'Merchant' : 'Customer' }} access is issued through the same authorization
            server. Role selection is resolved after token issuance, not on the login form.
          </p>
        </div>

        <el-button
          :loading="startingProvider === 'password'"
          class="full-width"
          round
          type="primary"
          @click="handleAuthorizationStart('password')"
        >
          Continue with Authorization Server
        </el-button>

        <div class="oauth-divider-line">
          <span>Or continue with</span>
        </div>

        <div class="oauth-icon-row">
          <button
            class="oauth-icon-btn"
            type="button"
            :disabled="startingProvider === 'github'"
            @click="handleAuthorizationStart('github')"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M12 2C6.48 2 2 6.58 2 12.22c0 4.5 2.87 8.3 6.84 9.65.5.1.68-.22.68-.5 0-.25-.01-1.08-.01-1.96-2.78.62-3.37-1.21-3.37-1.21-.45-1.19-1.1-1.5-1.1-1.5-.9-.63.07-.62.07-.62 1 .07 1.52 1.05 1.52 1.05.88 1.56 2.3 1.11 2.86.85.09-.66.35-1.11.63-1.37-2.22-.26-4.55-1.14-4.55-5.09 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.31.1-2.73 0 0 .84-.28 2.75 1.05A9.3 9.3 0 0 1 12 6.9c.85 0 1.7.12 2.5.36 1.9-1.33 2.74-1.05 2.74-1.05.55 1.42.2 2.47.1 2.73.64.72 1.03 1.64 1.03 2.76 0 3.96-2.33 4.82-4.56 5.08.36.32.68.94.68 1.9 0 1.37-.01 2.47-.01 2.8 0 .28.18.6.69.5A10.23 10.23 0 0 0 22 12.22C22 6.58 17.52 2 12 2Z"
              />
            </svg>
          </button>
        </div>
        <p class="oauth-entry-tip">GitHub Sign In</p>
        <p class="oauth-note">
          GitHub sign-in returns to the same OAuth callback flow as the native authorization server.
        </p>

        <el-button class="full-width back-market" plain round @click="backToMarket">
          Back to Marketplace
        </el-button>
      </div>
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

.signin-content {
  display: grid;
  gap: 0.95rem;
}

.signin-hint {
  border: 1px solid rgba(27, 44, 74, 0.08);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 18px;
  padding: 0.9rem 1rem;
}

.hint-title {
  margin: 0 0 0.35rem 0;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--accent);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.hint-copy {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.5;
  font-size: 0.9rem;
}

.oauth-divider-line {
  margin: 12px 0 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 0.82rem;
}

.oauth-divider-line::before,
.oauth-divider-line::after {
  content: '';
  height: 1px;
  background: rgba(41, 54, 82, 0.18);
  flex: 1;
}

.oauth-icon-row {
  display: flex;
  justify-content: center;
}

.oauth-icon-btn {
  width: 52px;
  height: 52px;
  border: 1px solid rgba(33, 41, 58, 0.2);
  border-radius: 50%;
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.oauth-icon-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(33, 41, 58, 0.16);
}

.oauth-icon-btn:disabled {
  cursor: wait;
  opacity: 0.7;
}

.oauth-icon-btn svg {
  width: 28px;
  height: 28px;
  fill: #111111;
}

.oauth-entry-tip {
  margin: 8px 0 0;
  text-align: center;
  color: var(--text-main);
  font-size: 0.9rem;
}

.oauth-note {
  margin: 12px 0 0;
  color: var(--text-muted);
  font-size: 0.8rem;
}

.back-market {
  margin-top: 10px;
}
</style>