import { defineConfig } from 'vite'
import * as uniModule from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

const uni =
  (uniModule as unknown as { default?: { default?: () => unknown } }).default?.default ??
  (uniModule as unknown as { default?: () => unknown }).default ??
  (uniModule as unknown as () => unknown)
const uniPlugin = (typeof uni === 'function' ? uni : (uni as unknown as { default?: () => unknown }).default)
if (typeof uniPlugin !== 'function') {
  throw new TypeError('vite-plugin-uni export is not a function')
}

export default defineConfig({
  plugins: [uniPlugin()],
  build: {
    outDir: 'dist'
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  }
})
