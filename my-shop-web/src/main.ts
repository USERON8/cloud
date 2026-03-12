import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'
import VueLazyload from 'vue-lazyload'

const app = createApp(App)
app.use(router)
app.use(VueLazyload, {
  observer: true
})
app.mount('#app')
