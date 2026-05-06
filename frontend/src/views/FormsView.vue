<template>
  <div class="forms-page">
    <div class="page-header">
      <h2>{{ t('forms.title') }}</h2>
      <div class="page-actions">
        <button @click="refresh" class="btn-secondary">{{ t('forms.refresh') }}</button>
        <button @click="openCopilot" class="btn-primary">{{ t('forms.newForm') }}</button>
      </div>
    </div>

    <template v-if="isPending">
      <div v-for="i in 3" :key="i" class="skeleton-row" />
    </template>

    <div v-else-if="isError" class="error-state">{{ t('common.error') }}: {{ error?.message }}</div>

    <DataTable
      v-else-if="forms.length"
      :columns="columns"
      :data="forms"
      :title="t('forms.title')"
      searchable
    >
      <template #actions="{ row }">
        <button class="text-btn" @click="previewForm(row)">{{ t('forms.preview') }}</button>
      </template>
    </DataTable>

    <div v-else class="empty-state">{{ t('forms.noItems') }}</div>

    <Teleport to="body">
      <div v-if="previewItem" class="modal-overlay" @click.self="previewItem = null">
        <div class="modal-content">
          <div class="modal-header">
            <h3>{{ previewItem.name }}</h3>
            <button @click="previewItem = null" class="close-btn">✕</button>
          </div>
          <div class="modal-body">
            <DynamicForm :schema="previewItem.content" :title="previewItem.name" @submit="previewItem = null" />
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useGet } from '@/composables/useServerState'
import DataTable from '@/components/data/DataTable.vue'
import DynamicForm from '@/components/form/DynamicForm.vue'

const { t } = useI18n()

interface FormItem {
  id: string
  name: string
  type: string
  version: number
  status: string
  createdAt: string
  content?: Record<string, unknown>
}

const columns = computed(() => [
  { key: 'name', label: t('common.name'), sortable: true },
  { key: 'version', label: t('common.version'), sortable: true },
  { key: 'status', label: t('common.status'), sortable: true },
  { key: 'createdAt', label: t('common.created'), sortable: true },
])

const { data, isPending, isError, error, refetch } = useGet<FormItem[]>('forms', '/api/v1/metadata', {
  params: { category: 'form', type: 'FORM' },
})
const forms = computed(() => data.value || [])
const previewItem = ref<FormItem | null>(null)

function refresh() { refetch() }
function openCopilot() { window.dispatchEvent(new CustomEvent('copilot:open')) }
function previewForm(row: FormItem) { previewItem.value = row }
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-header h2 { color: #1f2937; margin: 0; }
.page-actions { display: flex; gap: 0.5rem; }
.btn-primary { padding: 0.5rem 1rem; background: #3b82f6; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 0.875rem; }
.btn-secondary { padding: 0.5rem 1rem; background: white; color: #374151; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; font-size: 0.875rem; }
.text-btn { background: none; border: none; color: #3b82f6; cursor: pointer; font-size: 0.75rem; }
.skeleton-row { height: 48px; background: #f3f4f6; margin-bottom: 0.5rem; border-radius: 6px; animation: pulse 1.5s infinite; }
@keyframes pulse { 50% { opacity: 0.5; } }
.empty-state { text-align: center; padding: 3rem; color: #9ca3af; }
.error-state { text-align: center; padding: 2rem; color: #dc2626; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 500; display: flex; align-items: center; justify-content: center; }
.modal-content { background: white; border-radius: 12px; max-width: 600px; width: 90%; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.5rem; border-bottom: 1px solid #e5e7eb; }
.modal-header h3 { margin: 0; }
.close-btn { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: #6b7280; }
.modal-body { padding: 1.5rem; }
</style>
