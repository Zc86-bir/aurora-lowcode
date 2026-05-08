<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-icon">◆</div>
      <div class="brand-text">Aurora</div>
    </div>

    <nav
      v-for="group in consoleStore.domains"
      :key="group.domain"
      class="nav-section"
    >
      <div class="nav-section-label">{{ group.title }}</div>
      <div
        v-for="item in group.items"
        :key="item.id"
        class="nav-group"
      >
        <ConsoleSidebarItem :item="item" :depth="0" />
      </div>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { defineComponent, h, resolveComponent, type VNode } from 'vue'
import { useConsoleStore } from '@/stores/console'
import type { ConsoleMenuItem } from '@/types/console'

const consoleStore = useConsoleStore()

const ConsoleSidebarItem = defineComponent({
  name: 'ConsoleSidebarItem',
  props: {
    item: {
      type: Object as () => ConsoleMenuItem,
      required: true,
    },
    depth: {
      type: Number,
      required: true,
    },
  },
  setup(props) {
    const RouterLink = resolveComponent('router-link')

    return (): VNode[] => {
      const nodes: VNode[] = [
        h(
          RouterLink,
          {
            to: props.item.route,
            class: ['nav-item', { 'nav-item-child': props.depth > 0 }],
          },
          () => props.item.title,
        ),
      ]

      for (const child of props.item.children) {
        nodes.push(h(ConsoleSidebarItem, {
          key: child.id,
          item: child,
          depth: props.depth + 1,
        }))
      }

      return nodes
    }
  },
})
</script>

<style scoped>
.sidebar {
  width: 220px;
  background: var(--color-sidebar-bg, #1e293b);
  color: #cbd5e1;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 1.25rem 1.25rem 1rem;
}

.brand-icon {
  font-size: 1.25rem;
  color: var(--color-primary);
}

.brand-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: white;
}

.nav-section {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 0.75rem;
}

.nav-section + .nav-section {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin-top: 0.25rem;
  padding-top: 0.75rem;
}

.nav-section-label {
  font-size: 0.65rem;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.25rem 0.75rem 0.5rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  color: #94a3b8;
  text-decoration: none;
  font-size: var(--text-sm);
  transition: all var(--transition);
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

.nav-item.router-link-active {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
  font-weight: 500;
}

.nav-group {
  display: flex;
  flex-direction: column;
}

.nav-item-child {
  padding-left: 1.75rem;
  font-size: var(--text-xs);
}
</style>
