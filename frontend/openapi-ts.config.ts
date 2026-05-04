// frontend/src/api/openapi-ts.config.ts
// @hey-api/openapi-ts configuration

import { defineConfig } from '@hey-api/openapi-ts'

export default defineConfig({
  input: '../src/main/resources/api-docs.yaml',
  output: 'src/api/generated',
  client: 'fetch',
  types: {
    enums: 'typescript',
  },
  services: {
    asClass: true,
  },
  plugins: [
    '@hey-api/sdk',
    '@hey-api/typescript',
  ],
})
