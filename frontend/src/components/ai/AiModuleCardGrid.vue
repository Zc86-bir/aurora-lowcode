<template>
  <div class="module-grid">
    <article v-for="item in items" :key="item.to" class="card module-card">
      <div class="module-card-header">
        <h3>{{ item.title }}</h3>
        <span class="badge" :class="statusClass(item.status)">{{ item.status }}</span>
      </div>
      <p class="module-card-description">{{ item.description }}</p>
      <router-link class="text-btn" :to="item.to">{{ item.ctaLabel }}</router-link>
    </article>
  </div>
</template>

<script setup lang="ts">
import type { AiCapabilityCard, AiCapabilityStatus } from '@/api/ai-platform-contract'

defineProps<{ items: AiCapabilityCard[] }>()

function statusClass(status: AiCapabilityStatus) {
  return {
    Live: 'badge-success',
    Entry: 'badge-info',
    Planned: 'badge-warning',
  }[status]
}
</script>

<style scoped>
.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1rem;
}

.module-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.module-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.module-card-header h3 {
  margin: 0;
  font-size: var(--text-base);
  font-weight: 600;
}

.module-card-description {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>
