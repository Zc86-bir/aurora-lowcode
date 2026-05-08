// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from 'vitest'
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

describe('ai routes', () => {
  beforeEach(() => {
    localStorageMock.clear()
  })

  it('registers the ai platform routes', () => {
    expect(router.resolve('/ai').name).toBe('AiPlatformHome')
    expect(router.resolve('/ai/models').name).toBe('AiModelConfig')
    expect(router.resolve('/ai/assistant').name).toBe('AiAssistant')
    expect(router.resolve('/ai/generation').name).toBe('AiGeneration')
    expect(router.resolve('/ai/mobile').name).toBe('AiMobileChat')
  })
})