import { createSSRApp } from 'vue'
import App from './App.vue'
import { hydrateSessionFromStorage } from './auth/session'
import { hydrateCartFromStorage } from './store/cart'

export function createApp() {
  hydrateSessionFromStorage()
  hydrateCartFromStorage()
  const app = createSSRApp(App)
  return { app }
}
