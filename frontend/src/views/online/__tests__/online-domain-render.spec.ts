// @vitest-environment happy-dom
import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import OnlineFormsView from '@/views/online/forms/OnlineFormsView.vue'
import OnlineReportsView from '@/views/online/reports/OnlineReportsView.vue'

vi.mock('@/views/FormsView.vue', () => ({
  default: { template: '<div data-testid="legacy-forms-view">legacy forms</div>' },
}))

vi.mock('@/views/ReportsView.vue', () => ({
  default: { template: '<div data-testid="legacy-reports-view">legacy reports</div>' },
}))

describe('online domain wrappers', () => {
  it('mounts the legacy forms implementation through the online domain route view', () => {
    const wrapper = mount(OnlineFormsView)

    expect(wrapper.find('[data-testid="legacy-forms-view"]').exists()).toBe(true)
  })

  it('mounts the legacy reports implementation through the online domain route view', () => {
    const wrapper = mount(OnlineReportsView)

    expect(wrapper.find('[data-testid="legacy-reports-view"]').exists()).toBe(true)
  })
})
