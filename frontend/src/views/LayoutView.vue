<template>
  <div class="layout">
    <ConsoleSidebar />

    <div class="main-area">
      <ConsoleTopbar
        :title="currentTitle"
        :current-domain-label="currentDomainLabel"
        :tenant-label="currentTenantLabel"
      />

      <main class="content">
        <ErrorBoundary>
          <Suspense>
            <router-view />
            <template #fallback>
              <div class="loading-state">Loading...</div>
            </template>
          </Suspense>
        </ErrorBoundary>
      </main>
    </div>

    <AICopilotPanel />
  </div>
</template>

<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useConsoleStore } from '@/stores/console'
import { getConsoleDomainTitle } from '@/core/console/console-shell'
import AICopilotPanel from '@/components/copilot/AICopilotPanel.vue'
import ConsoleSidebar from '@/components/layout/ConsoleSidebar.vue'
import ConsoleTopbar from '@/components/layout/ConsoleTopbar.vue'
import ErrorBoundary from '@/components/shared/ErrorBoundary.vue'

const authStore = useAuthStore()
const consoleStore = useConsoleStore()
const route = useRoute()
const { t } = useI18n()

watchEffect(() => {
  consoleStore.setCurrentRoute(route.path)
})

const currentTitle = computed(() => {
  const titleKey = route.meta?.titleKey
  if (typeof titleKey === 'string' && titleKey.length > 0) {
    return t(titleKey)
  }

  return String(route.meta?.title ?? 'Aurora')
})
const currentDomainLabel = computed(() => getConsoleDomainTitle(consoleStore.currentDomain))
const currentTenantLabel = computed(() => authStore.username || 'Dev Tenant')
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }

/* ─── Main area ─── */
.main-area {
  flex: 1;
  margin-left: 220px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.content {
  flex: 1;
  padding: var(--space-xl);
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: var(--color-text-muted);
}
</style>
