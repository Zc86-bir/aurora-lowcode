import { describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useConsoleStore } from '@/stores/console'

describe('console store', () => {
  it('groups menu items by top-level domain and tracks current domain', () => {
    setActivePinia(createPinia())
    const store = useConsoleStore()

    store.setMenuTree([
      { id: 'm1', code: 'menu:workbench:home', title: 'Workbench', route: '/workbench', domain: 'workbench', children: [] },
      { id: 'm2', code: 'menu:system:users', title: 'Users', route: '/system/users', domain: 'system', children: [] },
    ])
    store.setCurrentRoute('/system/users')

    expect(store.menuTree).toHaveLength(2)
    expect(store.domains.map((item) => item.domain)).toEqual(['workbench', 'system'])
    expect(store.currentDomain).toBe('system')
  })

  it('matches the most specific route on path boundaries only', () => {
    setActivePinia(createPinia())
    const store = useConsoleStore()

    store.setMenuTree([
      { id: 'm1', code: 'menu:system:home', title: 'System', route: '/system', domain: 'system', children: [] },
      { id: 'm2', code: 'menu:system:users', title: 'Users', route: '/system/users', domain: 'system', children: [] },
      { id: 'm3', code: 'menu:ops:home', title: 'Ops', route: '/systematic', domain: 'ops', children: [] },
    ])

    store.setCurrentRoute('/system/users/details')
    expect(store.currentDomain).toBe('system')

    store.setCurrentRoute('/systematic-tools')
    expect(store.currentDomain).toBe('workbench')
  })
})
