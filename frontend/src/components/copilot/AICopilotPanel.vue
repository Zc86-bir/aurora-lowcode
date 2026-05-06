<template>
  <div class="copilot-container">
    <!-- Floating trigger button -->
    <button class="copilot-trigger" :class="{ active: isOpen }" @click="isOpen = !isOpen" :title="t('copilot.trigger')">
      <span v-if="!isOpen">🤖</span>
      <span v-else>✕</span>
    </button>

    <!-- Slide-out panel -->
    <Transition name="panel-slide">
      <div v-if="isOpen" class="copilot-panel">
        <!-- Header -->
        <div class="copilot-header">
          <h3>{{ t('copilot.title') }}</h3>
          <button v-if="messages.length" class="clear-btn" @click="clearHistory">{{ t('copilot.clear') }}</button>
        </div>

        <!-- Messages area -->
        <div ref="scrollRef" class="copilot-messages">
          <div v-if="!messages.length" class="empty-state">{{ t('copilot.empty') }}</div>
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="message"
            :class="msg.role"
          >
            <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="msg-content" v-html="renderMarkdown(msg.content)" />
          </div>
          <div v-if="isGenerating" class="message assistant">
            <div class="msg-avatar">🤖</div>
            <div class="msg-content typing-indicator">{{ t('copilot.generating') }}<span class="dots">...</span></div>
          </div>
        </div>

        <!-- Input area -->
        <div class="copilot-input-area">
          <input
            v-model="inputText"
            type="text"
            :placeholder="t('copilot.placeholder')"
            :disabled="isGenerating"
            @keydown.enter="submit"
          />
          <button v-if="!isGenerating" class="send-btn" @click="submit" :disabled="!inputText.trim()">{{ t('copilot.send') }}</button>
          <button v-else class="stop-btn" @click="stopGeneration">{{ t('copilot.stop') }}</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCopilotChat } from '@/composables/useCopilotChat'
import MarkdownIt from 'markdown-it'

const { t } = useI18n()
const { messages, isGenerating, sendMessage, stopGeneration, clearHistory } = useCopilotChat()

const md = new MarkdownIt({ html: true, linkify: true })
const isOpen = ref(false)
const inputText = ref('')
const scrollRef = ref<HTMLElement | null>(null)

function renderMarkdown(content: string): string {
  return md.render(content)
}

async function submit() {
  const text = inputText.value.trim()
  if (!text || isGenerating.value) return
  inputText.value = ''
  sendMessage(text)
  await nextTick()
  scrollRef.value?.scrollTo({ top: scrollRef.value.scrollHeight, behavior: 'smooth' })
}
</script>

<style scoped>
.copilot-trigger {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #3b82f6;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 1.25rem;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.copilot-trigger:hover { background: #2563eb; transform: scale(1.05); }
.copilot-trigger.active { background: #dc2626; }

.copilot-panel {
  position: fixed;
  bottom: 5rem;
  right: 1.5rem;
  width: 380px;
  max-height: 520px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
  z-index: 999;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.copilot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #1f2937;
  color: white;
}
.copilot-header h3 { margin: 0; font-size: 0.875rem; }
.clear-btn { background: none; border: none; color: #9ca3af; cursor: pointer; font-size: 0.75rem; }

.copilot-messages {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.empty-state {
  text-align: center;
  color: #9ca3af;
  padding: 2rem;
  font-size: 0.875rem;
}

.message {
  display: flex;
  gap: 0.5rem;
  max-width: 90%;
}
.message.assistant { align-self: flex-start; }
.message.user { align-self: flex-end; flex-direction: row-reverse; }

.msg-avatar { font-size: 1.25rem; flex-shrink: 0; }
.msg-content {
  background: #f3f4f6;
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  line-height: 1.4;
  word-break: break-word;
}
.message.user .msg-content { background: #3b82f6; color: white; }
.typing-indicator { color: #6b7280; }
.dots { animation: blink 1.4s steps(1) infinite; }
@keyframes blink { 50% { opacity: 0; } }

.copilot-input-area {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem;
  border-top: 1px solid #e5e7eb;
}
.copilot-input-area input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.8125rem;
}
.send-btn, .stop-btn {
  padding: 0.5rem 0.75rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.75rem;
}
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.panel-slide-enter-active, .panel-slide-leave-active { transition: all 0.25s ease; }
.panel-slide-enter-from, .panel-slide-leave-to { opacity: 0; transform: translateY(20px); }
</style>
