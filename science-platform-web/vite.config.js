import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// 开发服务器配置：所有 /api 请求代理到 Spring Boot 后端，
// 前端代码里只写相对路径 /api/v1/...，避免在多个页面硬编码 localhost 地址。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // 把第三方库拆成独立 chunk：
    // Element Plus 体积大且很少变动，单独分包后浏览器可以长期缓存，
    // 业务代码更新时用户只需重新下载较小的业务包。
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }
          if (id.includes('element-plus') || id.includes('@element-plus')) {
            return 'element-plus'
          }
          if (id.includes('node_modules/vue') || id.includes('node_modules/pinia')) {
            return 'vue-vendor'
          }
          return 'vendor'
        },
      },
    },
    // 分包后单个 chunk 阈值放宽，避免每次构建都刷体积警告
    chunkSizeWarningLimit: 800,
  },
})
