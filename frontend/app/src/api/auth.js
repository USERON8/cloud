import request from './axiosInstance'

export const authApi = {
    /**
     * 用户登录
     * @param {Object} data 登录数据
     * @param {string} data.username 用户名
     * @param {string} data.password 密码
     * @returns {Promise} 登录响应
     */
    login(data) {
        return request({
            url: '/auth/login',
            method: 'post',
            data
        })
    },

    /**
     * 用户注册
     * @param {Object} data 用户数据
     * @param {string} data.username 用户名
     * @param {string} data.password 密码
     * @param {string} data.nickname 昵称
     * @param {string} data.email 邮箱
     * @param {string} data.phone 手机号
     * @returns {Promise} 注册响应
     */
    register(data) {
        return request({
            url: '/api/auth/register',
            method: 'post',
            data
        })
    },

    /**
     * 用户注册并登录
     * @param {Object} data 用户数据
     * @param {string} data.username 用户名
     * @param {string} data.password 密码
     * @param {string} data.nickname 昵称
     * @param {string} data.email 邮箱
     * @param {string} data.phone 手机号
     * @returns {Promise} 注册并登录响应
     */
    registerAndLogin(data) {
        return request({
            url: '/api/auth/register-and-login',
            method: 'post',
            data
        })
    },

    /**
     * 用户登出
     * @returns {Promise} 登出响应
     */
    logout() {
        return request({
            url: '/api/auth/logout',
            method: 'get'
        })
    },
    refreshToken(refreshToken) {
        return request({
            url: '/auth/refresh-token',
            method: 'post',
        })

    }
}