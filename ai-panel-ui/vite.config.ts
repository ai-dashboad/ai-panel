import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // Channel service API (providers configuration) — scripts/demo-providers-server.mjs or channel-web
      '/api/v1/providers': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },
      // Model groups + health metrics (workers/model)
      '/api/v1/model-groups': {
        target: 'http://localhost:8787',
        changeOrigin: true,
      },
      '/api/v1/stats': {
        target: 'http://localhost:8787',
        changeOrigin: true,
      },
      // Cloudflare Workers migration: model management (workers/model, `wrangler dev --port 8787`)
      '/api/v1/models': {
        target: 'http://localhost:8787',
        changeOrigin: true,
      },
      // Cloudflare Workers migration: API key management (workers/key, `wrangler dev --port 8788`)
      '/api/v1/keys': {
        target: 'http://localhost:8788',
        changeOrigin: true,
      },
      // Cloudflare Workers migration: billing/usage (workers/billing, `wrangler dev --port 8789`)
      '/api/v1/billing': {
        target: 'http://localhost:8789',
        changeOrigin: true,
      },
      // Cloudflare Workers migration: subscription requests (workers/subscriptions, `wrangler dev --port 8793`)
      '/api/v1/subscription-requests': {
        target: 'http://localhost:8793',
        changeOrigin: true,
      },
      // Only proxy requests that start with /api/ (with trailing slash)
      // This prevents matching routes like /api-keys
      '^/api/': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'echarts': ['echarts'],
        },
      },
    },
  },
})
