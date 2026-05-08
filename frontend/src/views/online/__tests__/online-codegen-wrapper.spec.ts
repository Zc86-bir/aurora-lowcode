// @vitest-environment happy-dom
import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import OnlineCodeGeneratorView from '@/views/online/codegen/OnlineCodeGeneratorView.vue'

vi.mock('@/views/GenerateView.vue', () => ({
  default: { template: '<div data-testid="legacy-generate-view">legacy generate</div>' },
}))

describe('online code generator wrapper', () => {
  it('mounts the legacy generator implementation', () => {
    const wrapper = mount(OnlineCodeGeneratorView)

    expect(wrapper.find('[data-testid="legacy-generate-view"]').exists()).toBe(true)
  })
})
