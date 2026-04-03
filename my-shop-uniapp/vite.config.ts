import { defineConfig, loadEnv, type PluginOption } from 'vite'
import * as uniModule from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

const buildPlatform = process.env.UNI_PLATFORM || 'h5'

const uni =
  (uniModule as unknown as { default?: { default?: () => unknown } }).default?.default ??
  (uniModule as unknown as { default?: () => unknown }).default ??
  (uniModule as unknown as () => unknown)
const uniPlugin = (typeof uni === 'function' ? uni : (uni as unknown as { default?: () => unknown }).default)
if (typeof uniPlugin !== 'function') {
  throw new TypeError('vite-plugin-uni export is not a function')
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gatewayTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:18080'

  return {
    plugins: [uniPlugin() as PluginOption],
    build: {
      outDir: `dist/${buildPlatform}`
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_DEV_SERVER_PORT || 5173),
      proxy: {
        '/api': {
          target: gatewayTarget,
          changeOrigin: true
        },
        '/auth': {
          target: gatewayTarget,
          changeOrigin: true
        },
        '/oauth2': {
          target: gatewayTarget,
          changeOrigin: true
        }
      }
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    }
  }
})
