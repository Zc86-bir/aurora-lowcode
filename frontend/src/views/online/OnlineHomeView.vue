<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ t('online.onlineLowCode') }}</h2>
        <p class="page-description">{{ t('online.onlineLowCodeDescription') }}</p>
      </div>
    </div>

    <OnlineModuleCardGrid :items="items" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import OnlineModuleCardGrid from '@/components/online/OnlineModuleCardGrid.vue'

const { t } = useI18n()

const items = computed(() => [
  {
    title: t('online.onlineForms'),
    description: t('online.onlineFormsDescription'),
    to: '/online/forms',
  },
  {
    title: t('online.onlineReports'),
    description: t('online.onlineReportsDescription'),
    to: '/online/reports',
  },
  {
    title: t('online.onlineCodeGenerator'),
    description: t('online.onlineCodeGeneratorDescription'),
    to: '/online/codegen',
  },
  {
    title: t('online.onlineDashboards'),
    description: t('online.onlineDashboardsDescription'),
    to: '/online/dashboards',
  },
  {
    title: t('online.onlineNamingRules'),
    description: t('online.onlineNamingRulesDescription'),
    to: '/online/naming-rules',
  },
  {
    title: t('online.onlineValidationRules'),
    description: t('online.onlineValidationRulesDescription'),
    to: '/online/validation-rules',
  },
])
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
