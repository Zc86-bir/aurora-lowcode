<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ t('forms.title') }}</h2>
      <div style="display:flex;gap:0.5rem">
        <button class="btn btn-secondary btn-sm" @click="refresh">{{ t('forms.refresh') }}</button>
        <button class="btn btn-primary btn-sm" @click="openCopilot">{{ t('forms.newForm') }}</button>
      </div>
    </div>

    <div v-if="loading && !items.length" v-for="i in 3" :key="i" class="skeleton" />

    <div v-else-if="error" class="empty-state">{{ t('common.error') }}: {{ error }}</div>

    <DataTable v-else-if="items.length" :columns="columns" :data="items" :title="t('forms.title')" searchable>
      <template #actions="{ row }">
        <button class="text-btn" @click="previewForm(row)">{{ t('forms.preview') }}</button>
      </template>
    </DataTable>

    <div v-else class="empty-state">No forms yet. Create one with AI Copilot.</div>

    <Teleport to="body">
      <div v-if="previewItem" class="modal-overlay" @click.self="previewItem = null">
        <div class="modal-content">
          <div class="modal-header">
            <h3>{{ previewItem.name }}</h3>
            <button @click="previewItem = null" class="close-btn">✕</button>
          </div>
          <div class="modal-body">
            <DynamicForm v-if="previewItem.content" :schema="previewItem.content" :title="previewItem.name" @submit="previewItem = null" />
            <div v-else class="empty-state">No preview available</div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppForms } from '@/composables/useAppData'
import DataTable from '@/components/data/DataTable.vue'
import DynamicForm from '@/components/form/DynamicForm.vue'
import type { AppFormItem } from '@/api/dev-data'

const { t } = useI18n()

const columns = computed(() => [
  { key: 'name', label: t('common.name'), sortable: true },
  { key: 'status', label: t('common.status'), sortable: true },
  { key: 'version', label: t('common.version'), sortable: true },
  { key: 'createdAt', label: t('common.created'), sortable: true },
])

const { items, loading, error, refresh } = useAppForms()
const previewItem = ref<AppFormItem | null>(null)

function openCopilot() { window.dispatchEvent(new CustomEvent('copilot:open')) }
function previewForm(row: Record<string, unknown>) { previewItem.value = row as unknown as AppFormItem }
</script>
