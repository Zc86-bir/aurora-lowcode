<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Dictionary Categories</h2>
        <p class="page-description">Define stable lookup categories so teams can extend reference data without duplicating business concepts.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Dictionary Entries',
    description: 'Jump into the underlying values once a category definition is ready for operations.',
    to: '/system/dictionaries',
  },
  {
    title: 'Form Permissions',
    description: 'Check which controlled forms depend on each category before making cross-module lookup changes.',
    to: '/system/form-permissions',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
