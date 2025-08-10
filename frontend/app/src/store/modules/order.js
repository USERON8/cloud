import {defineStore} from 'pinia'
import {orderApi} from '@/api/order'
import { ElMessage } from 'element-plus'

export const useOrderStore = defineStore('order', {
    state: () => ({
        // 订单列表
        orderList: [],
        // 订单分页信息
        orderPagination: {
            current: 1,
            size: 10,
            total: 0,
            pages: 0
        },
        // 当前选中的订单详情
        currentOrder: null,
        // 加载状态
        loading: false
    }),

    actions: {
        /**
         * 分页查询订单列表
         * @param {Object} params 查询参数
         */
        async fetchOrderList(params = {}) {
            this.loading = true
            try {
                const response = await orderApi.getOrderList({
                    current: this.orderPagination.current,
                    size: this.orderPagination.size,
                    ...params
                })

                if (response.code === 200) {
                    this.orderList = response.data.records
                    this.orderPagination = {
                        current: response.data.current,
                        size: response.data.size,
                        total: response.data.total,
                        pages: response.data.pages
                    }
                    return response
                } else {
                    ElMessage.error(response.message || '查询订单列表失败')
                    throw new Error(response.message || '查询订单列表失败')
                }
            } catch (error) {
                console.error('查询订单列表失败:', error)
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 获取订单详情
         * @param {number} id 订单ID
         */
        async fetchOrderItem(id) {
            this.loading = true
            try {
                const response = await orderApi.getOrderItem(id)

                if (response.code === 200) {
                    this.currentOrder = response.data
                    return response
                } else {
                    ElMessage.error(response.message || '查询订单详情失败')
                    throw new Error(response.message || '查询订单详情失败')
                }
            } catch (error) {
                console.error('查询订单详情失败:', error)
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 创建订单
         * @param {Object} data 订单数据
         */
        async createOrder(data) {
            try {
                const response = await orderApi.createOrder(data)

                if (response.code === 200) {
                    ElMessage.success(response.message || '订单创建成功')
                    return response
                } else {
                    ElMessage.error(response.message || '创建订单失败')
                    throw new Error(response.message || '创建订单失败')
                }
            } catch (error) {
                console.error('创建订单失败:', error)
                throw error
            }
        },

        /**
         * 更新订单
         * @param {number} id 订单ID
         * @param {Object} data 订单数据
         */
        async updateOrder(id, data) {
            try {
                const response = await orderApi.updateOrder(id, data)

                if (response.code === 200) {
                    ElMessage.success(response.message || '订单更新成功')
                    return response
                } else {
                    ElMessage.error(response.message || '更新订单失败')
                    throw new Error(response.message || '更新订单失败')
                }
            } catch (error) {
                console.error('更新订单失败:', error)
                throw error
            }
        },

        /**
         * 删除订单
         * @param {number} id 订单ID
         */
        async deleteOrder(id) {
            try {
                const response = await orderApi.deleteOrder(id)

                if (response.code === 200) {
                    ElMessage.success(response.message || '订单删除成功')
                    return response
                } else {
                    ElMessage.error(response.message || '删除订单失败')
                    throw new Error(response.message || '删除订单失败')
                }
            } catch (error) {
                console.error('删除订单失败:', error)
                throw error
            }
        },

        /**
         * 设置当前页码
         * @param {number} current 页码
         */
        setCurrentPage(current) {
            this.orderPagination.current = current
        },

        /**
         * 设置每页大小
         * @param {number} size 每页大小
         */
        setPageSize(size) {
            this.orderPagination.size = size
        },

        /**
         * 清除当前订单详情
         */
        clearCurrentOrder() {
            this.currentOrder = null
        }
    }
})