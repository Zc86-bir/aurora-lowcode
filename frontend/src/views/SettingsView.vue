<template>
  <div class="settings-page">
    <h2>{{ t('settings.title') }}</h2>
    <div class="settings-layout">
      <!-- Sidebar tabs -->
      <nav class="settings-tabs">
        <button v-for="tab in tabs" :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
          {{ tab.label }}
        </button>
      </nav>

      <!-- Tab content -->
      <div class="settings-content">
        <!-- Profile -->
        <section v-if="activeTab === 'profile'">
          <h3>{{ t('settings.profile') }}</h3>
          <div class="form-group">
            <label>{{ t('settings.language') }}</label>
            <select v-model="locale" class="form-select" @change="switchLocale">
              <option value="en">English</option>
              <option value="zh-CN">简体中文</option>
            </select>
          </div>
        </section>

        <!-- AI Models (BYOK) -->
        <section v-if="activeTab === 'ai-models'">
          <h3>{{ t('settings.aiModels') }}</h3>
          <p class="note">{{ t('settings.apiKeyNote') }}</p>
          <div class="form-group">
            <label>Anthropic API Key</label>
            <input v-model="anthropicKey" type="password" class="form-input" placeholder="sk-ant-..." />
          </div>
          <div class="form-group">
            <label>OpenAI API Key</label>
            <input v-model="openaiKey" type="password" class="form-input" placeholder="sk-proj-..." />
          </div>
          <button class="btn-primary" @click="saveKeys">{{ t('settings.save') }}</button>
        </section>

        <!-- Theme & Access -->
        <section v-if="activeTab === 'theme'">
          <h3>{{ t('settings.theme') }}</h3>
          <div class="theme-grid">
            <button v-for="th in themes" :key="th.key" class="theme-card" :class="{ active: currentTheme === th.key }" @click="setTheme(th.key)">
              <div class="theme-preview" :style="{ background: th.bg, color: th.fg }">Aa</div>
              <span>{{ th.label }}</span>
            </button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/i18n'

const { t } = useI18n()

const activeTab = ref('profile')
const locale = ref(localStorage.getItem('aurora_locale') || 'en')
const anthropicKey = ref('')
const openaiKey = ref('')
const currentTheme = ref(localStorage.getItem('aurora_theme') || 'light')

const tabs = [
  { key: 'profile', label: t('settings.profile') },
  { key: 'ai-models', label: t('settings.aiModels') },
  { key: 'theme', label: t('settings.theme') },
]

const themes = [
  { key: 'light', label: t('settings.themeLight'), bg: '#ffffff', fg: '#000000' },
  { key: 'dark', label: t('settings.themeDark'), bg: '#1f2937', fg: '#ffffff' },
  { key: 'high-contrast', label: t('settings.themeHighContrast'), bg: '#000000', fg: '#ffffff' },
  { key: 'colorblind', label: t('settings.themeColorblind'), bg: '#f0f4f8', fg: '#1a1a2e' },
]

function switchLocale() {
  setLocale(locale.value)
}

function setTheme(theme: string) {
  currentTheme.value = theme
  localStorage.setItem('aurora_theme', theme)
  document.documentElement.setAttribute('data-theme', theme)
}

async function saveKeys() {
  // Submit keys to backend via the api interceptor
  // In production, use the generated SDK and submit to a secure endpoint
  if (anthropicKey.value) {
    localStorage.setItem('aurora_anthropic_key', anthropicKey.value)
  }
  if (openaiKey.value) {
    localStorage.setItem('aurora_openai_key', openaiKey.value)
  }
}

onMounted(() => {
  const saved = localStorage.getItem('aurora_theme')
  if (saved) {
    currentTheme.value = saved
    document.documentElement.setAttribute('data-theme', saved)
  }
})
</script>

<style scoped>
.settings-page { max-width: 800px; }
.settings-page h2 { color: #1f2937; margin-bottom: 1.5rem; }
.settings-layout { display: flex; gap: 2rem; }
.settings-tabs { display: flex; flex-direction: column; gap: 0.25rem; min-width: 180px; }
.tab-btn { padding: 0.5rem 1rem; background: none; border: none; border-radius: 6px; cursor: pointer; text-align: left; color: #4b5563; font-size: 0.875rem; }
.tab-btn:hover { background: #f3f4f6; }
.tab-btn.active { background: #eff6ff; color: #2563eb; font-weight: 600; }
.settings-content { flex: 1; }
.settings-content h3 { margin: 0 0 1rem; color: #1f2937; }
.note { font-size: 0.8125rem; color: #6b7280; margin-bottom: 1rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-size: 0.875rem; color: #374151; margin-bottom: 0.25rem; }
.form-input, .form-select { width: 100%; padding: 0.5rem 0.75rem; border: 1px solid #d1d5db; border-radius: 6px; font-size: 0.8125rem; }
.btn-primary { padding: 0.5rem 1.5rem; background: #3b82f6; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 0.875rem; }
.theme-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 1rem; }
.theme-card { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; padding: 1rem; border: 2px solid #e5e7eb; border-radius: 8px; cursor: pointer; background: none; }
.theme-card.active { border-color: #3b82f6; }
.theme-preview { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 1.125rem; }
</style>
