// @vitest-environment happy-dom

import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SystemEntityTable from '@/components/system/SystemEntityTable.vue'

describe('SystemEntityTable', () => {
  it('renders title, headers, and empty state without an actions slot', () => {
    const wrapper = mount(SystemEntityTable, {
      props: {
        title: 'Users',
        columns: ['Username', 'Email'],
        rows: [],
      },
    })

    expect(wrapper.text()).toContain('Users')
    expect(wrapper.text()).toContain('Username')
    expect(wrapper.text()).toContain('Email')
    expect(wrapper.text()).toContain('No records found.')
    expect(wrapper.find('.table-actions').exists()).toBe(false)
  })

  it('renders actions slot content when provided', () => {
    const wrapper = mount(SystemEntityTable, {
      props: {
        title: 'Users',
        columns: ['Username', 'Email'],
        rows: [],
      },
      slots: {
        actions: '<button type="button">Add User</button>',
      },
    })

    expect(wrapper.text()).toContain('Add User')
    expect(wrapper.find('.table-actions').exists()).toBe(true)
  })
})
