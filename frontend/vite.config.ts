// frontend/vite.config.ts
// Aurora Low-Code Platform — Vite 6 Configuration
// Features: chunk splitting, preload/prefetch, CSP compliance, performance budgets

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import VueRouter from 'unplugin-vue-router/vite'
import UnoCSS from 'unocss/vite'
import Components from 'unplugin-vue-components/vite'
import AutoImport from 'unplugin-auto-import/vite'
import { visualizer } from 'rollup-plugin-visualizer'
import { compression } from 'vite-plugin-compression2'
import { resolve } from 'node:path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const isProd = mode === 'production'
  const API_BASE = env.VITE_API_BASE || 'http://localhost:8080'

export default defineConfig({
  plugins: [
    VueRouter({
      routesFolder: 'src/views',
      dts: 'src/types/typed-router.d.ts',
    }),
    vue({
      template: {
        compilerOptions: {
          // Strict CSP: no inline styles
          isCustomElement: (tag) => tag.startsWith('svg:') || tag.startsWith('math:'),
        },
      },
    }),
    vueJsx(),
    UnoCSS(),
    Components({
      dirs: ['src/components'],
      dts: 'src/types/components.d.ts',
    }),
    AutoImport({
      imports: ['vue', 'vue-router', '@vueuse/core'],
      dts: 'src/types/auto-imports.d.ts',
      dirs: ['src/composables', 'src/stores'],
    }),
    isProd && compression({ algorithm: 'gzip', threshold: 10240 }),
    isProd && compression({ algorithm: 'brotliCompress', threshold: 10240 }),
    isProd && visualizer({
      filename: 'dist/stats.html',
      open: false,
      gzipSize: true,
      brotliSize: true,
    }),
  ].filter(Boolean),

  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '~': resolve(__dirname, 'node_modules'),
    },
  },

  build: {
    target: 'esnext',
    minify: 'esbuild',
    sourcemap: isProd ? 'hidden' : 'inline',
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        // Chunk splitting strategy
        manualChunks(id) {
          // Vendor chunks by package size priority
          if (id.includes('node_modules')) {
            // Vue core (small, always needed)
            if (id.includes('vue') || id.includes('@vue')) {
              return 'vue-core'
            }
            // Charting (heavy, lazy load)
            if (id.includes('echarts') || id.includes('chart')) {
              return 'charts'
            }
            // Editor (heavy, lazy load)
            if (id.includes('monaco') || id.includes('codemirror')) {
              return 'editor'
            }
            // PDF/Excel export (heavy, lazy load)
            if (id.includes('jspdf') || id.includes('xlsx') || id.includes('file-saver')) {
              return 'export'
            }
            // Yjs collaboration (medium, lazy load)
            if (id.includes('yjs') || id.includes('y-')) {
              return 'collaboration'
            }
            // Everything else
            return 'vendor'
          }
          // Route-level code splitting
          if (id.includes('/views/')) {
            const match = id.match(/\/views\/([^/]+)\//)
            if (match) return `views/${match[1]}`
          }
        },
        // Asset naming with content hash for long-term caching
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
      },
    },
    // Performance budget enforcement
    reportCompressedSize: true,
  },

  server: {
    port: 3000,
    strictPort: true,
    host: true,
    cors: true,
    proxy: {
      '/api': {
        target: API_BASE,
        changeOrigin: true,
        secure: false,
      },
    },
  },

  preview: {
    port: 4173,
    host: true,
  },

  optimizeDeps: {
    include: ['vue', 'vue-router', '@vueuse/core', 'pinia'],
    exclude: ['monaco-editor'],
  },

  css: {
    // Disable CSS modules for UnoCSS compatibility
    modules: {
      localsConvention: 'camelCase',
    },
    devSourcemap: true,
  },

  // CSP-compliant: no inline styles in production
  html: {
    cspNonce: isProd ? '__VITE_CSP_NONCE__' : undefined,
  },
})
