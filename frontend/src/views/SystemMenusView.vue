<template>
  <div class="page">
    <div class="page-header">
      <h2>Menu Management</h2>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <SystemEntityTable
      v-else
      title="Menus"
      :columns="['Name', 'Path', 'Type', 'Icon', 'Sort', 'Permission Key', 'Actions']"
      :rows="sortedMenus"
    >
      <template #actions>
        <button class="btn btn-primary" @click="openCreate(null)">+ Add Menu</button>
      </template>

      <tr v-for="m in sortedMenus" :key="m.id" :class="{ 'child-row': m.parentId }">
        <td>
          <span v-if="m.parentId" class="indent">└ </span>
          <strong>{{ m.name }}</strong>
        </td>
        <td class="mono">{{ m.path || '—' }}</td>
        <td><span class="badge badge-info">{{ m.type }}</span></td>
        <td>{{ m.icon || '—' }}</td>
        <td>{{ m.sortOrder }}</td>
        <td class="mono">{{ m.permissionKey || '—' }}</td>
        <td class="actions">
          <button class="text-btn" @click="openCreate(m.id)">+ Child</button>
          <button class="text-btn" @click="openEdit(m)">Edit</button>
          <button class="text-btn danger" @click="deleteMenu(m)">Delete</button>
        </td>
      </tr>
    </SystemEntityTable>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? 'Edit Menu' : 'Add Menu' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Name *</label>
            <input v-model="form.name" class="form-input" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Path</label>
              <input v-model="form.path" class="form-input" placeholder="/system/example" />
            </div>
            <div class="form-group">
              <label class="form-label">Type</label>
              <select v-model="form.type" class="form-select">
                <option value="GROUP">Group</option>
                <option value="MENU">Menu</option>
                <option value="BUTTON">Button</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Icon</label>
              <input v-model="form.icon" class="form-input" placeholder="UserOutlined" />
            </div>
            <div class="form-group">
              <label class="form-label">Sort Order</label>
              <input v-model.number="form.sortOrder" class="form-input" type="number" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Permission Key</label>
            <input v-model="form.permissionKey" class="form-input" placeholder="system:user:list" />
          </div>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeModal">Cancel</button>
            <button class="btn btn-primary" @click="saveMenu" :disabled="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { SysMenu } from '@/api/admin-contract'
import * as api from '@/api/admin-contract'
import SystemEntityTable from '@/components/system/SystemEntityTable.vue'

const menus = ref<SysMenu[]>([])
const loading = ref(true)
const error = ref('')
const showModal = ref(false)
const editingId = ref<string | null>(null)
const parentId = ref<string | null>(null)
const saving = ref(false)

const form = ref({ name: '', path: '', type: 'MENU', icon: '', sortOrder: 0, permissionKey: '' })

const sortedMenus = computed(() => {
  return [...menus.value].sort((a, b) => {
    if (!a.parentId && b.parentId) return -1
    if (a.parentId && !b.parentId) return 1
    return (a.sortOrder || 0) - (b.sortOrder || 0)
  })
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    menus.value = await api.listMenus()
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate(pid: string | null) {
  editingId.value = null
  parentId.value = pid
  form.value = { name: '', path: '', type: 'MENU', icon: '', sortOrder: 0, permissionKey: '' }
  showModal.value = true
}

function openEdit(m: SysMenu) {
  editingId.value = m.id
  parentId.value = m.parentId || null
  form.value = { name: m.name, path: m.path, type: m.type, icon: m.icon, sortOrder: m.sortOrder, permissionKey: m.permissionKey }
  showModal.value = true
}

function closeModal() { showModal.value = false }

async function saveMenu() {
  if (!form.value.name) return
  saving.value = true
  try {
    const data = { ...form.value, parentId: parentId.value || undefined }
    if (editingId.value) {
      await api.updateMenu(editingId.value, data)
    } else {
      await api.createMenu(data)
    }
    closeModal()
    await loadData()
  } catch (e: any) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function deleteMenu(m: SysMenu) {
  if (!confirm(`Delete menu ${m.name}?`)) return
  try {
    await api.deleteMenu(m.id)
    await loadData()
  } catch (e: any) {
    error.value = e.message
  }
}

onMounted(loadData)
</script>

<style scoped>
.child-row td:first-child { padding-left: 2.5rem; }
.indent { color: var(--color-text-muted); }
.mono { font-family: var(--font-mono); font-size: var(--text-xs); }
.actions { display: flex; gap: var(--space-sm); }
.text-btn.danger { color: var(--color-error); }
.form-actions { display: flex; gap: var(--space-sm); justify-content: flex-end; margin-top: var(--space-lg); }
</style>
