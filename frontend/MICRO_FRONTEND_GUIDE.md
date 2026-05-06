# Aurora Micro-Frontend Guide

## Overview

Aurora is a **Module Federation host**. Third-party Vue applications can register as **remotes** and mount into the Aurora console without modifying the host codebase.

This guide covers V1.1 static registration. Dynamic registration is reserved for future versions.

## Quick Start

### 1. Create a Remote Project

```
my-remote/
  src/
    App.vue          # Your remote root component
    main.ts          # Remote entry point
  vite.config.ts     # Federation remote config
  package.json
```

### 2. Remote vite.config.ts

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'my-remote',
      filename: 'remoteEntry.js',
      exposes: {
        './App': './src/App.vue',
      },
      shared: {
        vue: { singleton: true },
        'vue-router': { singleton: true },
        pinia: { singleton: true },
        'vue-i18n': { singleton: true },
        '@tanstack/vue-query': { singleton: true },
      },
    }),
  ],
  build: {
    target: 'esnext',
  },
})
```

### 3. Register in Aurora

Add your remote to `frontend/src/core/RemoteRegistry.ts`:

```ts
import { remoteRegistry, type RemoteDefinition } from '@/core/RemoteRegistry'

const myRemote: RemoteDefinition = {
  remoteId: 'my-team-dashboard',
  remoteName: 'my-remote',
  entryUrl: 'https://trusted-cdn.example.com/my-remote/remoteEntry.js',
  routeBase: 'my-dashboard',
  displayName: 'My Dashboard',
  exposes: { './App': './App' },
  requiredCapabilities: ['knowledge-enhanced-generation'],
  allowedTenants: ['tenant-code-1'],
  enabled: true,
}

remoteRegistry.push(myRemote)
```

### 4. Build

```bash
# Remote
cd my-remote && pnpm build

# Host (Aurora)
cd frontend && pnpm build
```

## Required Shared Dependencies

All remotes MUST share these dependencies as singletons with the host:

| Package | Role |
|---------|------|
| `vue` | Core framework |
| `vue-router` | Navigation |
| `pinia` | State management (when needed) |
| `vue-i18n` | Internationalization |
| `@tanstack/vue-query` | Server state |
| `useServerState` (host export) | Platform API access |
| `tokens.css` (CSS contract) | Design system |

## Security Rules

1. **No arbitrary remote origins**
   - Only remote entries registered in the static registry may load.
   - Registry entries must be reviewed before enabling in production.

2. **CSP must allow trusted origins**
   - Production `nginx.conf` must explicitly include approved remote script origins:
   ```
   add_header Content-Security-Policy "script-src 'self' https://trusted-cdn.example.com; object-src 'none'; base-uri 'self'" always;
   ```

3. **Remotes must not access internal APIs directly**
   - Vector store, knowledge retrieval, and tenant data access go through host-governed capability APIs.
   - Direct embedding or similarity search is forbidden in remotes.

4. **Remote failure isolation**
   - A failing remote must not crash the host navigation.
   - Host renders a fallback error boundary if a remote module fails to load.

## Route Contract

Each remote route must declare:

| Field | Required | Description |
|-------|----------|-------------|
| `remoteId` | Yes | Unique identifier for this remote module |
| `remoteName` | Yes | Federation remote name |
| `entryUrl` | Yes | Full URL to remote's `remoteEntry.js` |
| `routeBase` | Yes | Path segment for mounting (e.g. `my-feature`) |
| `displayName` | Yes | Human-readable name for navigation |
| `exposes` | Yes | Module mapping (e.g. `{ './App': './src/App.vue' }`) |
| `requiredCapabilities` | Yes | Platform capabilities needed by this remote |
| `allowedTenants` | No | Optional tenant restriction list |
| `compatibilityRange` | No | Optional semver range for compatibility check |
| `enabled` | Yes | Whether this remote is active |

## Local Development

```bash
# Terminal 1: Host
cd frontend && pnpm dev

# Terminal 2: Remote
cd my-remote && pnpm dev

# Host runs on :3000, remote on :3001
# Host imports remote from localhost:3001/remoteEntry.js during development
```

## CSP Notes

- Host uses `cspNonce` for inline styles in production.
- Remote scripts MUST be served with correct CORS headers from their CDN/origin.
- During local development, add `http://localhost:3001` to CSP `script-src`.
