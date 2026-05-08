<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ t('online.onlineValidationRules') }}</h2>
        <p class="page-description">{{ t('online.onlineValidationRulesDescription') }}</p>
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
    title: t('online.onlineValidationRulesCardFormsTitle'),
    description: t('online.onlineValidationRulesCardFormsDescription'),
    to: '/online/forms',
  },
  {
    title: t('online.onlineValidationRulesCardNamingTitle'),
    description: t('online.onlineValidationRulesCardNamingDescription'),
    to: '/online/naming-rules',
  },
  {
    title: t('online.onlineValidationRulesCardReportsTitle'),
    description: t('online.onlineValidationRulesCardReportsDescription'),
    to: '/online/reports',
  },
  {
    title: t('online.onlineValidationRulesCardDashboardsTitle'),
    description: t('online.onlineValidationRulesCardDashboardsDescription'),
    to: '/online/dashboards',
  },
]
</script>

<style scoped>
.page-description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>
