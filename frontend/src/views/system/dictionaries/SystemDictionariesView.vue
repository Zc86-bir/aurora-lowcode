<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Dictionaries</h2>
        <p class="page-description">Govern shared lookup values so forms, reports, and integrations use the same operational language.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Dictionary Categories',
    description: 'Organize lookup catalogs by business domain before they sprawl across modules.',
    to: '/system/dictionary-categories',
  },
  {
    title: 'Form Permissions',
    description: 'Cross-check dictionary-backed forms with the access rules that protect them.',
    to: '/system/form-permissions',
  },
  {
    title: 'Data Sources',
    description: 'Audit where managed lookup values flow into integrations, imports, and operational system mappings.',
    to: '/system/data-sources',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
