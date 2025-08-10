export const productApi = {
    /**
     * 获取产品列表
     * @param {Object} params 查询参数
     * @returns {Promise} 产品列表响应
     */
    getProductList(params) {
        // 注意：当前后端没有专门的产品服务接口
        // 这里暂时返回一个模拟实现，后续需要后端提供对应接口
        return Promise.resolve({
            code: 200,
            message: '查询成功',
            data: {
                records: [],
                total: 0,
                current: 1,
                size: 10
            }
        })
    },

    /**
     * 获取产品详情
     * @param {number} id 产品ID
     * @returns {Promise} 产品详情响应
     */
    getProductItem(id) {
        // 注意：当前后端没有专门的产品服务接口
        // 这里暂时返回一个模拟实现，后续需要后端提供对应接口
        return Promise.resolve({
            code: 200,
            message: '查询成功',
            data: null
        })
    },

    /**
     * 创建产品
     * @param {Object} data 产品数据
     * @returns {Promise} 创建响应
     */
    createProduct(data) {
        // 注意：当前后端没有专门的产品服务接口
        // 这里暂时返回一个模拟实现，后续需要后端提供对应接口
        return Promise.resolve({
            code: 200,
            message: '创建成功',
            data: null
        })
    },

    /**
     * 更新产品
     * @param {number} id 产品ID
     * @param {Object} data 产品数据
     * @returns {Promise} 更新响应
     */
    updateProduct(id, data) {
        // 注意：当前后端没有专门的产品服务接口
        // 这里暂时返回一个模拟实现，后续需要后端提供对应接口
        return Promise.resolve({
            code: 200,
            message: '更新成功',
            data: null
        })
    },

    /**
     * 删除产品
     * @param {number} id 产品ID
     * @returns {Promise} 删除响应
     */
    deleteProduct(id) {
        // 注意：当前后端没有专门的产品服务接口
        // 这里暂时返回一个模拟实现，后续需要后端提供对应接口
        return Promise.resolve({
            code: 200,
            message: '删除成功',
            data: null
        })
    }
}