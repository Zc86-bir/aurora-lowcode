<template>
  <div class="page">
    <div class="page-header">
      <h2>Button Permissions</h2>
      <button class="btn btn-primary" @click="openCreate">+ Add Button</button>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="buttons.length === 0" class="empty-state">No button permissions found.</div>
    <div v-else class="card table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>Button Code</th>
            <th>Button Name</th>
            <th>Permission Key</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in buttons" :key="b.id">
            <td class="mono">{{ b.buttonCode }}</td>
            <td>{{ b.buttonName }}</td>
            <td class="mono">{{ b.permissionKey || '—' }}</td>
            <td>
              <span class="badge" :class="b.status === 'ACTIVE' ? 'badge-success' : 'badge-error'">{{ b.status }}</span>
            </td>
            <td class="actions">
              <button class="text-btn" @click="openEdit(b)">Edit</button>
              <button class="text-btn danger" @click="deleteButton(b)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? 'Edit Button' : 'Add Button' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Menu ID *</label>
            <input v-model="form.menuId" class="form-input" placeholder="UUID of parent menu" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Button Code *</label>
              <input v-model="form.buttonCode" class="form-input" placeholder="btn_create" />
            </div>
            <div class="form-group">
              <label class="form-label">Button Name *</label>
              <input v-model="form.buttonName" class="form-input" placeholder="Create" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Permission Key</label>
            <input v-model="form.permissionKey" class="form-input" placeholder="system:user:create" />
          </div>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeModal">Cancel</button>
            <button class="btn btn-primary" @click="saveButton" :disabled="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { SysButton } from '@/api/admin-contract'
import * as api from '@/api/admin-contract'

const buttons = ref<SysButton[]>([])
const loading = ref(true)
const error = ref('')
const showModal = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)

const form = ref({ menuId: '', buttonCode: '', buttonName: '', permissionKey: '' })

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    buttons.value = await api.listButtons()
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { menuId: '', buttonCode: '', buttonName: '', permissionKey: '' }
  showModal.value = true
}

function openEdit(b: SysButton) {
  editingId.value = b.id
  form.value = { menuId: b.menuId, buttonCode: b.buttonCode, buttonName: b.buttonName, permissionKey: b.permissionKey }
  showModal.value = true
}

function closeModal() { showModal.value = false }

async function saveButton() {
  if (!form.value.menuId || !form.value.buttonCode || !form.value.buttonName) return
  saving.value = true
  try {
    if (editingId.value) {
      await api.updateButton(editingId.value, form.value)
    } else {
      await api.createButton(form.value)
    }
    closeModal()
    await loadData()
  } catch (e: any) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function deleteButton(b: SysButton) {
  if (!confirm(`Delete button ${b.buttonName}?`)) return
  try {
    await api.deleteButton(b.id)
    await loadData()
  } catch (e: any) {
    error.value = e.message
  }
}

onMounted(loadData)
</script>

<style scoped>
.table-card { padding: 0; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; padding: 0.75rem var(--space-lg); font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); background: #f8fafc; border-bottom: 1px solid var(--color-border); }
.data-table td { padding: 0.75rem var(--space-lg); font-size: var(--text-sm); border-bottom: 1px solid var(--color-border); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #f8fafc; }
.mono { font-family: var(--font-mono); font-size: var(--text-xs); }
.actions { display: flex; gap: var(--space-md); }
.text-btn.danger { color: var(--color-error); }
.form-actions { display: flex; gap: var(--space-sm); justify-content: flex-end; margin-top: var(--space-lg); }
</style>
