<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>My Departments</h2>
        <p class="page-description">Track the departments you manage so ownership, staffing, and downstream approvals stay visible.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Department Directory',
    description: 'Open the full organization map when a local update needs broader structural context.',
    to: '/system/departments',
  },
  {
    title: 'Positions',
    description: 'Confirm the position structure attached to your departments before updating staffing and approvers.',
    to: '/system/positions',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
