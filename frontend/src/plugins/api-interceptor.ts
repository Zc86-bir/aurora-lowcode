/**
 * API Interceptor — auto-injects Authorization and X-Tenant-Id headers
 * into all fetch requests made through the generated API client.
 *
 * This plugin patches the global fetch to ensure every API call
 * carries the current auth token and tenant context.
 */

const originalFetch = window.fetch

window.fetch = async function interceptedFetch(
  input: RequestInfo | URL,
  init?: RequestInit
): Promise<Response> {
  const token = localStorage.getItem('auth_token')
  const tenantId = localStorage.getItem('tenant_id')

  const headers = new Headers(init?.headers)

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  if (tenantId && !headers.has('X-Tenant-Id')) {
    headers.set('X-Tenant-Id', tenantId)
  }

  const response = await originalFetch(input, { ...init, headers })

  // Auto-logout on 401
  if (response.status === 401) {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('tenant_id')
    window.location.href = '/login'
  }

  return response
}
