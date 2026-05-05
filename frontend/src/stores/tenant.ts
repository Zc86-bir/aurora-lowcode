import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useTenantStore = defineStore('tenant', () => {
  const tenantId = ref<string | null>(localStorage.getItem('tenant_id'))
  const tenantCode = ref<string | null>(null)
  const tenantName = ref<string | null>(null)

  const hasTenant = computed(() => !!tenantId.value)

  function setTenantId(id: string) {
    tenantId.value = id
    localStorage.setItem('tenant_id', id)
  }

  function setTenantInfo(code: string, name: string) {
    tenantCode.value = code
    tenantName.value = name
  }

  function clearTenant() {
    tenantId.value = null
    tenantCode.value = null
    tenantName.value = null
    localStorage.removeItem('tenant_id')
  }

  return {
    tenantId,
    tenantCode,
    tenantName,
    hasTenant,
    setTenantId,
    setTenantInfo,
    clearTenant,
  }
})
