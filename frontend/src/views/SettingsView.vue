<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ t('settings.title') }}</h2>
    </div>

    <div class="settings-layout">
      <nav class="tabs">
        <button v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
      </nav>

      <div class="tab-content">
        <div v-if="activeTab === 'profile'" class="card">
          <div class="form-group">
            <label class="form-label">{{ t('settings.language') }}</label>
            <select v-model="locale" class="form-select" @change="switchLocale">
              <option value="en">English</option>
              <option value="zh-CN">简体中文</option>
            </select>
          </div>
        </div>

        <div v-if="activeTab === 'ai-models'" class="card">
          <p style="color:var(--color-text-secondary);font-size:var(--text-sm);margin:0 0 1rem">Keys are stored locally. Backend BYOK endpoint planned.</p>
          <div class="form-group">
            <label class="form-label">Anthropic API Key</label>
            <input v-model="anthropicKey" type="password" class="form-input" placeholder="sk-ant-..." />
          </div>
          <div class="form-group">
            <label class="form-label">OpenAI API Key</label>
            <input v-model="openaiKey" type="password" class="form-input" placeholder="sk-proj-..." />
          </div>
          <div style="display:flex;align-items:center;gap:1rem">
            <button class="btn btn-primary" :disabled="saving" @click="saveKeys">{{ saving ? 'Saving...' : t('settings.save') }}</button>
            <span v-if="saveMessage" :class="saveOk ? 'text-success' : 'text-error'" style="font-size:var(--text-sm)">{{ saveMessage }}</span>
          </div>
        </div>

        <div v-if="activeTab === 'theme'" class="card">
          <div class="theme-grid">
            <button v-for="th in themes" :key="th.key" class="theme-card" :class="{ active: currentTheme === th.key }" @click="setTheme(th.key)">
              <div class="theme-preview" :style="{ background: th.bg, color: th.fg }">Aa</div>
              <span style="font-size:var(--text-sm);font-weight:500">{{ th.label }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/i18n'
import { ThemeCompiler } from '@/utils/ThemeCompiler'

const { t } = useI18n()

const activeTab = ref('profile')
const locale = ref(localStorage.getItem('aurora_locale') || 'en')
const anthropicKey = ref(localStorage.getItem('aurora_ai_anthropic_key') || '')
const openaiKey = ref(localStorage.getItem('aurora_ai_openai_key') || '')
const saving = ref(false)
const saveMessage = ref('')
const saveOk = ref(false)
const currentTheme = ref(localStorage.getItem('aurora_theme') || 'light')

let themeCompiler: ThemeCompiler | null = null

const tabs = computed(() => [
  { key: 'profile', label: t('settings.profile') },
  { key: 'ai-models', label: t('settings.aiModels') },
  { key: 'theme', label: t('settings.theme') },
])

const themes = computed(() => [
  { key: 'light', label: t('settings.themeLight'), bg: '#ffffff', fg: '#0f172a' },
  { key: 'dark', label: t('settings.themeDark'), bg: '#1e293b', fg: '#f1f5f9' },
  { key: 'high-contrast', label: t('settings.themeHighContrast'), bg: '#000000', fg: '#ffffff' },
  { key: 'colorblind', label: t('settings.themeColorblind'), bg: '#f0f4f8', fg: '#1a1a2e' },
])

function switchLocale() { setLocale(locale.value); localStorage.setItem('aurora_locale', locale.value) }

function setTheme(theme: string) {
  currentTheme.value = theme
  localStorage.setItem('aurora_theme', theme)
  if (themeCompiler) { try { themeCompiler.compile(theme as never) } catch {} }
  document.documentElement.setAttribute('data-theme', theme)
}

async function saveKeys() {
  saving.value = true; saveMessage.value = ''
  try { localStorage.setItem('aurora_ai_anthropic_key', anthropicKey.value); localStorage.setItem('aurora_ai_openai_key', openaiKey.value); await new Promise(r => setTimeout(r, 200)); saveMessage.value = 'Saved'; saveOk.value = true }
  catch { saveMessage.value = 'Save failed'; saveOk.value = false }
  finally { saving.value = false }
}

onMounted(() => {
  const saved = localStorage.getItem('aurora_theme')
  if (saved) { currentTheme.value = saved; document.documentElement.setAttribute('data-theme', saved) }
  try { themeCompiler = new ThemeCompiler() } catch { themeCompiler = null }
})
</script>

<style scoped>
.settings-layout { display: flex; gap: var(--space-xl); }
.tabs { display: flex; flex-direction: column; min-width: 160px; }
.tab { padding: 0.5rem 1rem; background: none; border: none; border-radius: var(--radius-sm); cursor: pointer; text-align: left; font-size: var(--text-sm); color: var(--color-text-secondary); }
.tab:hover { background: #f1f5f9; }
.tab.active { background: var(--color-primary-light); color: var(--color-primary); font-weight: 600; }
.tab-content { flex: 1; }
.text-success { color: var(--color-success); }
.text-error { color: var(--color-error); }
.theme-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: var(--space-md); }
.theme-card { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; padding: 1rem; border: 2px solid var(--color-border); border-radius: var(--radius-md); cursor: pointer; background: none; }
.theme-card.active { border-color: var(--color-primary); }
.theme-preview { width: 48px; height: 48px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 1.125rem; }
</style>
