import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'  // 引入 path 模块

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': path.resolve('./src') }  // 别名配置
  },
  server: {
    port: 3000,              // 自定义端口
    proxy: {                 // API 代理
      '/api': {
        target: 'http://localhost:80',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})