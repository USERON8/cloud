import request from './axiosInstance'

export const stockApi = {
    /**
     * 根据商品ID获取库存信息
     * @param {number} productId 商品ID
     * @returns {Promise} 库存信息响应
     */
    getStockByProductId(productId) {
        return request({
            url: `/api/stock/product/${productId}`,
            method: 'get'
        })
    },

    /**
     * 分页查询库存列表
     * @param {Object} data 分页查询参数
     * @param {number} data.page 页码
     * @param {number} data.size 每页大小
     * @param {string} data.productName 商品名称（可选）
     * @returns {Promise} 库存列表响应
     */
    getStockList(data) {
        return request({
            url: '/api/stock/page',
            method: 'post',
            data
        })
    },

    /**
     * 获取库存详情
     * @param {number} id 库存ID
     * @returns {Promise} 库存详情响应
     */
    getStockItem(id) {
        return request({
            url: `/api/stock/product/${id}`,
            method: 'get'
        })
    }
}