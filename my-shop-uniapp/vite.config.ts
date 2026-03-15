import { defineConfig } from 'vite'
import { default as uni } from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

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
