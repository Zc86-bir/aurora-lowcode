<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Notices</h2>
        <p class="page-description">Prepare internal announcements, policy updates, and release communication that must reach the right operators fast.</p>
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
    description: 'Validate recipient coverage before shipping broad communication to department stakeholders.',
    to: '/system/contacts',
  },
  {
    title: 'Tenants',
    description: 'Review tenant-facing communication dependencies before publishing broad operational notices.',
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
