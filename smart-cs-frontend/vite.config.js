import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      // ===== 统一代理所有 /api 请求 ====
      '/api': {
        target: 'http://121.199.172.24:8080',
        changeOrigin: true
      },
      // ===== API 子路径代理（精确匹配，避免拦截 SPA 路由）=====
      // Chat 服务
      '/chat/sessions': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/chat/session/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/chat/chat': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/chat/chat/stream': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/chat/sessions/list': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/chat/sessions/delete': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // Knowledge 服务
      '/knowledge/documents': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/knowledge/upload': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/knowledge/del/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/knowledge/stats': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // Admin 服务
      '/admin/stats/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/admin/config/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // Auth 服务
      '/auth/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/api/auth/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // User 服务
      '/users/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/health': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    },
    // ===== SPA Fallback 中间件（proxy 之后兜底处理）=====
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = req.url.split('?')[0]
        // 静态资源（带扩展名）直接放行
        if (url.match(/\.\w{2,6}$/)) return next()
        // 已匹配到代理的路径直接放行（静态文件或 API 路径）
        if (server.config.server.proxy) {
          for (const prefix of Object.keys(server.config.server.proxy)) {
            if (url.startsWith(prefix)) return next()
          }
        }
        // 其他 GET 请求（非静态资源、非 API）→ 返回 index.html（SPA 兜底）
        if (req.method === 'GET') {
          req.url = '/index.html'
        }
        next()
      })
    }
  }
})
