<template>
  <div class="dashboard">
    <h2>{{ t('dashboard.title') }}</h2>

    <div class="status-bar">
      <span class="status-indicator" :class="healthStatus === 'UP' ? 'up' : 'down'" />
      <span>{{ t('dashboard.systemStatus') }}: {{ healthStatus ?? 'OFFLINE (dev)' }}</span>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">{{ t('dashboard.cachedMetadata') }}</div>
        <div class="stat-value">{{ stats.cachedItems }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ t('dashboard.totalReloads') }}</div>
        <div class="stat-value">{{ stats.totalReloads }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ t('dashboard.reloadErrors') }}</div>
        <div class="stat-value">{{ stats.totalFailures }}</div>
      </div>
      <div class="stat-card highlight">
        <div class="stat-label">{{ t('dashboard.aiGeneration') }}</div>
        <div class="stat-value">
          <button class="action-btn" @click="openCopilot">{{ t('dashboard.newWithAI') }}</button>
        </div>
      </div>
    </div>

    <div v-if="backendError" class="error-banner">
      {{ t('common.couldNotReachBackend') }}
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
    if (meta?.data) {
      stats.value = meta.data
    } else if (isDev()) {
      stats.value = DEV_STATS
    }
  } catch {
    if (isDev()) {
      stats.value = DEV_STATS
      backendError.value = true
    }
  }
})

function openCopilot() {
  window.dispatchEvent(new CustomEvent('copilot:open'))
}
</script>

<style scoped>
.dashboard h2 {
  margin-bottom: 0.75rem;
  color: #1f2937;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: #6b7280;
  margin-bottom: 1.5rem;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
}
.status-indicator.up { background: #10b981; }
.status-indicator.down { background: #ef4444; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.stat-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stat-card.highlight {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
}

.stat-label {
  font-size: 0.75rem;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin-top: 0.25rem;
}

.action-btn {
  all: unset;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
  color: #2563eb;
  background: white;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  border: 1px solid #bfdbfe;
  margin-top: 0.25rem;
}

.action-btn:hover {
  background: #2563eb;
  color: white;
}

.error-banner {
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  color: #dc2626;
  font-size: 0.8125rem;
}
</style>
