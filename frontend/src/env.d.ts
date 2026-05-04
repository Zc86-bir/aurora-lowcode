/// <reference types="vite/client" />
/// <reference types="unplugin-vue-router/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE: string
  readonly VITE_WS_URL: string
  readonly VITE_APP_TITLE: string
  readonly VITE_CSP_NONCE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
