import { defineStore } from 'pinia'
import { login as apiLogin } from '@/api/auth'

// 用户信息接口
export interface UserInfo {
  id: number
  username: string
  email: string
  phone: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

// 管理员信息接口
export interface AdminInfo {
  id: number
  username: string
  email: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

// 用户状态接口
interface UserState {
  userInfo: UserInfo | null
  adminInfo: AdminInfo | null
  token: string | null
  isAuthenticated: boolean
  isAdminAuthenticated: boolean
}

// 定义用户store
export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: null,
    adminInfo: null,
    token: null,
    isAuthenticated: false,
    isAdminAuthenticated: false
  }),

  getters: {
    // 获取用户信息
    getUserInfo: (state): UserInfo | null => state.userInfo,
    
    // 获取管理员信息
    getAdminInfo: (state): AdminInfo | null => state.adminInfo,
    
    // 获取用户token
    getToken: (state): string | null => state.token,
    
    // 检查是否为普通用户认证
    isUserAuthenticated: (state): boolean => state.isAuthenticated,
    
    // 检查是否为管理员认证
    isUserAdmin: (state): boolean => state.isAdminAuthenticated,
    
    // 获取用户角色
    getUserRoles: (state): string[] => state.userInfo?.roles || [],
    
    // 获取管理员角色
    getAdminRoles: (state): string[] => state.adminInfo?.roles || []
  },

  actions: {
    // 设置用户信息
    setUserInfo(userInfo: UserInfo) {
      this.userInfo = userInfo
      this.isAuthenticated = true
    },

    // 设置管理员信息
    setAdminInfo(adminInfo: AdminInfo) {
      this.adminInfo = adminInfo
      this.isAdminAuthenticated = true
    },

    // 设置token
    setToken(token: string) {
      this.token = token
      // 同时保存到localStorage
      localStorage.setItem('token', token)
    },

    // 设置OAuth2访问令牌
    setOAuth2Token(accessToken: string) {
      this.token = accessToken
      // 同时保存到localStorage
      localStorage.setItem('token', accessToken)
    },

    // 清除用户信息（登出）
    clearUserInfo() {
      this.userInfo = null
      this.token = null
      this.isAuthenticated = false
      // 清除localStorage中的token
      localStorage.removeItem('token')
    },

    // 清除管理员信息（登出）
    clearAdminInfo() {
      this.adminInfo = null
      this.token = null
      this.isAdminAuthenticated = false
      // 清除localStorage中的token
      localStorage.removeItem('token')
    },

    // 更新用户信息
    updateUserInfo(userInfo: Partial<UserInfo>) {
      if (this.userInfo) {
        this.userInfo = { ...this.userInfo, ...userInfo }
      }
    },

    // 更新管理员信息
    updateAdminInfo(adminInfo: Partial<AdminInfo>) {
      if (this.adminInfo) {
        this.adminInfo = { ...this.adminInfo, ...adminInfo }
      }
    },

    // 检查是否有特定权限
    hasPermission(permission: string): boolean {
      const userPermissions = this.userInfo?.permissions || []
      return userPermissions.includes(permission)
    },

    // 检查管理员是否有特定权限
    hasAdminPermission(permission: string): boolean {
      const adminPermissions = this.adminInfo?.permissions || []
      return adminPermissions.includes(permission)
    },

    // 检查是否有特定角色
    hasRole(role: string): boolean {
      const userRoles = this.userInfo?.roles || []
      return userRoles.includes(role)
    },

    // 检查管理员是否有特定角色
    hasAdminRole(role: string): boolean {
      const adminRoles = this.adminInfo?.roles || []
      return adminRoles.includes(role)
    },

    // 从localStorage恢复认证状态
    restoreAuthState() {
      const token = localStorage.getItem('token')
      if (token) {
        this.token = token
        // 这里可以根据需要从后端获取用户信息
        // 例如：this.fetchUserInfo()
      }
    },

    // 验证token有效性
    async validateToken(): Promise<boolean> {
      // 简化的token验证逻辑
      return !!this.token
    },

    // 刷新token
    async refreshToken(): Promise<string | null> {
      // 简化的刷新token逻辑
      return this.token
    },

    // 登出
    logout() {
      this.clearUserInfo()
      this.clearAdminInfo()
    }
  }
})