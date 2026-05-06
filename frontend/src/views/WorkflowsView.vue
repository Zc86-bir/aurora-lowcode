<template>
  <div class="workflows-page">
    <div class="page-header">
      <h2>{{ t('workflows.title') }}</h2>
      <button @click="refresh" class="btn-secondary">{{ t('workflows.refresh') }}</button>
    </div>

    <template v-if="isPending">
      <div v-for="i in 3" :key="i" class="skeleton-row" />
    </template>

    <div v-else-if="isError" class="error-state">{{ t('common.error') }}: {{ error?.message }}</div>

    <DataTable v-else-if="workflows.length" :columns="columns" :data="workflows" :title="t('workflows.title')" searchable>
      <template #actions="{ row }">
        <button class="text-btn" @click="viewDiagram(row)">{{ t('workflows.viewDiagram') }}</button>
      </template>
    </DataTable>

    <div v-else class="empty-state">{{ t('workflows.noItems') }}</div>

    <Teleport to="body">
      <div v-if="diagramItem" class="modal-overlay" @click.self="diagramItem = null">
        <div class="modal-content modal-wide">
          <div class="modal-header">
            <h3>{{ diagramItem.name }}</h3>
            <button @click="diagramItem = null" class="close-btn">✕</button>
          </div>
          <div class="modal-body">
            <BpmnViewer :xml="diagramItem.bpmnXml || sampleBpmnXml" />
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
import BpmnViewer from '@/components/workflow/BpmnViewer.vue'

const { t } = useI18n()

interface WorkflowItem {
  id: string
  name: string
  type: string
  version: number
  status: string
  createdAt: string
  bpmnXml?: string
}

const columns = computed(() => [
  { key: 'name', label: t('common.name'), sortable: true },
  { key: 'version', label: t('common.version'), sortable: true },
  { key: 'status', label: t('common.status'), sortable: true },
  { key: 'createdAt', label: t('common.created'), sortable: true },
])

const sampleBpmnXml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  targetNamespace="http://aurora-lowcode.io">
  <process id="Process_1" isExecutable="true">
    <startEvent id="Start_1" name="Start" />
    <sequenceFlow id="Flow_1" sourceRef="Start_1" targetRef="Task_1" />
    <task id="Task_1" name="Review" />
    <sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="End_1" />
    <endEvent id="End_1" name="End" />
  </process>
  <bpmndi:BPMNDiagram>
    <bpmndi:BPMNPlane>
      <bpmndi:BPMNShape id="_BPMNShape_Start_1" bpmnElement="Start_1"><dc:Bounds x="156" y="82" width="36" height="36" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="_BPMNShape_Task_1" bpmnElement="Task_1"><dc:Bounds x="263" y="65" width="100" height="80" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="_BPMNShape_End_1" bpmnElement="End_1"><dc:Bounds x="432" y="82" width="36" height="36" /></bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`

const { data, isPending, isError, error, refetch } = useGet<WorkflowItem[]>('workflows', '/api/v1/metadata', {
  params: { category: 'workflow', type: 'WORKFLOW' },
})
const workflows = computed(() => data.value || [])
const diagramItem = ref<WorkflowItem | null>(null)

function refresh() { refetch() }
function viewDiagram(row: WorkflowItem) { diagramItem.value = row }
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-header h2 { color: #1f2937; margin: 0; }
.btn-secondary { padding: 0.5rem 1rem; background: white; color: #374151; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; font-size: 0.875rem; }
.text-btn { background: none; border: none; color: #3b82f6; cursor: pointer; font-size: 0.75rem; }
.skeleton-row { height: 48px; background: #f3f4f6; margin-bottom: 0.5rem; border-radius: 6px; animation: pulse 1.5s infinite; }
@keyframes pulse { 50% { opacity: 0.5; } }
.empty-state { text-align: center; padding: 3rem; color: #9ca3af; }
.error-state { text-align: center; padding: 2rem; color: #dc2626; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 500; display: flex; align-items: center; justify-content: center; }
.modal-content { background: white; border-radius: 12px; max-width: 900px; width: 95%; max-height: 85vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.5rem; border-bottom: 1px solid #e5e7eb; }
.modal-header h3 { margin: 0; }
.close-btn { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: #6b7280; }
.modal-body { padding: 1.5rem; }
</style>
