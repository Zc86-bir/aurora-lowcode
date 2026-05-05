<template>
  <div id="aurora-app" :data-theme="currentTheme">
    <div v-if="isLoading" class="global-loading">
      <div class="spinner" />
    </div>
    <router-view v-else />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTenantStore } from '@/stores/tenant'

const authStore = useAuthStore()
const tenantStore = useTenantStore()

const currentTheme = ref('light')
const isLoading = ref(true)

onMounted(async () => {
  try {
    const token = localStorage.getItem('auth_token')
    const tenantId = localStorage.getItem('tenant_id')

    if (token) {
      authStore.setToken(token)
    }
    if (tenantId) {
      tenantStore.setTenantId(tenantId)
    }
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
#aurora-app {
  font-family: Inter, system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.global-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
