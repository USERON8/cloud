import axios from './axiosInstance'; 

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