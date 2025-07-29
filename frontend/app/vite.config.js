import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
        sourcemap: false,
        rollupOptions: {
            output: {
                manualChunks: {
                    vendor: ['vue', 'element-plus'],
                    utils: ['axios']
                }
            }
        }
    },
    server: {
        port: 5173,
        host: '0.0.0.0',
        proxy: {
            // 代理/api开头的请求（如果需要）
            '/api': {
                target: 'http://localhost:80',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '')
            }
        }
    }
})