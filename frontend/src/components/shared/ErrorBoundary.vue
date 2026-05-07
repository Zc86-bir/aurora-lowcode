<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-icon">!</div>
    <h3>Something went wrong</h3>
    <p class="error-detail">{{ error?.message || 'An unexpected error occurred' }}</p>
    <button class="retry-btn" @click="reset">Try Again</button>
  </div>
  <template v-else>
    <slot />
  </template>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)
const error = ref<Error | null>(null)

onErrorCaptured((err, instance, info) => {
  console.error('[ErrorBoundary]', err, info)
  hasError.value = true
  error.value = err instanceof Error ? err : new Error(String(err))
  return false
})

function reset() {
  hasError.value = false
  error.value = null
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
  text-align: center;
}
.error-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fef2f2;
  color: #dc2626;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}
.error-boundary h3 { color: #1f2937; margin: 0 0 0.5rem; }
.error-detail { color: #6b7280; font-size: 0.8125rem; max-width: 400px; }
.retry-btn { margin-top: 1rem; padding: 0.5rem 1.5rem; background: #3b82f6; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 0.875rem; }
</style>
