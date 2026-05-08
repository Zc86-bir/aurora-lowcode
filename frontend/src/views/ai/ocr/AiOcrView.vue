<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ content.title }}</h2>
        <p class="page-description">{{ content.description }}</p>
      </div>
    </div>

    <AiCapabilityPanel :title="content.primaryCardTitle" :body="content.primaryCardBody" :support-copy="content.supportCopy" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { getAiEntryPageContent } from '@/api/ai-platform-contract'
import AiCapabilityPanel from '@/components/ai/AiCapabilityPanel.vue'

const { t } = useI18n()
const content = computed(() => getAiEntryPageContent(t, 'ocr'))
</script>

<style scoped>
.page-description {
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>
