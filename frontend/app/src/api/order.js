import request from './axiosInstance'

// 订单服务API
export const orderApi = {
    /**
     * 获取订单列表
     * @param {Object} params 查询参数
     * @returns {Promise} 订单列表响应
     */
    getOrderList(params) {
        return request({
            url: '/api/order/list',
            method: 'get',
            params: params
        }).catch(error => {
            console.error('获取订单列表失败:', error)
            return Promise.resolve({
                code: 500,
                message: '获取订单列表失败',
                data: {
                    records: [],
                    total: 0,
                    current: params.current || 1,
                    size: params.size || 10
                }
            })
        })
    },

    /**
     * 获取订单详情
     * @param {number} id 订单ID
     * @returns {Promise} 订单详情响应
     */
    getOrderItem(id) {
        return request({
            url: `/api/order/${id}`,
            method: 'get'
        }).catch(error => {
            console.error(`获取订单详情失败(ID: ${id}):`, error)
            return Promise.resolve({
                code: 500,
                message: '获取订单详情失败',
                data: null
            })
        })
    },

    /**
     * 创建订单
     * @param {Object} data 订单数据
     * @returns {Promise} 创建响应
     */
    createOrder(data) {
        return request({
            url: '/api/order',
            method: 'post',
            data: data
        }).catch(error => {
            console.error('创建订单失败:', error)
            return Promise.resolve({
                code: 500,
                message: '创建订单失败',
                data: null
            })
        })
    },

    /**
     * 更新订单
     * @param {number} id 订单ID
     * @param {Object} data 订单数据
     * @returns {Promise} 更新响应
     */
    updateOrder(id, data) {
        return request({
            url: `/api/order/${id}`,
            method: 'put',
            data: data
        }).catch(error => {
            console.error(`更新订单失败(ID: ${id}):`, error)
            return Promise.resolve({
                code: 500,
                message: '更新订单失败',
                data: null
            })
        })
    },

    /**
     * 删除订单
     * @param {number} id 订单ID
     * @returns {Promise} 删除响应
     */
    deleteOrder(id) {
        return request({
            url: `/api/order/${id}`,
            method: 'delete'
        }).catch(error => {
            console.error(`删除订单失败(ID: ${id}):`, error)
            return Promise.resolve({
                code: 500,
                message: '删除订单失败',
                data: null
            })
        })
    }
}