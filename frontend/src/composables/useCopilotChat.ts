import { ref, onUnmounted } from 'vue'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

export function useCopilotChat() {
  const messages = ref<ChatMessage[]>([])
  const isGenerating = ref(false)
  const abortController = ref<AbortController | null>(null)

  // Cleanup SSE on unmount
  onUnmounted(() => {
    abortController.value?.abort()
  })

  function sendMessage(text: string) {
    if (!text.trim()) return
    messages.value.push({ role: 'user', content: text, timestamp: Date.now() })
    isGenerating.value = true

    const controller = new AbortController()
    abortController.value = controller

    const baseUrl = import.meta.env.VITE_API_BASE || '/api/v1'

    // Use fetch via apiFetch pattern (SSE requires raw fetch body reading)
    fetch(`${baseUrl}/copilot/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: text,
        history: messages.value.filter(m => m.role === 'user').slice(-10),
      }),
      signal: controller.signal,
    }).then(async (response) => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const reader = response.body?.getReader()
      if (!reader) throw new Error('No response body')

      const assistantMsg: ChatMessage = { role: 'assistant', content: '', timestamp: Date.now() }
      messages.value.push(assistantMsg)
      const decoder = new TextDecoder()

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        for (const line of chunk.split('\n')) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            if (data === '[DONE]') continue
            try {
              const parsed = JSON.parse(data)
              if (parsed.content) {
                assistantMsg.content += parsed.content
              }
            } catch {
              assistantMsg.content += data
            }
          }
        }
        // Trigger reactivity
        messages.value = [...messages.value]
      }
    }).catch((err) => {
      if (err.name === 'AbortError') return
      messages.value.push({
        role: 'assistant',
        content: `Error: ${err.message}`,
        timestamp: Date.now(),
      })
    }).finally(() => {
      isGenerating.value = false
      abortController.value = null
    })
  }

  function stopGeneration() {
    abortController.value?.abort()
    isGenerating.value = false
  }

  function clearHistory() {
    messages.value = []
  }

  return { messages, isGenerating, sendMessage, stopGeneration, clearHistory }
}
