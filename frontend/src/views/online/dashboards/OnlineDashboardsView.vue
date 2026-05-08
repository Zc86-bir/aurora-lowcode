<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ t('online.onlineDashboards') }}</h2>
        <p class="page-description">{{ t('online.onlineDashboardsDescription') }}</p>
      </div>
    </div>

    <OnlineModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import OnlineModuleCardGrid from '@/components/online/OnlineModuleCardGrid.vue'

const { t } = useI18n()

const items = [
  {
    title: t('online.onlineDashboardsCardReportsTitle'),
    description: t('online.onlineDashboardsCardReportsDescription'),
    to: '/online/reports',
  },
  {
    title: t('online.onlineDashboardsCardFormsTitle'),
    description: t('online.onlineDashboardsCardFormsDescription'),
    to: '/online/forms',
  },
  {
    title: t('online.onlineDashboardsCardCodegenTitle'),
    description: t('online.onlineDashboardsCardCodegenDescription'),
    to: '/online/codegen',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
