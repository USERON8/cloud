import {defineStore} from 'pinia'
import {stockApi} from '@/api/stock'
import { ElMessage } from 'element-plus'

export const useStockStore = defineStore('stock', {
    state: () => ({
        // 库存列表
        stockList: [],
        // 库存分页信息
        stockPagination: {
            current: 1,
            size: 10,
            total: 0,
            pages: 0
        },
        // 当前选中的库存详情
        currentStock: null,
        // 加载状态
        loading: false
    }),

    actions: {
        /**
         * 分页查询库存列表
         * @param {Object} params 查询参数
         */
        async fetchStockList(params = {}) {
            this.loading = true
            try {
                const response = await stockApi.getStockList({
                    current: this.stockPagination.current,
                    size: this.stockPagination.size,
                    ...params
                })

                if (response.code === 200) {
                    this.stockList = response.data.records
                    this.stockPagination = {
                        current: response.data.current,
                        size: response.data.size,
                        total: response.data.total,
                        pages: response.data.pages
                    }
                    return response
                } else {
                    ElMessage.error(response.message || '查询库存列表失败')
                    throw new Error(response.message || '查询库存列表失败')
                }
            } catch (error) {
                console.error('查询库存列表失败:', error)
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 根据商品ID获取库存详情
         * @param {number} productId 商品ID
         */
        async fetchStockByProductId(productId) {
            this.loading = true
            try {
                const response = await stockApi.getStockByProductId(productId)

                if (response.code === 200) {
                    this.currentStock = response.data
                    return response
                } else {
                    ElMessage.error(response.message || '查询库存详情失败')
                    throw new Error(response.message || '查询库存详情失败')
                }
            } catch (error) {
                console.error('查询库存详情失败:', error)
                ElMessage.error('查询库存详情失败')
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 设置当前页码
         * @param {number} current 页码
         */
        setCurrentPage(current) {
            this.stockPagination.current = current
        },

        /**
         * 设置每页大小
         * @param {number} size 每页大小
         */
        setPageSize(size) {
            this.stockPagination.size = size
        },

        /**
         * 清除当前库存详情
         */
        clearCurrentStock() {
            this.currentStock = null
        }
    }
})