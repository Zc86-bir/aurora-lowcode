<template>
  <div class="reports-page">
    <div class="page-header">
      <h2>{{ t('reports.title') }}</h2>
      <div class="page-actions">
        <button @click="refresh" class="btn-secondary">{{ t('reports.refresh') }}</button>
      </div>
    </div>

    <template v-if="loading && !items.length">
      <div v-for="i in 3" :key="i" class="skeleton-row" />
    </template>

    <div v-else-if="error" class="error-state">{{ t('common.error') }}: {{ error }}</div>

    <DataTable v-else-if="items.length" :columns="columns" :data="items" :title="t('reports.title')" searchable>
      <template #actions="{ row }">
        <button class="text-btn" @click="previewReport(row)">{{ t('reports.preview') }}</button>
      </template>
    </DataTable>

    <div v-else class="empty-state">{{ t('reports.noItems') }}</div>

    <Teleport to="body">
      <div v-if="previewItem" class="modal-overlay" @click.self="previewItem = null">
        <div class="modal-content modal-wide">
          <div class="modal-header">
            <h3>{{ previewItem.name }}</h3>
            <button @click="previewItem = null" class="close-btn">✕</button>
          </div>
          <div class="modal-body">
            <template v-if="reportLoading">Loading report data...</template>
            <div v-else-if="reportError" class="error-state">{{ reportError }}</div>
            <div v-else-if="reportData">
              <div class="report-summary">
                <span class="summary-label">Rows: {{ reportData.pagination.total }}</span>
                <span class="summary-label">Time: {{ reportData.executionTime }}ms</span>
              </div>
              <DataTable
                v-if="reportColumns.length"
                :columns="reportColumns"
                :data="reportData.data"
                :title="previewItem.name"
              />
              <div v-else class="empty-state">No columns defined</div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppReports, useAppReportData } from '@/composables/useAppData'
import DataTable from '@/components/data/DataTable.vue'
import type { AppReportItem } from '@/api/dev-data'

const { t } = useI18n()

const columns = computed(() => [
  { key: 'name', label: t('common.name'), sortable: true },
  { key: 'status', label: t('common.status'), sortable: true },
  { key: 'version', label: t('common.version'), sortable: true },
  { key: 'createdAt', label: t('common.created'), sortable: true },
])

const { items, loading, error, refresh } = useAppReports()
const previewItem = ref<AppReportItem | null>(null)
const activeReportName = ref('')

const { result: reportData, loading: reportLoading, fetch: fetchReport } = useAppReportData(activeReportName)
const reportError = ref<string | null>(null)

const reportColumns = computed(() => {
  if (!reportData.value) return []
  const first = reportData.value.data[0]
  if (!first) return []
  return Object.keys(first).map(k => ({ key: k, label: k, sortable: true }))
})

watch(previewItem, (val) => {
  if (val) {
    activeReportName.value = val.name
    fetchReport()
  }
})

function previewReport(row: Record<string, unknown>) { previewItem.value = row as unknown as AppReportItem }
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-header h2 { color: #1f2937; margin: 0; }
.page-actions { display: flex; gap: 0.5rem; }
.btn-secondary { padding: 0.5rem 1rem; background: white; color: #374151; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; font-size: 0.875rem; }
.text-btn { background: none; border: none; color: #3b82f6; cursor: pointer; font-size: 0.75rem; }
.skeleton-row { height: 48px; background: #f3f4f6; margin-bottom: 0.5rem; border-radius: 6px; animation: pulse 1.5s infinite; }
@keyframes pulse { 50% { opacity: 0.5; } }
.empty-state { text-align: center; padding: 3rem; color: #9ca3af; }
.error-state { text-align: center; padding: 2rem; color: #dc2626; }
.report-summary { display: flex; gap: 1.5rem; margin-bottom: 1rem; font-size: 0.8125rem; color: #6b7280; }
.summary-label { background: #f3f4f6; padding: 0.25rem 0.75rem; border-radius: 4px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 500; display: flex; align-items: center; justify-content: center; }
.modal-content { background: white; border-radius: 12px; max-width: 720px; width: 90%; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.5rem; border-bottom: 1px solid #e5e7eb; }
.modal-header h3 { margin: 0; }
.close-btn { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: #6b7280; }
.modal-body { padding: 1.5rem; }
</style>
