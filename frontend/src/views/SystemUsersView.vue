<template>
  <div class="page">
    <div class="page-header">
      <h2>User Management</h2>
      <button class="btn btn-primary" @click="openCreate">+ Add User</button>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="users.length === 0" class="empty-state">No users found.</div>
    <div v-else class="card table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Roles</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td><strong>{{ u.username }}</strong></td>
            <td>{{ u.email || '—' }}</td>
            <td>{{ u.phone || '—' }}</td>
            <td>
              <span v-for="r in u.roles" :key="r" class="badge badge-info">{{ r }}</span>
              <span v-if="!u.roles.length" class="text-muted">None</span>
            </td>
            <td>
              <span class="badge" :class="u.status === 'ACTIVE' ? 'badge-success' : 'badge-error'">{{ u.status }}</span>
            </td>
            <td class="actions">
              <button class="text-btn" @click="openEdit(u)">Edit</button>
              <button class="text-btn" @click="deleteUser(u)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? 'Edit User' : 'Add User' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Username *</label>
            <input v-model="form.username" class="form-input" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Email</label>
              <input v-model="form.email" class="form-input" type="email" />
            </div>
            <div class="form-group">
              <label class="form-label">Phone</label>
              <input v-model="form.phone" class="form-input" />
            </div>
          </div>
          <div v-if="!editingId" class="form-group">
            <label class="form-label">Password</label>
            <input v-model="form.password" class="form-input" type="password" />
          </div>
          <div v-if="saveError" class="error-state" style="margin-top:0.75rem">{{ saveError }}</div>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeModal">Cancel</button>
            <button class="btn btn-primary" @click="saveUser" :disabled="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { SysUser } from '@/api/admin-contract'
import * as api from '@/api/admin-contract'

const users = ref<SysUser[]>([])
const loading = ref(true)
const error = ref('')
const showModal = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const saveError = ref('')

const form = ref({ username: '', email: '', phone: '', password: '' })

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    users.value = await api.listUsers()
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  saveError.value = ''
  form.value = { username: '', email: '', phone: '', password: '' }
  showModal.value = true
}

function openEdit(u: SysUser) {
  editingId.value = u.id
  saveError.value = ''
  form.value = { username: u.username, email: u.email, phone: u.phone, password: '' }
  showModal.value = true
}

function closeModal() { showModal.value = false }

async function saveUser() {
  if (!form.value.username) return
  saving.value = true
  saveError.value = ''
  try {
    if (editingId.value) {
      await api.updateUser(editingId.value, {
        username: form.value.username,
        email: form.value.email,
        phone: form.value.phone,
      })
    } else {
      await api.createUser(form.value)
    }
    closeModal()
    await loadData()
  } catch (e: any) {
    saveError.value = e.message
  } finally {
    saving.value = false
  }
}

async function deleteUser(u: SysUser) {
  if (!confirm(`Delete user ${u.username}?`)) return
  try {
    await api.deleteUser(u.id)
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
.actions { display: flex; gap: var(--space-md); }
.text-muted { color: var(--color-text-muted); font-size: var(--text-xs); }
.form-actions { display: flex; gap: var(--space-sm); justify-content: flex-end; margin-top: var(--space-lg); }
</style>
