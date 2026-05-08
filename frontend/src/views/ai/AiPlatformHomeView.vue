<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ t('ai.platform') }}</h2>
        <p class="page-description">{{ t('ai.platformDescription') }}</p>
      </div>
    </div>

    <AiModuleCardGrid :items="cards" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AiModuleCardGrid from '@/components/ai/AiModuleCardGrid.vue'
import { getAiPlatformCards } from '@/api/ai-platform-contract'

const { t } = useI18n()
const cards = computed(() => getAiPlatformCards(t))
</script>

<style scoped>
.page-description { margin: 0.5rem 0 0; color: var(--color-text-secondary); line-height: 1.6; max-width: 52rem; }
</style>
