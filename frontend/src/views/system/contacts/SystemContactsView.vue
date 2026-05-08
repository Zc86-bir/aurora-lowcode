<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Contacts</h2>
        <p class="page-description">Manage the people directory behind notices, escalations, and administrative handoffs across system modules.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Notices',
    description: 'Open the announcement workspace when a contact update must immediately feed communication plans.',
    to: '/system/notices',
  },
  {
    title: 'Departments',
    description: 'Map directory records back to the org units responsible for delivery and response.',
    to: '/system/departments',
  },
  {
    title: 'Tenants',
    description: 'Check tenant-level contact coverage before onboarding, incident handling, or renewal coordination.',
    to: '/system/tenants',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
