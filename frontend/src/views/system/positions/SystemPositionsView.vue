<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>Positions</h2>
        <p class="page-description">Maintain the role catalog used by teams, rosters, and approval chains across the enterprise console.</p>
      </div>
    </div>

    <SystemModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

const items = [
  {
    title: 'Departments',
    description: 'Review where each position lives inside the formal organizational hierarchy.',
    to: '/system/departments',
  },
  {
    title: 'Roles',
    description: 'Compare organizational positions with platform roles when responsibilities need to be translated into access.',
    to: '/system/roles',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
