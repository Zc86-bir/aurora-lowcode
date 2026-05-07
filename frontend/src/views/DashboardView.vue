<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ t('dashboard.title') }}</h2>
      <div class="status-bar">
        <span class="status-indicator" :class="healthStatus === 'UP' ? 'up' : 'down'" />
        <span class="status-text">{{ healthStatus ?? 'OFFLINE (dev)' }}</span>
      </div>
    </div>

    <div class="stats-grid">
      <div class="card stat-card">
        <div class="stat-icon bg-blue">◎</div>
        <div>
          <div class="stat-label">{{ t('dashboard.cachedMetadata') }}</div>
          <div class="stat-value">{{ stats.cachedItems }}</div>
        </div>
      </div>
      <div class="card stat-card">
        <div class="stat-icon bg-green">↻</div>
        <div>
          <div class="stat-label">{{ t('dashboard.totalReloads') }}</div>
          <div class="stat-value">{{ stats.totalReloads }}</div>
        </div>
      </div>
      <div class="card stat-card">
        <div class="stat-icon bg-yellow">⚠</div>
        <div>
          <div class="stat-label">{{ t('dashboard.reloadErrors') }}</div>
          <div class="stat-value">{{ stats.totalFailures }}</div>
        </div>
      </div>
      <div class="card stat-card highlight">
        <div class="stat-icon">✦</div>
        <div>
          <div class="stat-label">{{ t('dashboard.aiGeneration') }}</div>
          <button class="btn btn-primary btn-sm" @click="openCopilot">{{ t('dashboard.newWithAI') }}</button>
        </div>
      </div>
    </div>

    <div v-if="backendError" class="card notice-bar">
      ⚡ {{ t('common.couldNotReachBackend') }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { isDev } from '@/api/dev-data'

const { t } = useI18n()

const healthStatus = ref<string | null>(null)
const stats = ref({ cachedItems: 0, totalReloads: 0, totalFailures: 0, diffEntries: 0 })
const backendError = ref(false)

const DEV_STATS = { cachedItems: 23, totalReloads: 8, totalFailures: 0, diffEntries: 5 }

onMounted(async () => {
  try {
    const health = await fetch('/api/v1/health').then(r => r.json())
    healthStatus.value = health?.data?.status ?? 'DOWN'
  } catch {
    backendError.value = true
  }
  try {
    const meta = await fetch('/api/v1/metadata/stats').then(r => r.json())
    stats.value = meta?.data || (isDev() ? DEV_STATS : stats.value)
  } catch {
    if (isDev()) { stats.value = DEV_STATS; backendError.value = true }
  }
})

function openCopilot() {
  window.dispatchEvent(new CustomEvent('copilot:open'))
}
</script>

<style scoped>
.status-bar { display: flex; align-items: center; gap: 0.5rem; }
.status-indicator { width: 8px; height: 8px; border-radius: 50%; background: #94a3b8; }
.status-indicator.up { background: var(--color-success); }
.status-indicator.down { background: var(--color-error); }
.status-text { font-size: var(--text-xs); color: var(--color-text-secondary); }

.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: var(--space-md); }

.stat-card { display: flex; align-items: center; gap: var(--space-md); }
.stat-card.highlight { border-color: var(--color-primary); background: var(--color-primary-light); }

.stat-icon { width: 40px; height: 40px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 1.125rem; background: #f1f5f9; flex-shrink: 0; }
.stat-icon.bg-blue { background: #dbeafe; color: var(--color-primary); }
.stat-icon.bg-green { background: var(--color-success-light); color: var(--color-success); }
.stat-icon.bg-yellow { background: var(--color-warning-light); color: var(--color-warning); }

.stat-label { font-size: var(--text-xs); color: var(--color-text-secondary); margin-bottom: 2px; }
.stat-value { font-size: var(--text-2xl); font-weight: 700; color: var(--color-text); }

.notice-bar { margin-top: var(--space-lg); padding: var(--space-md) var(--space-lg); background: var(--color-warning-light); border: 1px solid #fde68a; color: #92400e; font-size: var(--text-sm); }
</style>
