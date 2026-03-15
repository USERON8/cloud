import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

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
