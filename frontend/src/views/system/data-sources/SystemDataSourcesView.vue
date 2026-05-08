<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Data Sources</h2>
        <p class="page-description">Track the operational systems that power imports, reporting, and low-code runtime integrations.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Data Rules',
    description: 'Verify each source aligns with the row-level restrictions applied to sensitive business data.',
    to: '/system/data-rules',
  },
  {
    title: 'Tenants',
    description: 'Confirm which tenant environments should inherit or isolate specific data connections.',
    to: '/system/tenants',
  },
  {
    title: 'Dictionaries',
    description: 'Validate source mappings against the shared lookup values that downstream automations expect.',
    to: '/system/dictionaries',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
