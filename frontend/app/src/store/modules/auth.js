import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { useUserStore } from './user'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('token') || '',
        refreshToken: localStorage.getItem('refreshToken') || '',
        userType: localStorage.getItem('userType') || '',
        nickname: localStorage.getItem('nickname') || '',
        isAuthenticated: !!localStorage.getItem('token'),
        loading: false
    }),

    actions: {
        async login(loginData) {
            this.loading = true
            try {
                const response = await authApi.login(loginData)

                if (response.code === 200) {
                    const { token, userType, nickname } = response.data

                    this.token = token
                    this.userType = userType
                    this.nickname = nickname
                    this.isAuthenticated = true

                    localStorage.setItem('token', token)
                    localStorage.setItem('userType', userType)
                    localStorage.setItem('nickname', nickname)

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

        async registerAndLogin(registerData) {
            this.loading = true
            try {
                const response = await authApi.registerAndLogin(registerData)

                if (response.code === 200) {
                    const { token, userType, nickname } = response.data

                    this.token = token
                    this.userType = userType
                    this.nickname = nickname
                    this.isAuthenticated = true

                    localStorage.setItem('token', token)
                    localStorage.setItem('userType', userType)
                    localStorage.setItem('nickname', nickname)

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

        async logout() {
            try {
                // 先清除用户信息
                const userStore = useUserStore()
                userStore.clearUserInfo()

                // 再清除认证信息
                this.token = ''
                this.refreshToken = ''
                this.userType = ''
                this.nickname = ''
                this.isAuthenticated = false

                localStorage.removeItem('token')
                localStorage.removeItem('refreshToken')
                localStorage.removeItem('userType')
                localStorage.removeItem('nickname')

                // 最后调用登出API
                await authApi.logout()
                ElMessage.success('登出成功')
            } catch (error) {
                console.error('登出失败:', error)
                ElMessage.error('登出失败')
            }
        },

        async refreshToken(refreshTokenValue) {
            try {
                const response = await authApi.refreshToken(refreshTokenValue)

                if (response.code === 200) {
                    const { token, refreshToken: newRefreshToken } = response.data

                    this.token = token
                    this.refreshToken = newRefreshToken

                    localStorage.setItem('token', token)
                    localStorage.setItem('refreshToken', newRefreshToken)

                    return response
                } else {
                    await this.logout()
                    throw new Error(response.message || 'token刷新失败')
                }
            } catch (error) {
                console.error('刷新token失败:', error)
                await this.logout()
                throw error
            }
        }
    }
})