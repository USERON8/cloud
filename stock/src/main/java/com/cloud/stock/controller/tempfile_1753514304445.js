import {createApp} from 'vue'
import App from './App.vue'

// 引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 创建应用实例
const app = createApp(App)

// 使用Element Plus
app.use(ElementPlus)

// 挂载应用
app.mount('#app')
