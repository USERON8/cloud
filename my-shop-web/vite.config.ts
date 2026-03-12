import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import compression from 'vite-plugin-compression'
import { visualizer } from 'rollup-plugin-visualizer'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:80'
  const analyze = env.VITE_ANALYZE === 'true'

  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver({ importStyle: 'css' })],
        dts: false
      }),
      Components({
        resolvers: [ElementPlusResolver({ importStyle: 'css' })],
        dts: false
      }),
      compression({
        algorithm: 'brotliCompress',
        ext: '.br',
        threshold: 10240,
        deleteOriginFile: false
      }),
      ...(analyze
        ? [
            visualizer({
              filename: 'dist/stats.html',
              open: false,
              gzipSize: true,
              brotliSize: true
            })
          ]
        : [])
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        },
        '/auth': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    },
    build: {
      target: 'es2018',
      cssTarget: 'safari14',
      chunkSizeWarningLimit: 700,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined
            }
            if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/')) {
              return 'framework'
            }
            if (id.includes('node_modules/element-plus/')) {
              return 'element-plus'
            }
            if (id.includes('node_modules/echarts/') || id.includes('node_modules/vue-echarts/')) {
              return 'charts'
            }
            if (id.includes('node_modules/@tiptap/')) {
              return 'editor'
            }
            if (id.includes('node_modules/@vueuse/')) {
              return 'vueuse'
            }
            if (id.includes('node_modules/axios/')) {
              return 'axios'
            }
            return 'vendor'
          }
        }
      }
    }
  }
})
