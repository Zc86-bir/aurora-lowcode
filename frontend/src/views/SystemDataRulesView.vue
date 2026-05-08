<template>
  <div class="page">
    <div class="page-header">
      <h2>Data Permission Rules</h2>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <SystemEntityTable
      v-else
      title="Data Permission Rules"
      :columns="['Rule Name', 'Resource Type', 'Expression', 'Status', 'Actions']"
      :rows="rules"
    >
      <template #actions>
        <button class="btn btn-primary" @click="openCreate">+ Add Rule</button>
      </template>

      <tr v-for="r in rules" :key="r.id">
        <td><strong>{{ r.ruleName }}</strong></td>
        <td><span class="badge badge-info">{{ r.resourceType }}</span></td>
        <td class="mono expression">{{ formatExpression(r.ruleExpression) }}</td>
        <td>
          <span class="badge" :class="r.status === 'ACTIVE' ? 'badge-success' : 'badge-error'">{{ r.status }}</span>
        </td>
        <td class="actions">
          <button class="text-btn" @click="openEdit(r)">Edit</button>
          <button class="text-btn danger" @click="deleteRule(r)">Delete</button>
        </td>
      </tr>
    </SystemEntityTable>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? 'Edit Rule' : 'Add Rule' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Role ID *</label>
            <input v-model="form.roleId" class="form-input" placeholder="UUID of role" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Resource Type *</label>
              <input v-model="form.resourceType" class="form-input" placeholder="form, report, etc." />
            </div>
            <div class="form-group">
              <label class="form-label">Rule Name *</label>
              <input v-model="form.ruleName" class="form-input" placeholder="Own data only" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Rule Expression (JSON)</label>
            <textarea v-model="form.expressionJson" class="form-textarea" rows="4" placeholder='{"field": "createdBy", "op": "eq", "value": "${userId}"}' />
          </div>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeModal">Cancel</button>
            <button class="btn btn-primary" @click="saveRule" :disabled="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { SysDataRule } from '@/api/admin-contract'
import * as api from '@/api/admin-contract'
import SystemEntityTable from '@/components/system/SystemEntityTable.vue'

const rules = ref<SysDataRule[]>([])
const loading = ref(true)
const error = ref('')
const showModal = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)

const form = ref({ roleId: '', resourceType: '', ruleName: '', expressionJson: '{}' })

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    rules.value = await api.listDataRules()
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { roleId: '', resourceType: '', ruleName: '', expressionJson: '{}' }
  showModal.value = true
}

function openEdit(r: SysDataRule) {
  editingId.value = r.id
  form.value = {
    roleId: r.roleId,
    resourceType: r.resourceType,
    ruleName: r.ruleName,
    expressionJson: JSON.stringify(r.ruleExpression, null, 2),
  }
  showModal.value = true
}

function closeModal() { showModal.value = false }

async function saveRule() {
  if (!form.value.roleId || !form.value.resourceType || !form.value.ruleName) return
  let ruleExpression: Record<string, unknown>
  try {
    ruleExpression = JSON.parse(form.value.expressionJson)
  } catch {
    error.value = 'Invalid JSON in rule expression'
    return
  }
  saving.value = true
  try {
    const data = { roleId: form.value.roleId, resourceType: form.value.resourceType, ruleName: form.value.ruleName, ruleExpression }
    if (editingId.value) {
      await api.updateDataRule(editingId.value, { ruleName: form.value.ruleName, ruleExpression })
    } else {
      await api.createDataRule(data)
    }
    closeModal()
    await loadData()
  } catch (e: any) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function deleteRule(r: SysDataRule) {
  if (!confirm(`Delete rule ${r.ruleName}?`)) return
  try {
    await api.deleteDataRule(r.id)
    await loadData()
  } catch (e: any) {
    error.value = e.message
  }
}

function formatExpression(expr: Record<string, unknown>) {
  const s = JSON.stringify(expr)
  return s.length > 60 ? s.slice(0, 60) + '...' : s
}

onMounted(loadData)
</script>

<style scoped>
.mono { font-family: var(--font-mono); font-size: var(--text-xs); }
.expression { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.actions { display: flex; gap: var(--space-md); }
.text-btn.danger { color: var(--color-error); }
.form-actions { display: flex; gap: var(--space-sm); justify-content: flex-end; margin-top: var(--space-lg); }
</style>
