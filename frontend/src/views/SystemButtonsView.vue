<template>
  <div class="page">
    <div class="page-header">
      <h2>Button Permissions</h2>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <SystemEntityTable
      v-else
      title="Button Permissions"
      :columns="['Button Code', 'Button Name', 'Permission Key', 'Status', 'Actions']"
      :rows="buttons"
    >
      <template #actions>
        <button class="btn btn-primary" @click="openCreate">+ Add Button</button>
      </template>

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
    </SystemEntityTable>

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
import SystemEntityTable from '@/components/system/SystemEntityTable.vue'

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
.mono { font-family: var(--font-mono); font-size: var(--text-xs); }
.actions { display: flex; gap: var(--space-md); }
.text-btn.danger { color: var(--color-error); }
.form-actions { display: flex; gap: var(--space-sm); justify-content: flex-end; margin-top: var(--space-lg); }
</style>
