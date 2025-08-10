import request from './axiosInstance'

export const userApi = {
    /**
     * 获取当前用户信息
     * @returns {Promise} 用户信息响应
     */
    getUserInfo() {
        return request({
            url: '/api/auth/user-info',
            method: 'get'
        })
    },

    /**
     * 更新用户信息
     * @param {Object} data 用户数据
     * @returns {Promise} 更新响应
     */
    updateUserInfo(data) {
        return request({
            url: '/api/admin/users/update/' + data.id,
            method: 'put',
            data
        })
    },

    /**
     * 修改密码
     * @param {Object} data 密码数据
     * @param {string} data.oldPassword 旧密码
     * @param {string} data.newPassword 新密码
     * @returns {Promise} 修改密码响应
     */
    changePassword(data) {
        return request({
            url: '/api/auth/change-password',
            method: 'post',
            data
        })
    },

    /**
     * 根据用户ID获取用户详情
     * @param {number} id 用户ID
     * @returns {Promise} 用户详情响应
     */
    getUserById(id) {
        return request({
            url: `/api/users/detail/${id}`,
            method: 'get'
        })
    },

    /**
     * 分页查询用户列表
     * @param {Object} params 查询参数
     * @param {number} params.page 页码
     * @param {number} params.size 每页大小
     * @param {string} params.username 用户名（可选）
     * @returns {Promise} 用户列表响应
     */
    getUserList(params) {
        return request({
            url: '/api/admin/users',
            method: 'get',
            params
        })
    }
}