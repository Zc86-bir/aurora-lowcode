import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fallbackConsoleMenuTree, getConsoleDomainTitle } from '@/core/console/console-shell'
import type { ConsoleDomain, ConsoleMenuItem, DomainMenuGroup } from '@/types/console'

export const useConsoleStore = defineStore('console', () => {
  const menuTree = ref<ConsoleMenuItem[]>(cloneMenuTree(fallbackConsoleMenuTree))
  const currentRoute = ref('')

  const domains = computed<DomainMenuGroup[]>(() => {
    const groups = new Map<ConsoleDomain, ConsoleMenuItem[]>()

    for (const item of menuTree.value) {
      const items = groups.get(item.domain)
      if (items) {
        items.push(item)
        continue
      }

      groups.set(item.domain, [item])
    }

    return Array.from(groups.entries()).map(([domain, items]) => ({
      domain,
      title: getConsoleDomainTitle(domain),
      items,
    }))
  })

  const currentDomain = computed<ConsoleDomain>(() => {
    const matchedItem = findMenuItemByRoute(menuTree.value, currentRoute.value)
    return matchedItem?.domain ?? 'workbench'
  })

  function setMenuTree(items: ConsoleMenuItem[]) {
    menuTree.value = cloneMenuTree(items.length > 0 ? items : fallbackConsoleMenuTree)
  }

  function setCurrentRoute(route: string) {
    currentRoute.value = route
  }

  return {
    menuTree,
    currentRoute,
    domains,
    currentDomain,
    setMenuTree,
    setCurrentRoute,
  }
})

function findMenuItemByRoute(items: ConsoleMenuItem[], route: string): ConsoleMenuItem | undefined {
  let matchedItem: ConsoleMenuItem | undefined

  for (const item of items) {
    if (isRouteMatch(item.route, route)) {
      const childMatch = findMenuItemByRoute(item.children, route)
      const candidate = childMatch ?? item

      if (!matchedItem || candidate.route.length > matchedItem.route.length) {
        matchedItem = candidate
      }
    }
  }

  return matchedItem
}

function isRouteMatch(candidateRoute: string, route: string): boolean {
  return route === candidateRoute || route.startsWith(`${candidateRoute}/`)
}

function cloneMenuTree(items: ConsoleMenuItem[]): ConsoleMenuItem[] {
  return items.map((item) => ({
    ...item,
    children: cloneMenuTree(item.children),
  }))
}
