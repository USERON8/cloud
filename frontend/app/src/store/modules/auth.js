import {defineStore} from 'pinia'
import {authApi} from '@/api/auth'
import {useUserStore} from './user'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        // 认证令牌
        token: localStorage.getItem('token') || '',
        // 刷新令牌
        refreshToken: localStorage.getItem('refreshToken') || '',
        // 用户类型
        userType: localStorage.getItem('userType') || '',
        // 用户昵称
        nickname: localStorage.getItem('nickname') || '',
        // 登录状态
        isAuthenticated: !!localStorage.getItem('token'),
        // 登录加载状态
        loading: false
    }),

    actions: {
        /**
         * 用户登录
         * @param {Object} loginData 登录数据
         * @param {string} loginData.username 用户名
         * @param {string} loginData.password 密码
         */
        async login(loginData) {
            this.loading = true
            try {
                const response = await authApi.login(loginData)

                if (response.code === 200) {
                    const {token, userType, nickname} = response.data

                    // 保存认证信息到状态和本地存储
                    this.token = token
                    this.userType = userType
                    this.nickname = nickname
                    this.isAuthenticated = true

                    localStorage.setItem('token', token)
                    localStorage.setItem('userType', userType)
                    localStorage.setItem('nickname', nickname)

                    // 获取用户详细信息
                    const userStore = useUserStore()
                    await userStore.fetchUserInfo()
                    
                    ElMessage.success(response.message || '登录成功')

                    return response
                } else {
                    ElMessage.error(response.message || '登录失败')
                    throw new Error(response.message || '登录失败')
                }
            } catch (error) {
                console.error('登录失败:', error)
                ElMessage.error('登录失败')
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 用户注册
         * @param {Object} registerData 注册数据
         */
        async register(registerData) {
            this.loading = true
            try {
                const response = await authApi.register(registerData)

                if (response.code === 200) {
                    ElMessage.success(response.message || '注册成功')
                    return response
                } else {
                    ElMessage.error(response.message || '注册失败')
                    throw new Error(response.message || '注册失败')
                }
            } catch (error) {
                console.error('注册失败:', error)
                ElMessage.error('注册失败')
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 用户注册并登录
         * @param {Object} registerData 注册数据
         */
        async registerAndLogin(registerData) {
            this.loading = true
            try {
                const response = await authApi.registerAndLogin(registerData)

                if (response.code === 200) {
                    const {token, userType, nickname} = response.data

                    // 保存认证信息到状态和本地存储
                    this.token = token
                    this.userType = userType
                    this.nickname = nickname
                    this.isAuthenticated = true

                    localStorage.setItem('token', token)
                    localStorage.setItem('userType', userType)
                    localStorage.setItem('nickname', nickname)

                    // 获取用户详细信息
                    const userStore = useUserStore()
                    await userStore.fetchUserInfo()
                    
                    ElMessage.success(response.message || '注册并登录成功')

                    return response
                } else {
                    ElMessage.error(response.message || '注册并登录失败')
                    throw new Error(response.message || '注册并登录失败')
                }
            } catch (error) {
                console.error('注册并登录失败:', error)
                ElMessage.error('注册并登录失败')
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 用户登出
         */
        async logout() {
            try {
                // 调用后端登出接口
                await authApi.logout()
                ElMessage.success('登出成功')
            } catch (error) {
                console.error('调用登出接口失败:', error)
                ElMessage.error('登出失败')
            } finally {
                // 清除本地认证信息
                this.token = ''
                this.refreshToken = ''
                this.userType = ''
                this.nickname = ''
                this.isAuthenticated = false

                localStorage.removeItem('token')
                localStorage.removeItem('refreshToken')
                localStorage.removeItem('userType')
                localStorage.removeItem('nickname')

                // 清除用户信息
                const userStore = useUserStore()
                userStore.clearUserInfo()
            }
        },
        
        /**
         * 刷新token
         * @param {string} refreshToken 刷新令牌
         */
        async refreshToken(refreshToken) {
            try {
                const response = await authApi.refreshToken(refreshToken)

                if (response.code === 200) {
                    const {token, refreshToken: newRefreshToken} = response.data

                    // 更新认证信息
                    this.token = token
                    this.refreshToken = newRefreshToken

                    localStorage.setItem('token', token)
                    localStorage.setItem('refreshToken', newRefreshToken)

                    return response
                } else {
                    // 刷新失败，执行登出操作
                    await this.logout()
                    throw new Error(response.message || 'token刷新失败')
                }
            } catch (error) {
                console.error('刷新token失败:', error)
                // 刷新失败，执行登出操作
                await this.logout()
                throw error
            }
        },

        /**
         * 清除用户信息
         */
        clearUserInfo() {
            this.userInfo = null
            this.currentUser = null
        }
    }
})