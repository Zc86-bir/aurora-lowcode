<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ t('generate.title') }}</h2>
      <p class="page-description">
        Generate online CRUD assets and AI-planned application scaffolds from a single code generation center.
      </p>
    </div>

    <div class="tab-bar">
      <button class="tab" :class="{ active: mode === 'crud' }" @click="mode = 'crud'">CRUD Generator</button>
      <button class="tab" :class="{ active: mode === 'app' }" @click="mode = 'app'">App Generator (AI)</button>
    </div>

    <!-- CRUD -->
    <div v-if="mode === 'crud'" style="max-width:750px">
      <div class="form-group">
        <label class="form-label">Entity Name *</label>
        <input v-model="crud.entityName" class="form-input" placeholder="e.g. Product" />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">Table Name</label>
          <input v-model="crud.tableName" class="form-input" placeholder="product" />
        </div>
        <div class="form-group">
          <label class="form-label">Package</label>
          <input v-model="crud.packagePrefix" class="form-input" placeholder="com.aurora" />
        </div>
      </div>
      <div style="display:flex;align-items:center;justify-content:space-between;margin:1.5rem 0 0.5rem">
        <h3 style="margin:0;font-size:var(--text-base);font-weight:600">Fields ({{ crud.fields.length }})</h3>
        <button class="btn btn-secondary btn-sm" @click="addField">+ Add</button>
      </div>
      <div v-if="!crud.fields.length" class="empty-state" style="padding:1rem">Add at least one field.</div>
      <div v-for="(f,i) in crud.fields" :key="i" class="field-row">
        <input v-model="f.name" class="form-input" placeholder="name" style="width:150px" />
        <select v-model="f.javaType" class="form-select" style="width:100px">
          <option>String</option><option>Integer</option><option>Long</option><option>Double</option><option>Boolean</option><option>LocalDate</option>
        </select>
        <label class="field-check"><input v-model="f.required" type="checkbox" /> Req</label>
        <label class="field-check"><input v-model="f.primaryKey" type="checkbox" /> PK</label>
        <button class="close-btn" style="width:28px;height:28px" @click="removeField(i)">✕</button>
      </div>
      <div style="margin-top:1.5rem;display:flex;align-items:center;gap:1rem">
        <button class="btn btn-primary" :disabled="!crud.entityName || !crud.fields.length || generating" @click="runCrudGenerate">{{ generating ? 'Generating...' : 'Generate CRUD' }}</button>
        <span v-if="genError" style="color:var(--color-error);font-size:var(--text-sm)">{{ genError }}</span>
      </div>
      <div v-if="crudResult" class="card" style="margin-top:1.5rem">
        <h3 style="margin:0 0 1rem;font-size:var(--text-base);font-weight:600">Generated {{ crudResult.files.length }} files</h3>
        <div v-for="f in crudResult.files" :key="f.path" style="display:flex;align-items:center;gap:0.75rem;font-size:var(--text-xs);padding:0.25rem 0">
          <span class="badge badge-info">{{ f.type }}</span>
          <code style="color:var(--color-text-secondary);font-family:var(--font-mono);font-size:var(--text-xs)">{{ f.path }}</code>
          <span style="color:var(--color-text-muted);margin-left:auto;font-family:var(--font-mono)">{{ f.checksum.slice(0,8) }}</span>
        </div>
      </div>
    </div>

    <!-- App Generator -->
    <div v-if="mode === 'app'" style="max-width:750px">
      <div class="form-group">
        <label class="form-label">Describe your application *</label>
        <textarea v-model="appPrompt" class="form-textarea" rows="4" placeholder="e.g. Build a CRM with customer form, follow-up workflow, and monthly sales report" />
      </div>
      <div style="display:flex;align-items:center;gap:1rem">
        <button class="btn btn-primary" :disabled="!appPrompt.trim() || generating" @click="runAppGenerate">{{ generating ? 'Generating...' : 'Generate with AI' }}</button>
        <span v-if="genError" style="color:var(--color-error);font-size:var(--text-sm)">{{ genError }}</span>
      </div>
      <div v-if="appResult" class="card" style="margin-top:1.5rem">
        <h3 style="margin:0 0 1rem;font-size:var(--text-base)">Plan: {{ appResult.planName }}</h3>
        <div style="display:flex;flex-direction:column;gap:0.5rem">
          <div v-for="(task,id) in appResult.taskResultsByTaskId" :key="id" class="field-row" style="border:0;padding:0.75rem;background:#f8fafc;border-radius:var(--radius-sm)">
            <span style="font-weight:600;min-width:40px">{{ id }}</span>
            <span :class="task.success ? 'badge badge-success' : 'badge badge-error'">{{ task.success ? 'OK' : 'FAIL' }}</span>
            <code style="font-size:var(--text-xs);color:var(--color-text-secondary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ JSON.stringify(task.output).slice(0,80) }}</code>
          </div>
        </div>
      </div>
    </div>
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

const crud = reactive({
  entityName: '', tableName: '', packagePrefix: 'com.aurora',
  fields: [] as { name: string; label: string; javaType: string; columnName: string; required: boolean; primaryKey: boolean; maxLength: number }[],
})
const crudResult = ref<{ files: { path: string; type: string; checksum: string }[]; commitHash: string; duration: number } | null>(null)

function addField() { crud.fields.push({ name: '', label: '', javaType: 'String', columnName: '', required: false, primaryKey: false, maxLength: 0 }) }
function removeField(i: number) { crud.fields.splice(i, 1) }

async function runCrudGenerate() {
  generating.value = true; genError.value = ''; crudResult.value = null
  try {
    const res = await generateCrud({
      entityName: crud.entityName, tableName: crud.tableName || undefined, packagePrefix: crud.packagePrefix || 'com.aurora',
      fields: crud.fields.map(f => ({ name: f.name, label: f.label || f.name, javaType: f.javaType, columnName: f.columnName || f.name.toLowerCase(), required: f.required, primaryKey: f.primaryKey, maxLength: f.maxLength || 0 })),
    })
    if (res.success) crudResult.value = res.data as never
    else genError.value = res.error || 'Generation failed'
  } catch (e: unknown) { genError.value = e instanceof Error ? e.message : 'Unexpected error' }
  finally { generating.value = false }
}

const appPrompt = ref('')
const appResult = ref<{ planName: string; taskResultsByTaskId: Record<string, { success: boolean; output: Record<string,unknown>; errorMessage: string | null }> } | null>(null)

async function runAppGenerate() {
  generating.value = true; genError.value = ''; appResult.value = null
  try {
    const res = await generateApp(appPrompt.value)
    if (res.success) appResult.value = res.data as never
    else genError.value = res.error || 'Generation failed'
  } catch (e: unknown) { genError.value = e instanceof Error ? e.message : 'Unexpected error' }
  finally { generating.value = false }
}
</script>

<style scoped>
.page-description { max-width: 48rem; margin: 0.5rem 0 0; color: var(--color-text-secondary); font-size: var(--text-sm); line-height: 1.6; }
.tab-bar { display: flex; margin-bottom: 1.5rem; border-bottom: 2px solid var(--color-border); }
.tab { padding: 0.6rem 1.25rem; background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer; font-size: var(--text-sm); color: var(--color-text-secondary); margin-bottom: -2px; }
.tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.field-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
.field-check { display: flex; align-items: center; gap: 0.25rem; font-size: var(--text-xs); color: var(--color-text-secondary); white-space: nowrap; }
.field-check input { margin: 0; }
</style>
