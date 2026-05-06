/**
 * API Interceptor — auto-injects Authorization, X-Tenant-Id, and Accept-Language
 * into all fetch requests made through the generated API client.
 *
 * This plugin patches the global fetch to ensure every API call
 * carries the current auth token, tenant context, and locale.
 */

const originalFetch = window.fetch

export async function interceptedFetch(
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

  // Inject Accept-Language for i18n
  if (!headers.has('Accept-Language')) {
    const locale = localStorage.getItem('aurora_locale') || navigator.language
    headers.set('Accept-Language', locale)
  }

  const response = await originalFetch(input, { ...init, headers })

  // Auto-logout on 401
  if (response.status === 401) {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('tenant_id')
    // Use dynamic import to avoid circular dependency
    const { default: router } = await import('@/router')
    router.push('/login')
  }

  return response
}

window.fetch = interceptedFetch
