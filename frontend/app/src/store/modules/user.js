import {defineStore} from 'pinia'
import {userApi} from '@/api/user'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', {
    state: () => ({
        // 用户信息
        userInfo: null,
        // 用户列表
        userList: [],
        // 用户分页信息
        userPagination: {
            current: 1,
            size: 10,
            total: 0,
            pages: 0
        },
        // 当前选中的用户详情
        currentUser: null,
        // 加载状态
        loading: false
    }),

    actions: {
        /**
         * 获取当前用户信息
         */
        async fetchUserInfo() {
            this.loading = true
            try {
                const response = await userApi.getUserInfo()

                if (response.code === 200) {
                    this.userInfo = response.data
                    return response
                } else {
                    ElMessage.error(response.message || '获取用户信息失败')
                    throw new Error(response.message || '获取用户信息失败')
                }
            } catch (error) {
                console.error('获取用户信息失败:', error)
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 更新用户信息
         * @param {Object} data 用户数据
         */
        async updateUserInfo(data) {
            try {
                const response = await userApi.updateUserInfo(data)

                if (response.code === 200) {
                    // 如果更新的是当前用户信息，则更新本地缓存
                    if (this.userInfo && this.userInfo.id === data.id) {
                        Object.assign(this.userInfo, data)
                    }
                    ElMessage.success(response.message || '用户信息更新成功')
                    return response
                } else {
                    ElMessage.error(response.message || '更新用户信息失败')
                    throw new Error(response.message || '更新用户信息失败')
                }
            } catch (error) {
                console.error('更新用户信息失败:', error)
                throw error
            }
        },

        /**
         * 修改密码
         * @param {Object} data 密码数据
         * @param {string} data.oldPassword 旧密码
         * @param {string} data.newPassword 新密码
         */
        async changePassword(data) {
            try {
                const response = await userApi.changePassword(data)

                if (response.code === 200) {
                    ElMessage.success(response.message || '密码修改成功')
                    return response
                } else {
                    ElMessage.error(response.message || '修改密码失败')
                    throw new Error(response.message || '修改密码失败')
                }
            } catch (error) {
                console.error('修改密码失败:', error)
                throw error
            }
        },

        /**
         * 根据用户ID获取用户详情
         * @param {number} id 用户ID
         */
        async fetchUserById(id) {
            this.loading = true
            try {
                const response = await userApi.getUserById(id)

                if (response.code === 200) {
                    this.currentUser = response.data
                    return response
                } else {
                    ElMessage.error(response.message || '获取用户详情失败')
                    throw new Error(response.message || '获取用户详情失败')
                }
            } catch (error) {
                console.error('获取用户详情失败:', error)
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 分页查询用户列表
         * @param {Object} params 查询参数
         */
        async fetchUserList(params = {}) {
            this.loading = true
            try {
                const response = await userApi.getUserList({
                    current: this.userPagination.current,
                    size: this.userPagination.size,
                    ...params
                })

                if (response.code === 200) {
                    this.userList = response.data.records
                    this.userPagination = {
                        current: response.data.current,
                        size: response.data.size,
                        total: response.data.total,
                        pages: response.data.pages
                    }
                    return response
                } else {
                    ElMessage.error(response.message || '查询用户列表失败')
                    throw new Error(response.message || '查询用户列表失败')
                }
            } catch (error) {
                console.error('查询用户列表失败:', error)
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
            this.userPagination.current = current
        },

        /**
         * 设置每页大小
         * @param {number} size 每页大小
         */
        setPageSize(size) {
            this.userPagination.size = size
        },

        /**
         * 清除当前用户详情
         */
        clearCurrentUser() {
            this.currentUser = null
        },

        /**
         * 清除用户信息（登出时使用）
         */
        clearUserInfo() {
            this.userInfo = null
            this.currentUser = null
        }
    }
})