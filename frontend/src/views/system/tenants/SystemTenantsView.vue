<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Tenant Management</h2>
        <p class="page-description">Operate tenant lifecycle, contact readiness, and platform isolation for each customer environment.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Contacts',
    description: 'Check tenant-facing operational contacts before upgrades, incidents, or support escalations.',
    to: '/system/contacts',
  },
  {
    title: 'Data Sources',
    description: 'Review which integrations are shared, isolated, or pending activation for each tenant.',
    to: '/system/data-sources',
  },
  {
    title: 'Notices',
    description: 'Coordinate tenant-facing announcements when platform lifecycle events require proactive communication.',
    to: '/system/notices',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
