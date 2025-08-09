import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

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
    resolve: {
        alias: {
          '@': path.resolve(__dirname, 'src')
        }
    },
    server: {
        port: 3000,
        host: '0.0.0.0',
        proxy: {
            // 代理所有API请求到网关
            '/api': {
                target: 'http://localhost:80',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '')
            },
            // 直接代理认证服务请求到网关
            '/auth': {
                target: 'http://localhost:80',
                changeOrigin: true
            },
            // 直接代理用户服务请求到网关
            '/users': {
                target: 'http://localhost:80',
                changeOrigin: true
            },
            // 直接代理库存服务请求到网关
            '/stock': {
                target: 'http://localhost:80',
                changeOrigin: true
            }
        }
    }
})