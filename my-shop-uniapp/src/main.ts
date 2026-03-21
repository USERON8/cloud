import { createSSRApp } from 'vue'
import App from './App.vue'
import { hydrateSessionFromStorage } from './auth/session'
import { hydrateCartFromStorage } from './store/cart'
import { pinia } from './stores/pinia'

export function createApp() {
  const app = createSSRApp(App)
  app.use(pinia)
  hydrateSessionFromStorage()
  hydrateCartFromStorage()
  return { app }
}
