import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
  ],
  // 代理配置
  server: {
    proxy: {
      // 代理所有以 /api 开头的 HTTP 请求
      '/api': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
        // 如果后端 API 本身不带 /api 前缀，可以开启 rewrite
        // rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 关键：代理 WebSocket 和 SockJS 连接
      '/ws-game': {
        target: 'http://localhost:8080',
        ws: true, // 开启 WebSocket 代理
        changeOrigin: true
      }
    }
  },
  define: {
    'global': 'window',
    'globalThis': 'window',
    'window.global': 'window',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      'sockjs-client': 'sockjs-client/dist/sockjs.min.js'
    },
  },
  optimizeDeps: {
    include: ['sockjs-client']
  }
})
