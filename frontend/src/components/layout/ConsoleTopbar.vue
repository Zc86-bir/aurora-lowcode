<template>
  <header class="top-bar">
    <div>
      <p class="domain-label">{{ currentDomainLabel }}</p>
      <h1 class="page-title">{{ title }}</h1>
    </div>
    <span class="tenant-badge">{{ tenantLabel }}</span>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  currentDomainLabel: string
  tenantLabel: string
}>()
</script>

<style scoped>
.top-bar {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0 var(--space-xl);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 50;
}

.domain-label {
  margin: 0 0 0.125rem;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.page-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.tenant-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.625rem;
  border-radius: 999px;
  background: var(--color-surface-subtle, #e2e8f0);
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  font-weight: 600;
}
</style>
