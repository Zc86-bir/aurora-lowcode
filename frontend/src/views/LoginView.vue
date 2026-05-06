<template>
  <div class="login-page">
    <div class="login-card">
      <h1>Aurora LowCode</h1>
      <form @submit.prevent="handleLogin">
        <input
          v-model="username"
          type="text"
          :placeholder="t('auth.login.username')"
          autocomplete="username"
          required
        />
        <input
          v-model="password"
          type="password"
          :placeholder="t('auth.login.password')"
          autocomplete="current-password"
          required
        />
        <button type="submit" :disabled="loading">
          {{ loading ? '...' : t('auth.login.submit') }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''

  try {
    const success = await authStore.login(username.value, password.value)
    if (!success) {
      error.value = t('auth.login.failed')
    }
  } catch {
    error.value = t('error.network')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f5f5;
}

.login-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

h1 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #1f2937;
}

input {
  width: 100%;
  padding: 0.75rem;
  margin-bottom: 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
}

button {
  width: 100%;
  padding: 0.75rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #dc2626;
  text-align: center;
  margin-top: 0.5rem;
  font-size: 0.875rem;
}
</style>
