<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ t('reports.title') }}</h2>
        <p class="page-description">Manage online report assets, query previews, and publishing workflows.</p>
      </div>
      <button class="btn btn-secondary btn-sm" @click="refresh">{{ t('reports.refresh') }}</button>
    </div>

    <div v-if="loading && !items.length" v-for="i in 3" :key="i" class="skeleton" />

    <div v-else-if="error" class="empty-state">{{ t('common.error') }}: {{ error }}</div>

    <DataTable v-else-if="items.length" :columns="columns" :data="items" :title="t('reports.title')" searchable>
      <template #actions="{ row }">
        <button class="text-btn" @click="previewReport(row)">{{ t('reports.preview') }}</button>
      </template>
    </DataTable>

    <div v-else class="empty-state">No reports yet.</div>

    <Teleport to="body">
      <div v-if="previewItem" class="modal-overlay" @click.self="previewItem = null">
        <div class="modal-content" style="max-width:900px">
          <div class="modal-header">
            <h3>{{ previewItem.name }}</h3>
            <button @click="previewItem = null" class="close-btn">✕</button>
          </div>
          <div class="modal-body">
            <template v-if="reportLoading">Loading report data...</template>
            <div v-else-if="reportError" class="error-state">{{ reportError }}</div>
            <div v-else-if="reportData">
              <div style="display:flex;gap:1rem;margin-bottom:1rem">
                <span class="badge badge-info">Rows: {{ reportData.pagination.total }}</span>
                <span class="badge badge-info">{{ reportData.executionTime }}ms</span>
              </div>
              <DataTable v-if="reportColumns.length" :columns="reportColumns" :data="reportData.data" />
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
  if (!reportData.value?.data?.[0]) return []
  return Object.keys(reportData.value.data[0]).map(k => ({ key: k, label: k, sortable: true }))
})

watch(previewItem, (val) => {
  if (val) { activeReportName.value = val.name; fetchReport() }
})

function previewReport(row: Record<string, unknown>) { previewItem.value = row as unknown as AppReportItem }
</script>

<style scoped>
.page-description {
  margin: 0.25rem 0 0;
  color: var(--color-text-secondary, #64748b);
  font-size: 0.95rem;
}
</style>
