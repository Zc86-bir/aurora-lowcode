<template>
  <div class="page">
    <div class="page-header">
      <h2>Role Management</h2>
      <button class="btn btn-primary" @click="openCreate">+ Add Role</button>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="roles.length === 0" class="empty-state">No roles found.</div>
    <div v-else class="role-grid">
      <div v-for="r in roles" :key="r.id" class="card role-card">
        <div class="role-header">
          <div>
            <h3>{{ r.roleName }}</h3>
            <span class="role-code">{{ r.roleCode }}</span>
          </div>
          <span class="badge" :class="r.status === 'ACTIVE' ? 'badge-success' : 'badge-error'">{{ r.status }}</span>
        </div>
        <p class="role-desc">{{ r.description || 'No description' }}</p>
        <div class="role-footer">
          <button class="text-btn" @click="openEdit(r)">Edit</button>
          <button class="text-btn" @click="openMenuAssign(r)">Assign Menus</button>
          <button class="text-btn danger" @click="deleteRole(r)">Delete</button>
        </div>
      </div>
    </div>

    <!-- Role Form Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? 'Edit Role' : 'Add Role' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Role Code *</label>
            <input v-model="form.roleCode" class="form-input" placeholder="e.g. ADMIN" />
          </div>
          <div class="form-group">
            <label class="form-label">Role Name *</label>
            <input v-model="form.roleName" class="form-input" placeholder="e.g. Administrator" />
          </div>
          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea v-model="form.description" class="form-textarea" rows="3" />
          </div>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeModal">Cancel</button>
            <button class="btn btn-primary" @click="saveRole" :disabled="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Menu Assignment Modal -->
    <div v-if="showMenuModal" class="modal-overlay" @click.self="showMenuModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Assign Menus to {{ editingRoleName }}</h3>
          <button class="close-btn" @click="showMenuModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="menuLoading" class="loading-state">Loading menus...</div>
          <div v-else class="menu-list">
            <label v-for="m in allMenus" :key="m.id" class="menu-item">
              <input type="checkbox" :value="m.id" v-model="selectedMenuIds" />
              <span>{{ m.name }}</span>
              <span class="menu-path">{{ m.path || '—' }}</span>
            </label>
          </div>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="showMenuModal = false">Cancel</button>
            <button class="btn btn-primary" @click="saveMenuAssignment" :disabled="savingMenu">{{ savingMenu ? 'Saving...' : 'Save' }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { SysRole, SysMenu } from '@/api/admin-contract'
import * as adminApi from '@/api/admin-contract'

const roles = ref<SysRole[]>([])
const allMenus = ref<SysMenu[]>([])
const loading = ref(true)
const error = ref('')
const showModal = ref(false)
const showMenuModal = ref(false)
const menuLoading = ref(false)
const editingId = ref<string | null>(null)
const editingRoleName = ref('')
const saving = ref(false)
const savingMenu = ref(false)
const selectedMenuIds = ref<string[]>([])

const form = ref({ roleCode: '', roleName: '', description: '' })

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    roles.value = await adminApi.listRoles()
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { roleCode: '', roleName: '', description: '' }
  showModal.value = true
}

function openEdit(r: SysRole) {
  editingId.value = r.id
  form.value = { roleCode: r.roleCode, roleName: r.roleName, description: r.description }
  showModal.value = true
}

function closeModal() { showModal.value = false }

async function saveRole() {
  if (!form.value.roleCode || !form.value.roleName) return
  saving.value = true
  try {
    if (editingId.value) {
      await adminApi.updateRole(editingId.value, form.value)
    } else {
      await adminApi.createRole(form.value)
    }
    closeModal()
    await loadData()
  } catch (e: any) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function deleteRole(r: SysRole) {
  if (!confirm(`Delete role ${r.roleName}?`)) return
  try {
    await adminApi.deleteRole(r.id)
    await loadData()
  } catch (e: any) {
    error.value = e.message
  }
}

async function openMenuAssign(r: SysRole) {
  editingId.value = r.id
  editingRoleName.value = r.roleName
  showMenuModal.value = true
  menuLoading.value = true
  try {
    const [menus, assigned] = await Promise.all([
      adminApi.listMenus(),
      adminApi.getRoleMenus(r.id),
    ])
    allMenus.value = menus
    selectedMenuIds.value = assigned.menuIds
  } catch (e: any) {
    error.value = e.message
  } finally {
    menuLoading.value = false
  }
}

async function saveMenuAssignment() {
  if (!editingId.value) return
  savingMenu.value = true
  try {
    await adminApi.assignRoleMenus(editingId.value, selectedMenuIds.value)
    showMenuModal.value = false
  } catch (e: any) {
    error.value = e.message
  } finally {
    savingMenu.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.role-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: var(--space-md); }
.role-card { display: flex; flex-direction: column; }
.role-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: var(--space-sm); }
.role-header h3 { margin: 0; font-size: var(--text-base); }
.role-code { font-size: var(--text-xs); color: var(--color-text-muted); font-family: var(--font-mono); }
.role-desc { font-size: var(--text-sm); color: var(--color-text-secondary); flex: 1; margin: var(--space-sm) 0; }
.role-footer { display: flex; gap: var(--space-md); padding-top: var(--space-sm); border-top: 1px solid var(--color-border); }
.text-btn.danger { color: var(--color-error); }
.menu-list { display: flex; flex-direction: column; gap: var(--space-xs); max-height: 300px; overflow-y: auto; }
.menu-item { display: flex; align-items: center; gap: var(--space-sm); padding: var(--space-xs) var(--space-sm); border-radius: var(--radius-sm); cursor: pointer; font-size: var(--text-sm); }
.menu-item:hover { background: #f1f5f9; }
.menu-path { margin-left: auto; font-size: var(--text-xs); color: var(--color-text-muted); font-family: var(--font-mono); }
.form-actions { display: flex; gap: var(--space-sm); justify-content: flex-end; margin-top: var(--space-lg); }
</style>
