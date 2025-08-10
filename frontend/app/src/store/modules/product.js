import {defineStore} from 'pinia'
import {productApi} from '@/api/product'
import {ElMessage} from 'element-plus'

export const useProductStore = defineStore('product', {
    state: () => ({
        // 产品列表
        productList: [],
        // 产品分页信息
        productPagination: {
            current: 1,
            size: 10,
            total: 0,
            pages: 0
        },
        // 当前选中的产品详情
        currentProduct: null,
        // 加载状态
        loading: false
    }),

    actions: {
        /**
         * 分页查询产品列表
         * @param {Object} params 查询参数
         */
        async fetchProductList(params = {}) {
            this.loading = true
            try {
                const response = await productApi.getProductList({
                    current: this.productPagination.current,
                    size: this.productPagination.size,
                    ...params
                })

                if (response.code === 200) {
                    this.productList = response.data.records
                    this.productPagination = {
                        current: response.data.current,
                        size: response.data.size,
                        total: response.data.total,
                        pages: response.data.pages
                    }
                    return response
                } else {
                    ElMessage.error(response.message || '查询产品列表失败')
                    throw new Error(response.message || '查询产品列表失败')
                }
            } catch (error) {
                console.error('查询产品列表失败:', error)
                ElMessage.error('查询产品列表失败')
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 获取产品详情
         * @param {number} id 产品ID
         */
        async fetchProductItem(id) {
            this.loading = true
            try {
                const response = await productApi.getProductItem(id)

                if (response.code === 200) {
                    this.currentProduct = response.data
                    return response
                } else {
                    ElMessage.error(response.message || '查询产品详情失败')
                    throw new Error(response.message || '查询产品详情失败')
                }
            } catch (error) {
                console.error('查询产品详情失败:', error)
                ElMessage.error('查询产品详情失败')
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 创建产品
         * @param {Object} data 产品数据
         */
        async createProduct(data) {
            try {
                const response = await productApi.createProduct(data)

                if (response.code === 200) {
                    ElMessage.success(response.message || '产品创建成功')
                    return response
                } else {
                    ElMessage.error(response.message || '创建产品失败')
                    throw new Error(response.message || '创建产品失败')
                }
            } catch (error) {
                console.error('创建产品失败:', error)
                ElMessage.error('创建产品失败')
                throw error
            }
        },

        /**
         * 更新产品
         * @param {number} id 产品ID
         * @param {Object} data 产品数据
         */
        async updateProduct(id, data) {
            try {
                const response = await productApi.updateProduct(id, data)

                if (response.code === 200) {
                    ElMessage.success(response.message || '产品更新成功')
                    return response
                } else {
                    ElMessage.error(response.message || '更新产品失败')
                    throw new Error(response.message || '更新产品失败')
                }
            } catch (error) {
                console.error('更新产品失败:', error)
                ElMessage.error('更新产品失败')
                throw error
            }
        },

        /**
         * 删除产品
         * @param {number} id 产品ID
         */
        async deleteProduct(id) {
            try {
                const response = await productApi.deleteProduct(id)

                if (response.code === 200) {
                    ElMessage.success(response.message || '产品删除成功')
                    return response
                } else {
                    ElMessage.error(response.message || '删除产品失败')
                    throw new Error(response.message || '删除产品失败')
                }
            } catch (error) {
                console.error('删除产品失败:', error)
                ElMessage.error('删除产品失败')
                throw error
            }
        },

        /**
         * 设置当前页码
         * @param {number} current 页码
         */
        setCurrentPage(current) {
            this.productPagination.current = current
        },

        /**
         * 设置每页大小
         * @param {number} size 每页大小
         */
        setPageSize(size) {
            this.productPagination.size = size
        },

    }
})

