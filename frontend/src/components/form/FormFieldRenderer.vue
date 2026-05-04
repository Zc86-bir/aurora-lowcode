<template>
  <!-- Form Field Renderer — renders a single form field based on metadata type -->
  <component
    :is="componentType"
    :id="field.name"
    :name="field.name"
    :model-value="modelValue"
    :placeholder="field.placeholder"
    :required="field.required"
    :maxlength="field.maxLength"
    :disabled="field.disabled"
    :type="inputType"
    :aria-describedby="field.description ? `${field.name}-desc` : undefined"
    :aria-invalid="!!error"
    :aria-errormessage="error ? `${field.name}-error` : undefined"
    @update:model-value="$emit('update:modelValue', $event)"
    class="w-full px-3 py-2 border rounded-lg text-sm
           border-border-primary bg-bg-primary text-text-primary
           focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
           disabled:opacity-50 disabled:cursor-not-allowed
           transition duration-150"
    :class="{ 'border-danger-500': error }"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FormField } from '@/types/form'

interface Props {
  field: FormField
  modelValue?: unknown
  error?: string
}

const props = defineProps<Props>()
defineEmits<{
  'update:modelValue': [value: unknown]
}>()

const componentType = computed(() => {
  switch (props.field.type) {
    case 'textarea': return 'textarea'
    case 'select': return 'select'
    case 'checkbox': return 'input'
    case 'radio': return 'input'
    default: return 'input'
  }
})

const inputType = computed(() => {
  switch (props.field.type) {
    case 'email': return 'email'
    case 'number': return 'number'
    case 'password': return 'password'
    case 'date': return 'date'
    case 'text':
    case 'textarea':
    case 'select':
    case 'checkbox':
    case 'radio': return undefined
    default: return 'text'
  }
})
</script>
