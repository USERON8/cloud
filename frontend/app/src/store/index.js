import {useAppStore} from './index'

// 按名导出各个 store
export {useAppStore} from './app'
export {useAuthStore} from './modules/auth'
export {useUserStore} from './modules/user'
export {useStockStore} from './modules/stock'
export {useOrderStore} from './modules/order'
export {useProductStore} from './modules/product'

// 默认导出对象
export default {
    useAppStore,
    useAuthStore,
    useUserStore,
    useStockStore,
    useOrderStore,
    useProductStore
}