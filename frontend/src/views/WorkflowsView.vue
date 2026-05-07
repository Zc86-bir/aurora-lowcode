<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ t('workflows.title') }}</h2>
      <button class="btn btn-secondary btn-sm" @click="refresh">{{ t('workflows.refresh') }}</button>
    </div>

    <div v-if="loading && !items.length" v-for="i in 3" :key="i" class="skeleton" />

    <div v-else-if="error" class="empty-state">{{ t('common.error') }}: {{ error }}</div>

    <DataTable v-else-if="items.length" :columns="columns" :data="items" :title="t('workflows.title')" searchable>
      <template #actions="{ row }">
        <button class="text-btn" @click="previewWorkflow(row)">{{ t('workflows.preview') }}</button>
      </template>
    </DataTable>

    <div v-else class="empty-state">No workflows yet.</div>

    <Teleport to="body">
      <div v-if="previewItem" class="modal-overlay" @click.self="previewItem = null">
        <div class="modal-content" style="max-width:950px">
          <div class="modal-header">
            <h3>{{ previewItem.name }}</h3>
            <button @click="previewItem = null" class="close-btn">✕</button>
          </div>
          <div class="modal-body">
            <div style="display:flex;gap:0.5rem;margin-bottom:1rem">
              <span class="badge badge-info">v{{ previewItem.version }}</span>
              <span :class="['badge', previewItem.status === 'ACTIVE' ? 'badge-success' : 'badge-warning']">{{ previewItem.status }}</span>
              <span class="badge">by {{ previewItem.createdBy }}</span>
            </div>
            <BpmnViewer v-if="bpmnXml" :xml="bpmnXml" />
            <div v-else class="empty-state">No BPMN definition available</div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppWorkflows, useAppBpmn } from '@/composables/useAppData'
import DataTable from '@/components/data/DataTable.vue'
import BpmnViewer from '@/components/workflow/BpmnViewer.vue'
import type { AppWorkflowItem } from '@/api/dev-data'

const { t } = useI18n()

const columns = computed(() => [
  { key: 'name', label: t('common.name'), sortable: true },
  { key: 'status', label: t('common.status'), sortable: true },
  { key: 'version', label: t('common.version'), sortable: true },
  { key: 'createdAt', label: t('common.created'), sortable: true },
])

const { items, loading, error, refresh } = useAppWorkflows()
const previewItem = ref<AppWorkflowItem | null>(null)
const activeWfName = ref('')
const { xml: bpmnXml } = useAppBpmn(activeWfName)

watch(previewItem, (val) => { if (val) activeWfName.value = val.name })

function previewWorkflow(row: Record<string, unknown>) { previewItem.value = row as unknown as AppWorkflowItem }
</script>
