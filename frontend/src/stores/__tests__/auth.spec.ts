import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

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

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorageMock.clear()
  })

  it('should start unauthenticated', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.token).toBeNull()
  })

  it('should set token and decode payload', () => {
    const store = useAuthStore()
    // Create a mock JWT payload
    const payload = {
      sub: 'user-123',
      username: 'admin',
      roles: 'ADMIN,USER',
      exp: Math.floor(Date.now() / 1000) + 3600, // 1 hour from now
    }
    const fakeJwt = `header.${btoa(JSON.stringify(payload))}.signature`

    store.setToken(fakeJwt)

    expect(store.isAuthenticated).toBe(true)
    expect(store.token).toBe(fakeJwt)
    expect(store.userId).toBe('user-123')
    expect(store.username).toBe('admin')
    expect(store.roles).toEqual(['ADMIN', 'USER'])
  })

  it('should reject expired JWT', () => {
    const store = useAuthStore()
    const payload = {
      sub: 'user-123',
      username: 'admin',
      roles: 'ADMIN',
      exp: Math.floor(Date.now() / 1000) - 3600, // 1 hour ago
    }
    const expiredJwt = `header.${btoa(JSON.stringify(payload))}.signature`

    store.setToken(expiredJwt)

    expect(store.isAuthenticated).toBe(false)
  })

  it('should clear auth state', () => {
    const store = useAuthStore()
    store.setToken(`header.${btoa(JSON.stringify({ sub: '1', username: 'u', exp: Date.now() / 1000 + 3600 }))}.sig`)
    store.clearAuth()

    expect(store.isAuthenticated).toBe(false)
    expect(store.token).toBeNull()
    expect(store.userId).toBeNull()
  })
})
