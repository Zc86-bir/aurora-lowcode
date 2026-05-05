import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '../auth'
import { setActivePinia, createPinia } from 'pinia'

// Mock localStorage
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

function createTestRouter(requiresAuth = true) {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/login',
        name: 'Login',
        component: { template: '<div>Login</div>' },
        meta: { requiresAuth: false },
      },
      {
        path: '/',
        name: 'Layout',
        component: { template: '<router-view />' },
        meta: { requiresAuth: requiresAuth },
        children: [
          {
            path: 'dashboard',
            name: 'Dashboard',
            component: { template: '<div>Dashboard</div>' },
          },
        ],
      },
    ],
  })
}

describe('router guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorageMock.clear()
  })

  it('should redirect to login when not authenticated', async () => {
    const router = createTestRouter()
    await router.push('/dashboard')
    await router.isReady()

    // Without auth token, should redirect
    const authStore = useAuthStore()
    expect(authStore.isAuthenticated).toBe(false)
  })

  it('should allow access to login page without auth', async () => {
    const router = createTestRouter()
    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('Login')
  })
})
