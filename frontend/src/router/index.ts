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
