<template>
  <div class="page">
    <div class="page-header">
      <h2>AI Model Configuration</h2>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="loadData">Refresh</button>
        <button class="btn btn-primary" @click="openCreate">+ Add Model</button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="configs.length === 0" class="empty-state">
      <p>No model configurations yet.</p>
      <button class="btn btn-primary" @click="openCreate">Add your first model</button>
    </div>
    <div v-else class="config-list">
      <div v-for="cfg in configs" :key="cfg.id" class="card config-card" :class="{ 'is-default': cfg.isDefault }">
        <div class="config-header">
          <div class="config-title">
            <span class="model-icon">{{ getProviderIcon(cfg.provider) }}</span>
            <div>
              <h3>{{ cfg.displayName }}</h3>
              <span class="model-id">{{ cfg.modelId }}</span>
            </div>
          </div>
          <div class="config-actions">
            <span class="badge" :class="cfg.status === 'ENABLED' ? 'badge-success' : 'badge-error'">
              {{ cfg.status }}
            </span>
            <span v-if="cfg.isDefault" class="badge badge-info">Default</span>
          </div>
        </div>
        <div class="config-details">
          <div class="detail-row">
            <span class="detail-label">Provider</span>
            <span>{{ cfg.provider }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Endpoint</span>
            <span class="mono">{{ cfg.requestUrl || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Created</span>
            <span>{{ formatDate(cfg.createdAt) }}</span>
          </div>
        </div>
        <div class="config-footer">
          <button class="text-btn" @click="openEdit(cfg)">Edit</button>
          <button class="text-btn" @click="toggleStatus(cfg)">
            {{ cfg.status === 'ENABLED' ? 'Disable' : 'Enable' }}
          </button>
          <button v-if="!cfg.isDefault" class="text-btn" @click="setAsDefault(cfg)">Set Default</button>
          <button class="text-btn" @click="openTest(cfg)">Test</button>
          <button class="text-btn danger" @click="deleteConfig(cfg)">Delete</button>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? 'Edit Model' : 'Add Model' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Model ID *</label>
            <input v-model="form.modelId" class="form-input" placeholder="e.g. claude-sonnet-4-20250514" />
          </div>
          <div class="form-group">
            <label class="form-label">Display Name</label>
            <input v-model="form.displayName" class="form-input" placeholder="Friendly name" />
          </div>
          <div class="form-group">
            <label class="form-label">Provider</label>
            <select v-model="form.provider" class="form-select">
              <option value="anthropic">Anthropic</option>
              <option value="openai">OpenAI</option>
              <option value="google">Google</option>
              <option value="meta">Meta</option>
              <option value="custom">Custom</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">API Key</label>
            <input v-model="form.apiKey" class="form-input" type="password" :placeholder="editingId ? 'Leave empty to keep current' : 'sk-...'" />
          </div>
          <div class="form-group">
            <label class="form-label">Request URL</label>
            <input v-model="form.requestUrl" class="form-input" placeholder="https://api.anthropic.com/v1/messages" />
          </div>
          <div class="form-group">
            <label class="form-label">
              <input v-model="form.isDefault" type="checkbox" /> Set as default model
            </label>
          </div>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeModal">Cancel</button>
            <button class="btn btn-primary" @click="saveConfig" :disabled="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Test Connection Modal -->
    <div v-if="showTestModal" class="modal-overlay" @click.self="showTestModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Test Connection</h3>
          <button class="close-btn" @click="showTestModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="testResult" :class="testResult.success ? 'test-success' : 'test-fail'">
            <span class="test-icon">{{ testResult.success ? '✓' : '✗' }}</span>
            <div>
              <strong>{{ testResult.message }}</strong>
              <div class="test-latency">Latency: {{ testResult.latencyMs }}ms</div>
            </div>
          </div>
          <div v-else class="loading-state">Testing...</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { AiModelConfig } from '@/api/ai-contract'
import * as api from '@/api/ai-contract'

const configs = ref<AiModelConfig[]>([])
const loading = ref(true)
const error = ref('')
const showModal = ref(false)
const showTestModal = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const testResult = ref<{ success: boolean; message: string; latencyMs: number } | null>(null)

const form = ref({
  modelId: '',
  displayName: '',
  provider: 'custom',
  apiKey: '',
  requestUrl: '',
  isDefault: false,
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    configs.value = await api.listModelConfigs()
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { modelId: '', displayName: '', provider: 'custom', apiKey: '', requestUrl: '', isDefault: false }
  showModal.value = true
}

function openEdit(cfg: AiModelConfig) {
  editingId.value = cfg.id
  form.value = {
    modelId: cfg.modelId,
    displayName: cfg.displayName,
    provider: cfg.provider,
    apiKey: '',
    requestUrl: cfg.requestUrl,
    isDefault: cfg.isDefault,
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveConfig() {
  if (!form.value.modelId) return
  saving.value = true
  try {
    if (editingId.value) {
      await api.updateModelConfig(editingId.value, {
        modelId: form.value.modelId,
        displayName: form.value.displayName,
        requestUrl: form.value.requestUrl,
        apiKey: form.value.apiKey || undefined,
      })
    } else {
      await api.createModelConfig(form.value)
    }
    closeModal()
    await loadData()
  } catch (e: any) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function toggleStatus(cfg: AiModelConfig) {
  try {
    await api.toggleModelConfigStatus(cfg.id)
    await loadData()
  } catch (e: any) {
    error.value = e.message
  }
}

async function setAsDefault(cfg: AiModelConfig) {
  try {
    await api.setDefaultModelConfig(cfg.id)
    await loadData()
  } catch (e: any) {
    error.value = e.message
  }
}

async function deleteConfig(cfg: AiModelConfig) {
  if (!confirm(`Delete ${cfg.displayName}?`)) return
  try {
    await api.deleteModelConfig(cfg.id)
    await loadData()
  } catch (e: any) {
    error.value = e.message
  }
}

async function openTest(cfg: AiModelConfig) {
  showTestModal.value = true
  testResult.value = null
  try {
    testResult.value = await api.testConnection({
      requestUrl: cfg.requestUrl || 'https://api.anthropic.com/v1/messages',
      apiKey: '',
    })
  } catch (e: any) {
    testResult.value = { success: false, message: e.message, latencyMs: 0 }
  }
}

function getProviderIcon(provider: string) {
  const map: Record<string, string> = { anthropic: '⬡', openai: '◉', google: '◈', meta: '◆' }
  return map[provider] || '⚙'
}

function formatDate(s: string) {
  return new Date(s).toLocaleDateString()
}

onMounted(loadData)
</script>

<style scoped>
.header-actions { display: flex; gap: var(--space-sm); }
.config-list { display: flex; flex-direction: column; gap: var(--space-md); }
.config-card { transition: box-shadow var(--transition); }
.config-card.is-default { border-color: var(--color-primary); background: var(--color-primary-light); }

.config-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--space-md); }
.config-title { display: flex; align-items: center; gap: var(--space-md); }
.model-icon { font-size: 1.5rem; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: #f1f5f9; border-radius: var(--radius-sm); }
.config-title h3 { margin: 0; font-size: var(--text-base); font-weight: 600; }
.model-id { font-size: var(--text-xs); color: var(--color-text-muted); font-family: var(--font-mono); }
.config-actions { display: flex; gap: var(--space-xs); align-items: center; }

.config-details { display: flex; flex-direction: column; gap: var(--space-xs); margin-bottom: var(--space-md); }
.detail-row { display: flex; justify-content: space-between; font-size: var(--text-sm); }
.detail-label { color: var(--color-text-secondary); }
.mono { font-family: var(--font-mono); font-size: var(--text-xs); }

.config-footer { display: flex; gap: var(--space-md); padding-top: var(--space-sm); border-top: 1px solid var(--color-border); }
.text-btn.danger { color: var(--color-error); }

.form-actions { display: flex; gap: var(--space-sm); justify-content: flex-end; margin-top: var(--space-lg); }

.test-success, .test-fail { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md); border-radius: var(--radius-sm); }
.test-success { background: var(--color-success-light); color: var(--color-success); }
.test-fail { background: var(--color-error-light); color: var(--color-error); }
.test-icon { font-size: 1.5rem; }
.test-latency { font-size: var(--text-xs); opacity: 0.8; }
</style>
