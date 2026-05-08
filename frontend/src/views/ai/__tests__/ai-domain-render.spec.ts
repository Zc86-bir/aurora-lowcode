// @vitest-environment happy-dom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import App from '@/App.vue'
import i18n from '@/i18n'
import router from '@/router'

const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} },
  }
})()

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })

vi.mock('@/components/copilot/AICopilotPanel.vue', () => ({
  default: { template: '<div data-testid="copilot-stub" />' },
}))

describe('ai domain render', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    localStorageMock.clear()
    await router.push('/login')

    const payload = {
      sub: '00000000-0000-0000-0000-000000000001',
      username: 'admin@aurora.dev',
      roles: 'ADMIN',
      exp: Math.floor(Date.now() / 1000) + 3600,
    }
    localStorageMock.setItem('auth_token', `header.${btoa(JSON.stringify(payload))}.signature`)
    localStorageMock.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')
  })

  it('renders the ai platform home inside the console shell', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router, i18n],
      },
    })

    await router.push('/ai')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('AI Platform')
    expect(wrapper.text()).toContain('Model Management')
    expect(wrapper.text()).toContain('AI Assistant')
    expect(wrapper.text()).toContain('Knowledge Q&A')
  })

  it('renders a status-first ai page', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router, i18n],
      },
    })

    await router.push('/ai/knowledge')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Knowledge Q&A')
    expect(wrapper.text()).toContain('Planned for a later phase')
    expect(wrapper.text()).toContain('Next milestone')
    expect(wrapper.text()).toContain('Define document-source, retrieval, and answer-evaluation workflows.')
  })

  it('renders an entry-first ai page with supporting capability copy', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router, i18n],
      },
    })

    await router.push('/ai/assistant')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('AI Assistant')
    expect(wrapper.text()).toContain('AI Copilot')
    expect(wrapper.text()).toContain('clear product surface for guided assistance')
  })
})
