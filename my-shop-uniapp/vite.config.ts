import { defineConfig } from 'vite'
import * as uniPlugin from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

const uni =
  (uniPlugin as unknown as { default?: { default?: () => unknown } }).default?.default ??
  (uniPlugin as unknown as { default?: () => unknown }).default ??
  (uniPlugin as unknown as () => unknown)

export default defineConfig({
  plugins: [uni()],
  build: {
    outDir: 'dist'
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  }
})
