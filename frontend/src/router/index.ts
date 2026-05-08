import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getEnabledRemotes } from '@/core/RemoteRegistry'

const coreRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/LayoutView.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
      },
      {
        path: 'forms',
        name: 'Forms',
        component: () => import('@/views/FormsView.vue'),
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/ReportsView.vue'),
      },
      {
        path: 'workflows',
        name: 'Workflows',
        component: () => import('@/views/WorkflowsView.vue'),
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/SettingsView.vue'),
      },
      {
        path: 'generate',
        name: 'Generate',
        component: () => import('@/views/GenerateView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      // Enterprise Console — System
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/SystemUsersView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/SystemRolesView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'system/menus',
        name: 'SystemMenus',
        component: () => import('@/views/SystemMenusView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'system/buttons',
        name: 'SystemButtons',
        component: () => import('@/views/SystemButtonsView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'system/data-rules',
        name: 'SystemDataRules',
        component: () => import('@/views/SystemDataRulesView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      // Enterprise Console — AI
      {
        path: 'ai/models',
        name: 'AiModelConfig',
        component: () => import('@/views/AiModelConfigView.vue'),
        meta: { roles: ['ADMIN'] },
      },
    ],
  },
]

// Remote extension routes — loaded from static registry
const remoteRoutes: RouteRecordRaw[] = getEnabledRemotes().map((remote) => ({
  path: `/${remote.routeBase}`,
  name: `remote:${remote.remoteId}`,
  component: () => import(/* @vite-ignore */ remote.entryUrl).catch(() => ({
    template: '<div class="error-state">Remote module failed to load</div>'
  })),
  meta: {
    requiresAuth: true,
    remoteId: remote.remoteId,
    requiredCapabilities: remote.requiredCapabilities,
  },
}))

const routes: RouteRecordRaw[] = [
  ...coreRoutes,
  ...remoteRoutes,
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard — check auth token and tenant
router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth === false) {
    return true
  }

  // Dev mode: auto-login with demo token
  if (import.meta.env.DEV && !authStore.isAuthenticated) {
    authStore.setToken('dev-demo-token-for-local-ui-development')
    // Set default tenant for dev mode
    if (typeof window !== 'undefined' && !localStorage.getItem('tenant_id')) {
      localStorage.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')
    }
    return true
  }

  if (!authStore.isAuthenticated) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  // Role-based guard
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles?.length && !requiredRoles.some(r => authStore.roles.includes(r))) {
    return '/dashboard'
  }

  return true
})

export default router
