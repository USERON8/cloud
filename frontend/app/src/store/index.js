import {useAppStore} from './index'

// 按名导出各个 store
export {useAppStore} from './app'
export {useAuthStore} from './modules/auth'
export {useUserStore} from './modules/user'
export {useStockStore} from './modules/stock'
export {useOrderStore} from './modules/order'
export {useProductStore} from './modules/product'

// 2. 初始化 Pinia (SSR 兼容方案)
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const initPinia = () => {
    const pinia = createPinia()
    pinia.use(piniaPluginPersistedstate)
    return pinia
}

// 3. 默认导出初始化方法
export default initPinia