<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Departments</h2>
        <p class="page-description">Model the organizational structure used for approvals, visibility rules, and operational ownership.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'My Departments',
    description: 'Review the departments you directly support and the workflows that depend on them.',
    to: '/system/my-departments',
  },
  {
    title: 'Positions',
    description: 'Standardize job positions that appear in organizational rosters and approval assignments.',
    to: '/system/positions',
  },
  {
    title: 'Contacts',
    description: 'Verify who owns each operating unit when structural changes affect escalations and approvals.',
    to: '/system/contacts',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
