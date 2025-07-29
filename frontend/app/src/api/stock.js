import axios from 'axios'

// 创建axios实例
const request = axios.create({
    baseURL: '', // 使用相对路径，让vite代理处理
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        console.log('发送请求:', config)
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        console.log('响应数据:', response.data)
        return response.data
    },
    error => {
        console.error('响应错误:', error)
        return Promise.reject(error)
    }
)

// 库存API
export const stockApi = {
    // 根据商品ID查询库存
    getByProductId(productId) {
        return request.get(`/stock/product/${productId}`)
    },

    // 分页查询库存
    pageQuery(params) {
        return request.post('/stock/page', params)
    },

    // 异步根据商品ID查询库存
    getByProductIdAsync(productId) {
        return request.get(`/stock/async/product/${productId}`)
    },

    // 异步分页查询库存
    pageQueryAsync(params) {
        return request.post('/stock/async/page', params)
    },

    // 异步批量查询库存
    batchQueryAsync(productIds) {
        return request.post('/stock/async/batch', productIds)
    },

    // 异步查询库存统计
    getStatistics() {
        return request.get('/stock/async/statistics')
    },

    // 并发查询多个商品库存
    concurrentQuery(productIds) {
        return request.post('/stock/async/concurrent', productIds)
    }
}