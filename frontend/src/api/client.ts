// frontend/src/api/client.ts
// API client configuration — all requests use generated SDK, no manual Axios

interface Config {
  baseUrl: string
  headers: Record<string, string>
}

// Base configuration for all API requests
export const apiConfig: Config = {
  baseUrl: import.meta.env.VITE_API_BASE || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
}

// Helper to inject auth token
export function setAuthToken(token: string): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem('auth_token', token)
  }
}

// Helper to get current auth token
export function getAuthToken(): string | null {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('auth_token')
  }
  return null
}

// Helper to inject tenant ID
export function setTenantId(tenantId: string): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem('tenant_id', tenantId)
  }
}

// Helper to get current tenant ID
export function getTenantId(): string | null {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('tenant_id')
  }
  return null
}
