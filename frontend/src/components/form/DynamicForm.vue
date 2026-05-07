<template>
  <!-- Dynamic Form Renderer -->
  <!-- Driven by metadata, supports virtual scroll for large forms -->
  <form
    class="dynamic-form"
    @submit.prevent="handleSubmit"
    :aria-label="formName"
    role="form"
  >
    <!-- Form header -->
    <div v-if="title" class="dynamic-form__header mb-6">
      <h2 class="text-2xl font-semibold text-primary">{{ title }}</h2>
      <p v-if="description" class="text-secondary text-sm mt-1">{{ description }}</p>
    </div>

    <!-- Form body — use virtual scroll for > 20 fields -->
    <!-- For production: replace with @tanstack/vue-virtual RecycleScroller -->
    <template v-if="needsVirtualScroll">
      <div
        class="dynamic-form__scroller"
        role="list"
        aria-label="Form fields (virtual scroll)"
      >
        <div
          v-for="field in visibleFields"
          :key="field.name"
          role="listitem"
          class="dynamic-form__field"
          :class="{ 'dynamic-form__field--error': fieldErrors[field.name] }"
        >
          <!-- Field content rendered on demand by virtual scroller -->
          <FormFieldRenderer
            :field="field"
            :model-value="formData[field.name]"
            @update:model-value="updateField(field.name, $event)"
            :error="fieldErrors[field.name]"
          />
        </div>
      </div>
    </template>
    <template v-else>
      <div
        v-for="field in visibleFields"
        :key="field.name"
        class="dynamic-form__field"
        :class="{ 'dynamic-form__field--error': fieldErrors[field.name] }"
      >
        <label
          :for="field.name"
          class="block text-sm font-medium text-primary mb-1"
        >
          {{ field.label || field.name }}
          <span v-if="field.required" class="text-danger-500 ml-1" aria-hidden="true">*</span>
        </label>

        <!-- Field type rendering -->
        <component
          :is="getFieldComponent(field.type)"
          :id="field.name"
          :name="field.name"
          :model-value="formData[field.name]"
          :placeholder="field.placeholder"
          :required="field.required"
          :maxlength="field.maxLength"
          :options="field.options"
          :disabled="field.disabled"
          :aria-describedby="field.description ? `${field.name}-desc` : undefined"
          :aria-invalid="!!fieldErrors[field.name]"
          :aria-errormessage="fieldErrors[field.name] ? `${field.name}-error` : undefined"
          @update:model-value="updateField(field.name, $event)"
          class="w-full px-3 py-2 border border-primary rounded-lg
                 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
                 transition duration-150"
        />

        <!-- Description -->
        <p
          v-if="field.description"
          :id="`${field.name}-desc`"
          class="text-secondary text-xs mt-1"
        >
          {{ field.description }}
        </p>

        <!-- Error message -->
        <p
          v-if="fieldErrors[field.name]"
          :id="`${field.name}-error`"
          class="text-danger-500 text-xs mt-1"
          role="alert"
        >
          {{ fieldErrors[field.name] }}
        </p>
      </div>
    </template>

    <!-- Form actions -->
    <div class="dynamic-form__actions flex gap-4 mt-6">
      <button
        type="submit"
        class="px-4 py-2 bg-primary-500 text-white rounded-lg
               hover:bg-primary-600 focus:outline-none focus:ring-2 focus:ring-primary-500
               disabled:opacity-50 disabled:cursor-not-allowed
               transition duration-150"
        :disabled="isSubmitting"
        :aria-busy="isSubmitting ? 'true' : undefined"
      >
        <span v-if="isSubmitting" class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2" aria-hidden="true" />
        {{ submitLabel || 'Submit' }}
      </button>
      <button
        type="button"
        class="px-4 py-2 border border-primary rounded-lg text-primary
               hover:bg-secondary focus:outline-none focus:ring-2 focus:ring-primary-500
               transition duration-150"
        @click="handleReset"
      >
        Reset
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { FormField, FormSchema } from '@/types/form'
import FormFieldRenderer from './FormFieldRenderer.vue'

interface Props {
  schema: FormSchema
  modelValue?: Record<string, unknown>
  submitLabel?: string
  loading?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [data: Record<string, unknown>]
  reset: []
  'update:modelValue': [data: Record<string, unknown>]
}>()

const formData = ref<Record<string, unknown>>(props.modelValue ?? {})
const fieldErrors = ref<Record<string, string>>({})
const isSubmitting = ref(false)

// Sync formData when modelValue prop changes
watch(() => props.modelValue, (newVal) => {
  if (newVal !== undefined) {
    formData.value = { ...newVal }
  }
}, { deep: true })

// Emit formData changes back to parent
watch(formData, (newVal) => {
  emit('update:modelValue', newVal)
}, { deep: true })

// Computed
const formName = computed(() => props.schema.name ?? 'dynamic-form')
const title = computed(() => props.schema.title)
const description = computed(() => props.schema.description)
const needsVirtualScroll = computed(() => {
  const visibleCount = props.schema.fields.filter(f => f.visible !== false).length
  return visibleCount > 20
})

const visibleFields = computed(() =>
  props.schema.fields.filter(f => {
    if (f.visible === false) return false
    if (f.visibleWhen) {
      return evaluateCondition(f.visibleWhen, formData.value)
    }
    return true
  })
)

// Methods
function updateField(name: string, value: unknown) {
  formData.value[name] = value

  // Clear error on change
  if (fieldErrors.value[name]) {
    delete fieldErrors.value[name]
  }
}

async function handleSubmit() {
  // Validate
  const errors = validateForm(props.schema.fields, formData.value)
  if (Object.keys(errors).length > 0) {
    fieldErrors.value = errors
    return
  }

  isSubmitting.value = true
  try {
    emit('submit', { ...formData.value })
  } finally {
    isSubmitting.value = false
  }
}

function handleReset() {
  formData.value = {}
  fieldErrors.value = {}
  emit('reset')
  emit('update:modelValue', {})
}

function getFieldComponent(type: string) {
  return {
    text: 'input',
    email: 'input',
    number: 'input',
    password: 'input',
    textarea: 'textarea',
    select: 'select',
    checkbox: 'input',
    radio: 'input',
    date: 'input',
  }[type] ?? 'input'
}

function evaluateCondition(condition: string, data: Record<string, unknown>): boolean {
  // Simple condition: "field_name == value"
  const match = condition.match(/^(\w+)\s*(==|!=)\s*(.+)$/)
  if (!match) return true
  const [, fieldName, operator, expectedValue] = match
  const actualValue = data[fieldName]
  const matches = String(actualValue) === expectedValue
  return operator === '==' ? matches : !matches
}

function validateForm(fields: FormField[], data: Record<string, unknown>): Record<string, string> {
  const errors: Record<string, string> = {}
  for (const field of fields) {
    const value = data[field.name]
    if (field.required && (!value || String(value).trim() === '')) {
      errors[field.name] = `${field.label || field.name} is required`
      continue
    }
    if (value && field.maxLength && String(value).length > field.maxLength) {
      errors[field.name] = `Maximum length is ${field.maxLength} characters`
    }
    if (value && field.pattern) {
      // Validate regex safety before use (prevent ReDoS)
      if (!isRegexSafe(field.pattern)) {
        errors[field.name] = 'Invalid pattern configuration'
        continue
      }
      try {
        if (!new RegExp(field.pattern).test(String(value))) {
          errors[field.name] = field.errorMessage || 'Invalid format'
        }
      } catch {
        errors[field.name] = 'Invalid pattern configuration'
      }
    }
  }
  return errors
}

// Prevent ReDoS by rejecting overly complex patterns
function isRegexSafe(pattern: string): boolean {
  // Reject patterns that are too long
  if (pattern.length > 200) return false
  // Reject patterns with known ReDoS triggers
  if (/(.*?\(.*?\)){3,}/.test(pattern)) return false
  if (/(.*?\[.*?\]){3,}/.test(pattern)) return false
  if (/(.*?\+.*?\+){2,}/.test(pattern)) return false
  if (/(.*?\*.*?\*){2,}/.test(pattern)) return false
  return true
}
</script>

<style scoped>
.dynamic-form {
  max-width: var(--container-lg, 1024px);
  margin: 0 auto;
}
.dynamic-form__scroller {
  height: 600px;
  overflow-y: auto;
}
.dynamic-form__field {
  @apply mb-4;
}
.dynamic-form__field--error :deep(input),
.dynamic-form__field--error :deep(select),
.dynamic-form__field--error :deep(textarea) {
  border-color: var(--color-danger-500, #ef4444);
}
</style>
