// @vitest-environment happy-dom
import { h } from 'vue'
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AiModuleCardGrid from '@/components/ai/AiModuleCardGrid.vue'

describe('AiModuleCardGrid', () => {
  it('renders ai capability cards with title, status, and action link', () => {
    const wrapper = mount(AiModuleCardGrid, {
      props: {
        items: [
          {
            title: 'AI Assistant',
            description: 'Open the assistant workspace.',
            status: 'Live',
            to: '/ai/assistant',
            ctaLabel: 'Open capability',
          },
        ],
      },
      global: {
        stubs: {
          'router-link': {
            props: ['to'],
            setup(props, { slots }) {
              return () => h('a', { 'data-route': String(props.to) }, slots.default?.())
            },
          },
        },
      },
    })

    expect(wrapper.text()).toContain('AI Assistant')
    expect(wrapper.text()).toContain('Live')
    expect(wrapper.text()).toContain('Open capability')
    expect(wrapper.find('a[data-route="/ai/assistant"]').exists()).toBe(true)
  })
})
