import { defineStore } from 'pinia'

// 全局应用状态管理
export const useAppStore = defineStore('app', {
    state: () => ({
        // 全局加载状态
        loading: false,
        // 主题模式 (light/dark)
        theme: localStorage.getItem('theme') || 'light',
        // 全局通知消息
        notifications: [],
        // 错误信息
        error: null
    }),

    actions: {
        // 切换主题
        toggleTheme() {
            this.theme = this.theme === 'light' ? 'dark' : 'light'
            localStorage.setItem('theme', this.theme)
            document.documentElement.className = this.theme
        },

        // 显示全局加载
        showLoading() {
            this.loading = true
        },

        // 隐藏全局加载
        hideLoading() {
            this.loading = false
        }
    },

    getters: {
        // 当前是否黑暗模式
        isDarkMode: (state) => state.theme === 'dark'
    }
})