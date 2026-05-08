<template>
  <div class="card capability-card">
    <div v-if="statusLabel" class="status-row">
      <span class="detail-label">{{ statusLabel }}</span>
      <span class="badge" :class="statusBadgeClass">{{ statusValue }}</span>
    </div>

    <h3>{{ title }}</h3>
    <p>{{ body }}</p>
    <p v-if="supportCopy" class="support-copy">{{ supportCopy }}</p>
    <p v-if="statusCaption" class="support-copy"><strong>{{ statusCaptionLabel }}:</strong> {{ statusCaption }}</p>
    <router-link v-if="ctaLabel && ctaTo" class="btn btn-primary" :to="ctaTo">{{ ctaLabel }}</router-link>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  body: string
  supportCopy?: string
  statusLabel?: string
  statusValue?: string
  statusTone?: 'success' | 'info' | 'warning' | 'error'
  statusCaption?: string
  statusCaptionLabel?: string
  ctaLabel?: string
  ctaTo?: string
}>(), {
  statusTone: 'info',
})

const statusBadgeClass = computed(() => {
  return {
    success: 'badge-success',
    info: 'badge-info',
    warning: 'badge-warning',
    error: 'badge-error',
  }[props.statusTone]
})
</script>

<style scoped>
.capability-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.capability-card h3 {
  margin: 0;
  font-size: var(--text-base);
  font-weight: 600;
}

.capability-card p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.support-copy {
  color: var(--color-text-secondary);
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
</style>
