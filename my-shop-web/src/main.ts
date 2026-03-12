import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import VueLazyload from 'vue-lazyload'
import { MotionPlugin } from '@vueuse/motion'

const app = createApp(App)
app.use(router)
app.use(VueLazyload, {
  observer: true
})
app.use(MotionPlugin)
app.mount('#app')
