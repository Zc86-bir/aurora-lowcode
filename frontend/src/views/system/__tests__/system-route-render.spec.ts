// @vitest-environment happy-dom

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from '@/App.vue'
import i18n from '@/i18n'
import router from '@/router'

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

const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
  const url = String(input)

  if (url.includes('/api/admin/users')) {
    return {
      ok: true,
      json: async () => ({ success: true, data: [] }),
    } as Response
  }

  throw new Error(`Unhandled fetch in test: ${url}`)
})

Object.defineProperty(globalThis, 'fetch', { value: fetchMock })

vi.mock('@/components/copilot/AICopilotPanel.vue', () => ({
  default: { template: '<div data-testid="copilot-stub" />' },
}))

describe('system route rendering', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    localStorageMock.clear()
    fetchMock.mockClear()
    await router.push('/login')
  })

  it('renders SystemUsersView after navigating from workbench with an ADMIN token', async () => {
    const payload = {
      sub: '00000000-0000-0000-0000-000000000001',
      username: 'admin@aurora.dev',
      roles: 'ADMIN',
      exp: Math.floor(Date.now() / 1000) + 3600,
    }

    localStorageMock.setItem('auth_token', `header.${btoa(JSON.stringify(payload))}.signature`)
    localStorageMock.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')

    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router, i18n],
      },
    })

    await router.push('/workbench')
    await wrapper.vm.$nextTick()
    expect(router.currentRoute.value.fullPath).toBe('/workbench')
    expect(router.currentRoute.value.name).toBe('WorkbenchHome')
    expect(wrapper.text()).toContain('Workbench')

    await router.push('/system/users')
    await wrapper.vm.$nextTick()

    expect(router.currentRoute.value.fullPath).toBe('/system/users')
    expect(router.currentRoute.value.name).toBe('SystemUsers')
    expect(wrapper.text()).toContain('User Management')
  })
})
