// @vitest-environment happy-dom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
  }
})()

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })

describe('console routes', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorageMock.clear()
  })

  it('resolves workbench and system user routes', async () => {
    const { default: router } = await import('../index')

    expect(router.resolve('/dashboard').name).toBe('Dashboard')
    expect(router.resolve('/workbench').name).toBe('WorkbenchHome')
    expect(router.resolve('/system/users').name).toBe('SystemUsers')
    expect(router.resolve('/system/form-permissions').name).toBe('SystemFormPermissions')
    expect(router.resolve('/system/departments').name).toBe('SystemDepartments')
    expect(router.resolve('/system/my-departments').name).toBe('SystemMyDepartments')
    expect(router.resolve('/system/dictionaries').name).toBe('SystemDictionaries')
    expect(router.resolve('/system/dictionary-categories').name).toBe('SystemDictionaryCategories')
    expect(router.resolve('/system/notices').name).toBe('SystemNotices')
    expect(router.resolve('/system/positions').name).toBe('SystemPositions')
    expect(router.resolve('/system/contacts').name).toBe('SystemContacts')
    expect(router.resolve('/system/data-sources').name).toBe('SystemDataSources')
    expect(router.resolve('/system/tenants').name).toBe('SystemTenants')
  })

  it('allows system admin routes when local storage already contains an ADMIN token', async () => {
    const payload = {
      sub: '00000000-0000-0000-0000-000000000001',
      username: 'admin@aurora.dev',
      roles: 'ADMIN',
      exp: Math.floor(Date.now() / 1000) + 3600,
    }

    localStorageMock.setItem('auth_token', `header.${btoa(JSON.stringify(payload))}.signature`)
    localStorageMock.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')

    const { default: router } = await import('../index')
    await router.push('/system/users')

    expect(router.currentRoute.value.fullPath).toBe('/system/users')
    expect(router.currentRoute.value.name).toBe('SystemUsers')
  })
})
