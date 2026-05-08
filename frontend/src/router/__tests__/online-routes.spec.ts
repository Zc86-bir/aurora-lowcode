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

describe('online routes', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorageMock.clear()
  })

  it('resolves online landing and child routes', async () => {
    const { default: router } = await import('../index')

    expect(router.resolve('/online').name).toBe('OnlineHome')
    expect(router.resolve('/online/forms').name).toBe('OnlineForms')
    expect(router.resolve('/online/reports').name).toBe('OnlineReports')
    expect(router.resolve('/online/dashboards').name).toBe('OnlineDashboards')
    expect(router.resolve('/online/naming-rules').name).toBe('OnlineNamingRules')
    expect(router.resolve('/online/validation-rules').name).toBe('OnlineValidationRules')
    expect(router.resolve('/online/codegen').name).toBe('OnlineCodeGenerator')
  })
})
