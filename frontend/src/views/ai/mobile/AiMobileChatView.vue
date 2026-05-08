<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ content.title }}</h2>
        <p class="page-description">{{ content.description }}</p>
      </div>
    </div>

    <AiCapabilityPanel :title="content.title" :body="content.description" :status-label="t('ai.status')" :status-value="t('ai.planned')" status-tone="warning" :status-caption="content.nextMilestone" :status-caption-label="t('ai.nextMilestone')" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { getAiStatusPageContent } from '@/api/ai-platform-contract'
import AiCapabilityPanel from '@/components/ai/AiCapabilityPanel.vue'

const { t } = useI18n()
const content = computed(() => getAiStatusPageContent(t, 'mobile'))
</script>

<style scoped>
.page-description {
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>
