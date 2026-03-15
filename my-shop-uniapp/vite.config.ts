import { defineConfig } from 'vite'
import * as uniPlugin from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

<<<<<<< HEAD
const uni =
  (uniPlugin as unknown as { default?: { default?: () => unknown } }).default?.default ??
  (uniPlugin as unknown as { default?: () => unknown }).default ??
  (uniPlugin as unknown as () => unknown)
=======
const uniPlugin = (typeof uni === 'function' ? uni : (uni as unknown as { default?: () => unknown }).default)
if (typeof uniPlugin !== 'function') {
  throw new TypeError('vite-plugin-uni export is not a function')
}
>>>>>>> origin/main

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
