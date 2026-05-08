// @vitest-environment happy-dom

import { h } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import LayoutView from '@/views/LayoutView.vue'
import { useConsoleStore } from '@/stores/console'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    path: '/workbench',
    meta: {},
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    username: null,
    logout: vi.fn(),
  }),
}))

describe('layout shell', () => {
  it('renders grouped console domains instead of hardcoded legacy links', () => {
    setActivePinia(createPinia())
    const store = useConsoleStore()
    store.setMenuTree([
      {
        id: 'w1',
        code: 'menu:workbench:home',
        title: 'Workbench',
        route: '/workbench',
        domain: 'workbench',
        children: [
          {
            id: 'w1-1',
            code: 'menu:workbench:tasks',
            title: 'My Tasks',
            route: '/workbench/tasks',
            domain: 'workbench',
            children: [
              {
                id: 'w1-1-1',
                code: 'menu:workbench:tasks:detail',
                title: 'Task Detail',
                route: '/workbench/tasks/detail',
                domain: 'workbench',
                children: [],
              },
            ],
          },
        ],
      },
      { id: 's1', code: 'menu:system:home', title: 'System Management', route: '/system', domain: 'system', children: [] },
    ])

    const wrapper = mount(LayoutView, {
      global: {
        stubs: {
          'router-link': {
            setup(_, { slots }) {
              return () => h('a', slots.default?.())
            },
          },
          'router-view': true,
          Suspense: true,
          AICopilotPanel: true,
        },
      },
    })

    const sections = wrapper.findAll('.nav-section')

    expect(sections).toHaveLength(store.domains.length)

    for (const [index, group] of store.domains.entries()) {
      const section = sections[index]
      const sectionLabel = section.find('.nav-section-label')
      const flattenTitles = (items: typeof group.items): string[] => {
        return items.flatMap((item) => [item.title, ...flattenTitles(item.children)])
      }
      const expectedItems = flattenTitles(group.items)

      expect(sectionLabel.text()).toBe(group.title)

      for (const expectedItem of expectedItems) {
        expect(section.text()).toContain(expectedItem)
      }
    }

    expect(wrapper.text()).toContain('Workbench')
    expect(wrapper.text()).toContain('My Tasks')
    expect(wrapper.text()).toContain('Task Detail')
    expect(wrapper.text()).toContain('System Management')
    expect(wrapper.text()).not.toContain('Forms')
  })
})
