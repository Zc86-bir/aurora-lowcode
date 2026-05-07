<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">◆</div>
        <div class="brand-text">Aurora</div>
      </div>

      <nav class="nav-section">
        <router-link to="/dashboard" class="nav-item">
          <span class="nav-icon">☰</span> {{ t('nav.dashboard') }}
        </router-link>
        <router-link to="/forms" class="nav-item">
          <span class="nav-icon">▣</span> {{ t('nav.forms') }}
        </router-link>
        <router-link to="/reports" class="nav-item">
          <span class="nav-icon">◷</span> {{ t('nav.reports') }}
        </router-link>
        <router-link to="/workflows" class="nav-item">
          <span class="nav-icon">↻</span> {{ t('nav.workflows') }}
        </router-link>
      </nav>

      <nav class="nav-section">
        <router-link to="/generate" class="nav-item">
          <span class="nav-icon">✦</span> {{ t('nav.generate') }}
        </router-link>
        <router-link to="/settings" class="nav-item">
          <span class="nav-icon">⚙</span> {{ t('nav.settings') }}
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-avatar">{{ (authStore.username || '?')[0]?.toUpperCase() }}</div>
        <span class="user-name">{{ authStore.username || 'Dev' }}</span>
        <button @click="authStore.logout()" class="logout-link">{{ t('nav.logout') }}</button>
      </div>
    </aside>

    <div class="main-area">
      <header class="top-bar">
        <h1 class="page-title">{{ currentTitle }}</h1>
      </header>

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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import AICopilotPanel from '@/components/copilot/AICopilotPanel.vue'
import ErrorBoundary from '@/components/shared/ErrorBoundary.vue'

const { t } = useI18n()
const authStore = useAuthStore()
const route = useRoute()

const currentTitle = computed(() => {
  const name = String(route.name || '')
  const map: Record<string, string> = {
    Dashboard: t('nav.dashboard'),
    Forms: t('nav.forms'),
    Reports: t('nav.reports'),
    Workflows: t('nav.workflows'),
    Generate: t('nav.generate'),
    Settings: t('nav.settings'),
  }
  return map[name] || 'Aurora'
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }

/* ─── Sidebar ─── */
.sidebar {
  width: 220px;
  background: var(--color-sidebar-bg, #1e293b);
  color: #cbd5e1;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0; left: 0; bottom: 0;
  z-index: 100;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 1.25rem 1.25rem 1rem;
}

.brand-icon { font-size: 1.25rem; color: var(--color-primary); }
.brand-text { font-size: 1.125rem; font-weight: 700; color: white; }

.nav-section {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 0.75rem;
}

.nav-section + .nav-section {
  border-top: 1px solid rgba(255,255,255,0.08);
  margin-top: 0.25rem;
  padding-top: 0.75rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  color: #94a3b8;
  text-decoration: none;
  font-size: var(--text-sm);
  transition: all var(--transition);
}

.nav-item:hover { background: rgba(255,255,255,0.06); color: #e2e8f0; }
.nav-item.router-link-active { background: rgba(59,130,246,0.15); color: #60a5fa; font-weight: 500; }

.nav-icon { font-size: 1rem; width: 20px; text-align: center; }

.sidebar-footer {
  margin-top: auto;
  padding: 1rem 1.25rem;
  border-top: 1px solid rgba(255,255,255,0.08);
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
}

.user-name { font-size: var(--text-xs); color: #e2e8f0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.logout-link { background: none; border: none; color: #64748b; cursor: pointer; font-size: var(--text-xs); padding: 0; }
.logout-link:hover { color: #94a3b8; }

/* ─── Main area ─── */
.main-area {
  flex: 1;
  margin-left: 220px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.top-bar {
  height: 52px;
  display: flex;
  align-items: center;
  padding: 0 var(--space-xl);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
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
