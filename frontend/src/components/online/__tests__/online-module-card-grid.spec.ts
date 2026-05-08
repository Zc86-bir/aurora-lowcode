// @vitest-environment happy-dom

import { h } from 'vue'
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OnlineModuleCardGrid from '@/components/online/OnlineModuleCardGrid.vue'

describe('OnlineModuleCardGrid', () => {
  it('renders online module cards with title-matched links', () => {
    const wrapper = mount(OnlineModuleCardGrid, {
      props: {
        items: [
          {
            title: 'Online Forms',
            description: 'Configure published form assets.',
            to: '/online/forms',
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

    expect(wrapper.text()).toContain('Online Forms')
    expect(wrapper.text()).toContain('Configure published form assets.')
    expect(wrapper.find('a[data-route="/online/forms"]').text()).toContain('Online Forms')
  })
})
