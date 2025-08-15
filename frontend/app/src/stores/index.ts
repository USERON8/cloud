import { createPinia } from 'pinia'
import { useUserStore } from './user'

// 创建pinia实例
const pinia = createPinia()

// 导出store
export { useUserStore }

// 导出pinia实例
export default pinia