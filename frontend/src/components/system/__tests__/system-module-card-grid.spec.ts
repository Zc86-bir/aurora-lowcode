// @vitest-environment happy-dom

import { h } from 'vue'
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SystemModuleCardGrid from '@/components/system/SystemModuleCardGrid.vue'

describe('SystemModuleCardGrid', () => {
  it('renders module cards with titles, descriptions, and open links', () => {
    const wrapper = mount(SystemModuleCardGrid, {
      props: {
        items: [
          {
            title: 'Departments',
            description: 'Manage reporting lines and operating structure.',
            to: '/system/departments',
          },
          {
            title: 'Tenants',
            description: 'Review tenant lifecycle and subscription state.',
            to: '/system/tenants',
          },
        ],
      },
      global: {
        stubs: {
          'router-link': {
            props: ['to'],
            setup(props, { slots }) {
              return () => h('a', { 'data-route-name': String(props.to) }, slots.default?.())
            },
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Departments')
    expect(wrapper.text()).toContain('Manage reporting lines and operating structure.')
    expect(wrapper.text()).toContain('Tenants')
    expect(wrapper.findAll('a')).toHaveLength(2)
    expect(wrapper.find('a[data-route-name="/system/departments"]').text()).toContain('Open')
  })
})
