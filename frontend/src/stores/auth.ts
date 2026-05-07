import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {

  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const userId = ref<string | null>(null)
  const username = ref<string | null>(null)
  const roles = ref<string[]>([])

  const isAuthenticated = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('auth_token', newToken)
    try {
      const payload = JSON.parse(atob(newToken.split('.')[1]))
      // Check JWT expiry
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        clearAuth()
        return
      }
      userId.value = payload.sub || null
      username.value = payload.username || null
      roles.value = payload.roles ? payload.roles.split(',') : []
    } catch {
      // Invalid JWT format
    }
  }

  function clearAuth() {
    token.value = null
    userId.value = null
    username.value = null
    roles.value = []
    localStorage.removeItem('auth_token')
    localStorage.removeItem('tenant_id')
  }

  async function login(usernameInput: string, password: string): Promise<boolean> {
    try {
      const response = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: usernameInput, password }),
      })

      if (!response.ok) {
        return false
      }

      const data = await response.json()
      if (data.requiresPasswordChange) {
        return false
      }

      setToken(data.token)
      localStorage.setItem('tenant_id', data.tenantId)

      const redirect = router.currentRoute.value.query.redirect as string
      await router.push(redirect || '/dashboard')
      return true
    } catch {
      return false
    }
  }

  async function logout() {
    try {
      await fetch('/auth/logout', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token.value}`,
        },
      })
    } finally {
      clearAuth()
      await router.push('/login')
    }
  }

  return {
    token,
    userId,
    username,
    roles,
    isAuthenticated,
    setToken,
    clearAuth,
    login,
    logout,
  }
})
