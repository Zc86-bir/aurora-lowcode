<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Form Permissions</h2>
        <p class="page-description">Control who can view, edit, publish, and operate mission-critical forms across business workflows.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Role Management',
    description: 'Refine admin and operator roles that drive field-level and form-level access rules.',
    to: '/system/roles',
  },
  {
    title: 'Data Rules',
    description: 'Coordinate form access with row-level restrictions for sensitive operational data.',
    to: '/system/data-rules',
  },
  {
    title: 'Menu Permissions',
    description: 'Align form access expectations with the navigation and capability model users actually experience.',
    to: '/system/menus',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
