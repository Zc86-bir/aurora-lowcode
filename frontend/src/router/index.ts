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
        path: 'workbench',
        name: 'WorkbenchHome',
        component: () => import('@/views/workbench/WorkbenchHomeView.vue'),
        meta: { title: 'Workbench' },
      },
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
        path: 'system',
        name: 'SystemHome',
        component: () => import('@/views/system/SystemHomeView.vue'),
        meta: { title: 'System Management', roles: ['ADMIN'] },
      },
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/SystemUsersView.vue'),
        meta: { title: 'User Management', roles: ['ADMIN'] },
      },
      {
        path: 'system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/SystemRolesView.vue'),
        meta: { title: 'Role Management', roles: ['ADMIN'] },
      },
      {
        path: 'system/menus',
        name: 'SystemMenus',
        component: () => import('@/views/SystemMenusView.vue'),
        meta: { title: 'Menu Management', roles: ['ADMIN'] },
      },
      {
        path: 'system/buttons',
        name: 'SystemButtons',
        component: () => import('@/views/SystemButtonsView.vue'),
        meta: { title: 'Button Permissions', roles: ['ADMIN'] },
      },
      {
        path: 'system/data-rules',
        name: 'SystemDataRules',
        component: () => import('@/views/SystemDataRulesView.vue'),
        meta: { title: 'Data Permission Rules', roles: ['ADMIN'] },
      },
      {
        path: 'system/form-permissions',
        name: 'SystemFormPermissions',
        component: () => import('@/views/system/form-permissions/SystemFormPermissionsView.vue'),
        meta: { title: 'Form Permissions', roles: ['ADMIN'] },
      },
      {
        path: 'system/departments',
        name: 'SystemDepartments',
        component: () => import('@/views/system/departments/SystemDepartmentsView.vue'),
        meta: { title: 'Departments', roles: ['ADMIN'] },
      },
      {
        path: 'system/my-departments',
        name: 'SystemMyDepartments',
        component: () => import('@/views/system/my-departments/MyDepartmentsView.vue'),
        meta: { title: 'My Departments', roles: ['ADMIN'] },
      },
      {
        path: 'system/dictionaries',
        name: 'SystemDictionaries',
        component: () => import('@/views/system/dictionaries/SystemDictionariesView.vue'),
        meta: { title: 'Dictionaries', roles: ['ADMIN'] },
      },
      {
        path: 'system/dictionary-categories',
        name: 'SystemDictionaryCategories',
        component: () => import('@/views/system/dictionary-categories/SystemDictionaryCategoriesView.vue'),
        meta: { title: 'Dictionary Categories', roles: ['ADMIN'] },
      },
      {
        path: 'system/notices',
        name: 'SystemNotices',
        component: () => import('@/views/system/notices/SystemNoticesView.vue'),
        meta: { title: 'Notices', roles: ['ADMIN'] },
      },
      {
        path: 'system/positions',
        name: 'SystemPositions',
        component: () => import('@/views/system/positions/SystemPositionsView.vue'),
        meta: { title: 'Positions', roles: ['ADMIN'] },
      },
      {
        path: 'system/contacts',
        name: 'SystemContacts',
        component: () => import('@/views/system/contacts/SystemContactsView.vue'),
        meta: { title: 'Contacts', roles: ['ADMIN'] },
      },
      {
        path: 'system/data-sources',
        name: 'SystemDataSources',
        component: () => import('@/views/system/data-sources/SystemDataSourcesView.vue'),
        meta: { title: 'Data Sources', roles: ['ADMIN'] },
      },
      {
        path: 'system/tenants',
        name: 'SystemTenants',
        component: () => import('@/views/system/tenants/SystemTenantsView.vue'),
        meta: { title: 'Tenant Management', roles: ['ADMIN'] },
      },
      // Enterprise Console — Online Low-Code
      {
        path: 'online',
        name: 'OnlineHome',
        component: () => import('@/views/online/OnlineHomeView.vue'),
        meta: { title: 'Online Low-Code', titleKey: 'online.onlineLowCode', roles: ['ADMIN'] },
      },
      {
        path: 'online/forms',
        name: 'OnlineForms',
        component: () => import('@/views/online/forms/OnlineFormsView.vue'),
        meta: { title: 'Online Forms', titleKey: 'online.onlineForms', roles: ['ADMIN'] },
      },
      {
        path: 'online/reports',
        name: 'OnlineReports',
        component: () => import('@/views/online/reports/OnlineReportsView.vue'),
        meta: { title: 'Online Reports', titleKey: 'online.onlineReports', roles: ['ADMIN'] },
      },
      {
        path: 'online/dashboards',
        name: 'OnlineDashboards',
        component: () => import('@/views/online/dashboards/OnlineDashboardsView.vue'),
        meta: { title: 'Online Dashboards', titleKey: 'online.onlineDashboards', roles: ['ADMIN'] },
      },
      {
        path: 'online/naming-rules',
        name: 'OnlineNamingRules',
        component: () => import('@/views/online/naming-rules/OnlineNamingRulesView.vue'),
        meta: { title: 'Online Naming Rules', titleKey: 'online.onlineNamingRules', roles: ['ADMIN'] },
      },
      {
        path: 'online/validation-rules',
        name: 'OnlineValidationRules',
        component: () => import('@/views/online/validation-rules/OnlineValidationRulesView.vue'),
        meta: { title: 'Online Validation Rules', titleKey: 'online.onlineValidationRules', roles: ['ADMIN'] },
      },
      {
        path: 'online/codegen',
        name: 'OnlineCodeGenerator',
        component: () => import('@/views/online/codegen/OnlineCodeGeneratorView.vue'),
        meta: { title: 'Online Code Generator', titleKey: 'online.onlineCodeGenerator', roles: ['ADMIN'] },
      },
// Enterprise Console — AI
      {
        path: 'ai',
        name: 'AiPlatformHome',
        component: () => import('@/views/ai/AiPlatformHomeView.vue'),
        meta: { title: 'AI Platform', titleKey: 'ai.platform', roles: ['ADMIN'] },
      },
      {
        path: 'ai/models',
        name: 'AiModelConfig',
        component: () => import('@/views/AiModelConfigView.vue'),
        meta: { title: 'Model Management', titleKey: 'ai.models', roles: ['ADMIN'] },
      },
      {
        path: 'ai/assistant',
        name: 'AiAssistant',
        component: () => import('@/views/ai/assistant/AiAssistantView.vue'),
        meta: { title: 'AI Assistant', titleKey: 'ai.assistant', roles: ['ADMIN'] },
      },
      {
        path: 'ai/generation',
        name: 'AiGeneration',
        component: () => import('@/views/ai/generation/AiGenerationView.vue'),
        meta: { title: 'AI Generation', titleKey: 'ai.generation', roles: ['ADMIN'] },
      },
      {
        path: 'ai/ocr',
        name: 'AiOcr',
        component: () => import('@/views/ai/ocr/AiOcrView.vue'),
        meta: { title: 'OCR Samples', titleKey: 'ai.ocr', roles: ['ADMIN'] },
      },
      {
        path: 'ai/knowledge',
        name: 'AiKnowledge',
        component: () => import('@/views/ai/knowledge/AiKnowledgeView.vue'),
        meta: { title: 'Knowledge Q&A', titleKey: 'ai.knowledge', roles: ['ADMIN'] },
      },
      {
        path: 'ai/workflows',
        name: 'AiWorkflows',
        component: () => import('@/views/ai/workflows/AiWorkflowsView.vue'),
        meta: { title: 'AI Workflows', titleKey: 'ai.workflows', roles: ['ADMIN'] },
      },
      {
        path: 'ai/image-chat',
        name: 'AiImageChat',
        component: () => import('@/views/ai/image-chat/AiImageChatView.vue'),
        meta: { title: 'Image Chat', titleKey: 'ai.imageChat', roles: ['ADMIN'] },
      },
      {
        path: 'ai/embed',
        name: 'AiEmbeddedChat',
        component: () => import('@/views/ai/embed/AiEmbeddedChatView.vue'),
        meta: { title: 'Embedded Chat', titleKey: 'ai.embed', roles: ['ADMIN'] },
      },
      {
        path: 'ai/mobile',
        name: 'AiMobileChat',
        component: () => import('@/views/ai/mobile/AiMobileChatView.vue'),
        meta: { title: 'Mobile Chat', titleKey: 'ai.mobile', roles: ['ADMIN'] },
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
    redirect: '/workbench',
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
