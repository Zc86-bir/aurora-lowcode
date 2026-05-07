<template>
  <div class="generate-page">
    <div class="page-header">
      <h2>{{ t('generate.title') }}</h2>
    </div>

    <div class="generator-tabs">
      <button class="tab-btn" :class="{ active: mode === 'crud' }" @click="mode = 'crud'">CRUD Generator</button>
      <button class="tab-btn" :class="{ active: mode === 'app' }" @click="mode = 'app'">App Generator (AI)</button>
    </div>

    <!-- ─── CRUD Generator ─── -->
    <section v-if="mode === 'crud'" class="generator-panel">
      <div class="form-group">
        <label>Entity Name *</label>
        <input v-model="crud.entityName" class="form-input" placeholder="e.g. Product" />
      </div>
      <div class="form-row">
        <div class="form-group flex-1">
          <label>Table Name</label>
          <input v-model="crud.tableName" class="form-input" placeholder="product" />
        </div>
        <div class="form-group flex-1">
          <label>Package Prefix</label>
          <input v-model="crud.packagePrefix" class="form-input" placeholder="com.aurora" />
        </div>
      </div>

      <div class="section-header">
        <h4>Fields ({{ crud.fields.length }})</h4>
        <button class="btn-small" @click="addField">+ Add Field</button>
      </div>

      <div v-if="!crud.fields.length" class="empty-hint">Add at least one field to generate CRUD code.</div>

      <div v-for="(f, i) in crud.fields" :key="i" class="field-row">
        <input v-model="f.name" class="field-input" placeholder="field name" />
        <select v-model="f.javaType" class="field-select">
          <option value="String">String</option>
          <option value="Integer">Integer</option>
          <option value="Long">Long</option>
          <option value="Double">Double</option>
          <option value="Boolean">Boolean</option>
          <option value="LocalDate">Date</option>
        </select>
        <label class="field-check"><input v-model="f.required" type="checkbox" /> Required</label>
        <label class="field-check"><input v-model="f.primaryKey" type="checkbox" /> PK</label>
        <button class="btn-remove" @click="removeField(i)">✕</button>
      </div>

      <div class="action-bar">
        <button class="btn-primary" :disabled="!crud.entityName || crud.fields.length === 0 || generating" @click="runCrudGenerate">
          {{ generating ? 'Generating...' : 'Generate CRUD' }}
        </button>
        <span v-if="genError" class="gen-error">{{ genError }}</span>
      </div>

      <div v-if="crudResult" class="result-panel">
        <h4>Generated Files ({{ crudResult.files.length }})</h4>
        <div class="file-list">
          <div v-for="file in crudResult.files" :key="file.path" class="file-item">
            <span class="file-type">{{ file.type }}</span>
            <code class="file-path">{{ file.path }}</code>
            <span class="file-checksum">{{ file.checksum.slice(0, 8) }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ─── App Generator (AI) ─── -->
    <section v-if="mode === 'app'" class="generator-panel">
      <div class="form-group">
        <label>Describe what you want to build *</label>
        <textarea v-model="appPrompt" class="form-textarea" rows="4" placeholder="e.g. Build a CRM system with customer form, follow-up workflow, and monthly sales report" />
      </div>

      <div class="action-bar">
        <button class="btn-primary" :disabled="!appPrompt.trim() || generating" @click="runAppGenerate">
          {{ generating ? 'Generating...' : 'Generate with AI' }}
        </button>
        <span v-if="genError" class="gen-error">{{ genError }}</span>
      </div>

      <div v-if="appResult" class="result-panel">
        <h4>Plan: {{ appResult.planName }}</h4>
        <div class="task-grid">
          <div v-for="(task, id) in appResult.taskResultsByTaskId" :key="id" class="task-card" :class="task.success ? 'success' : 'failed'">
            <div class="task-id">{{ id }}</div>
            <div class="task-status">{{ task.success ? '✓' : '✗' }}</div>
            <div class="task-output">{{ JSON.stringify(task.output).slice(0, 60) }}...</div>
          </div>
        </div>
        <div v-if="appResult.errorMessage" class="gen-error">Error: {{ appResult.errorMessage }}</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { generateCrud, generateApp } from '@/composables/useAppData'

const { t } = useI18n()

const mode = ref<'crud' | 'app'>('crud')
const generating = ref(false)
const genError = ref('')

// ─── CRUD state ───
const crud = reactive({
  entityName: '',
  tableName: '',
  packagePrefix: 'com.aurora',
  fields: [] as { name: string; label: string; javaType: string; columnName: string; required: boolean; primaryKey: boolean; maxLength: number }[],
})

interface CrudFile { path: string; type: string; checksum: string }
const crudResult = ref<{ files: CrudFile[]; commitHash: string; duration: number } | null>(null)

function addField() {
  crud.fields.push({ name: '', label: '', javaType: 'String', columnName: '', required: false, primaryKey: false, maxLength: 0 })
}

function removeField(i: number) { crud.fields.splice(i, 1) }

async function runCrudGenerate() {
  generating.value = true
  genError.value = ''
  crudResult.value = null
  try {
    const res = await generateCrud({
      entityName: crud.entityName,
      tableName: crud.tableName || undefined,
      packagePrefix: crud.packagePrefix || 'com.aurora',
      fields: crud.fields.map(f => ({
        name: f.name, label: f.label || f.name, javaType: f.javaType, columnName: f.columnName || f.name.toLowerCase(),
        required: f.required, primaryKey: f.primaryKey, maxLength: f.maxLength || 0,
      })),
    })
    if (res.success) {
      crudResult.value = res.data as { files: CrudFile[]; commitHash: string; duration: number }
    } else {
      genError.value = res.error || 'Generation failed'
    }
  } catch (e: unknown) {
    genError.value = e instanceof Error ? e.message : 'Unexpected error'
  } finally {
    generating.value = false
  }
}

// ─── App Generator state ───

const appPrompt = ref('')
const appResult = ref<{
  planName: string
  taskResultsByTaskId: Record<string, { success: boolean; output: Record<string, unknown>; errorMessage: string | null }>
  errorMessage?: string | null
} | null>(null)

async function runAppGenerate() {
  generating.value = true
  genError.value = ''
  appResult.value = null
  try {
    const res = await generateApp(appPrompt.value)
    if (res.success) {
      appResult.value = res.data as typeof appResult.value
    } else {
      genError.value = res.error || 'Generation failed'
    }
  } catch (e: unknown) {
    genError.value = e instanceof Error ? e.message : 'Unexpected error'
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 1.5rem; }
.page-header h2 { color: #1f2937; }
.generator-tabs { display: flex; gap: 0; margin-bottom: 1.5rem; border-bottom: 2px solid #e5e7eb; }
.tab-btn { padding: 0.75rem 1.5rem; background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer; font-size: 0.875rem; color: #6b7280; margin-bottom: -2px; }
.tab-btn.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.generator-panel { max-width: 800px; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-size: 0.8125rem; color: #374151; margin-bottom: 0.25rem; font-weight: 500; }
.form-row { display: flex; gap: 1rem; }
.flex-1 { flex: 1; }
.form-input, .form-textarea, .field-input, .field-select { width: 100%; padding: 0.5rem 0.75rem; border: 1px solid #d1d5db; border-radius: 6px; font-size: 0.8125rem; }
.form-textarea { resize: vertical; font-family: inherit; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin: 1.5rem 0 0.75rem; }
.section-header h4 { margin: 0; color: #1f2937; }
.btn-small { padding: 0.25rem 0.75rem; background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; border-radius: 4px; cursor: pointer; font-size: 0.75rem; }
.empty-hint { color: #9ca3af; font-size: 0.8125rem; padding: 1rem 0; }
.field-row { display: flex; gap: 0.5rem; align-items: center; margin-bottom: 0.5rem; }
.field-input { width: 160px; }
.field-select { width: 110px; }
.field-check { display: flex; align-items: center; gap: 0.25rem; font-size: 0.75rem; color: #6b7280; white-space: nowrap; }
.field-check input { margin: 0; }
.btn-remove { background: none; border: none; color: #ef4444; cursor: pointer; font-size: 1rem; padding: 0 0.25rem; }
.action-bar { display: flex; align-items: center; gap: 1rem; margin-top: 1.5rem; }
.btn-primary { padding: 0.75rem 2rem; background: #3b82f6; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 0.875rem; font-weight: 600; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.gen-error { color: #dc2626; font-size: 0.8125rem; }
.result-panel { margin-top: 2rem; padding: 1.5rem; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; }
.result-panel h4 { margin: 0 0 1rem; color: #1f2937; }
.file-list { display: flex; flex-direction: column; gap: 0.5rem; }
.file-item { display: flex; align-items: center; gap: 0.75rem; font-size: 0.75rem; }
.file-type { background: #dbeafe; color: #1e40af; padding: 0.125rem 0.5rem; border-radius: 4px; font-weight: 600; text-transform: uppercase; min-width: 80px; text-align: center; }
.file-path { color: #374151; font-family: monospace; }
.file-checksum { color: #9ca3af; font-family: monospace; margin-left: auto; }
.task-grid { display: grid; gap: 0.75rem; }
.task-card { display: flex; align-items: center; gap: 0.75rem; padding: 0.75rem; background: white; border: 1px solid #e5e7eb; border-radius: 6px; }
.task-card.success { border-left: 3px solid #10b981; }
.task-card.failed { border-left: 3px solid #ef4444; }
.task-id { font-weight: 600; font-size: 0.875rem; color: #1f2937; min-width: 40px; }
.task-status { font-size: 1rem; }
.task-card.success .task-status { color: #10b981; }
.task-card.failed .task-status { color: #ef4444; }
.task-output { font-size: 0.75rem; color: #6b7280; font-family: monospace; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
