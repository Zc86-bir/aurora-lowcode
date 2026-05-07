export interface FormField {
  name: string
  label?: string
  type: string
  required?: boolean
  disabled?: boolean
  placeholder?: string
  maxLength?: number
  pattern?: string
  errorMessage?: string
  description?: string
  options?: Array<{ label: string; value: unknown }>
  visible?: boolean
  visibleWhen?: string
  defaultValue?: unknown
}

export interface FormSchema {
  name: string
  title?: string
  description?: string
  fields: FormField[]
  layout?: 'vertical' | 'horizontal' | 'grid' | 'wizard'
  validation?: Record<string, unknown>
}
