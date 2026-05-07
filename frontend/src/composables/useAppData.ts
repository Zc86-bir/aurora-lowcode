// Unified data layer — real API with dev-mode fallback
import { ref, computed, type Ref } from 'vue'
import {
  DEV_FORMS,
  DEV_REPORTS,
  DEV_WORKFLOWS,
  DEV_REPORT_DATA,
  DEV_BPMN_XML,
  isDev,
} from '@/api/dev-data'
import type {
  AppFormItem,
  AppReportItem,
  AppWorkflowItem,
} from '@/api/dev-data'
import type { ReportExecutionResult } from '@/api/generated/types.gen'

// ─── Internal helpers ───

async function apiGet<T>(url: string): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

async function apiPost<T>(url: string, body: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

// ─── Forms ───

export function useAppForms() {
  const items = ref<AppFormItem[]>(isDev() ? [...DEV_FORMS] : [])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    error.value = null
    try {
      const res = await apiGet<{ data: AppFormItem[] }>('/api/v1/metadata?type=FORM')
      if (res.data?.length) items.value = res.data
    } catch {
      if (isDev()) {
        items.value = [...DEV_FORMS]
      } else {
        error.value = 'Failed to load forms'
      }
    } finally {
      loading.value = false
    }
  }

  if (!isDev() || !items.value.length) refresh()

  return { items: computed(() => items.value), loading, error, refresh }
}

// ─── Reports ───

export function useAppReports() {
  const items = ref<AppReportItem[]>(isDev() ? [...DEV_REPORTS] : [])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    error.value = null
    try {
      const res = await apiGet<{ data: AppReportItem[] }>('/api/v1/metadata?type=REPORT')
      if (res.data?.length) items.value = res.data
    } catch {
      if (isDev()) {
        items.value = [...DEV_REPORTS]
      } else {
        error.value = 'Failed to load reports'
      }
    } finally {
      loading.value = false
    }
  }

  if (!isDev() || !items.value.length) refresh()

  return { items: computed(() => items.value), loading, error, refresh }
}

export function useAppReportData(reportName: Ref<string>) {
  const result = ref<ReportExecutionResult | null>(null)
  const loading = ref(false)

  async function fetch() {
    loading.value = true
    try {
      const res = await apiGet<ReportExecutionResult>(`/api/v1/reports/${reportName.value}/execute`)
      result.value = res
    } catch {
      if (isDev()) {
        result.value = DEV_REPORT_DATA[reportName.value] || DEV_REPORT_DATA.monthly_sales_summary
      }
    } finally {
      loading.value = false
    }
  }

  if (reportName.value) fetch()

  return { result, loading, fetch }
}

// ─── Workflows ───

export function useAppWorkflows() {
  const items = ref<AppWorkflowItem[]>(isDev() ? [...DEV_WORKFLOWS] : [])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    error.value = null
    try {
      const res = await apiGet<{ data: AppWorkflowItem[] }>('/api/v1/metadata?type=WORKFLOW')
      if (res.data?.length) items.value = res.data
    } catch {
      if (isDev()) {
        items.value = [...DEV_WORKFLOWS]
      } else {
        error.value = 'Failed to load workflows'
      }
    } finally {
      loading.value = false
    }
  }

  if (!isDev() || !items.value.length) refresh()

  return { items: computed(() => items.value), loading, error, refresh }
}

export function useAppBpmn(workflowName: Ref<string>) {
  const xml = ref(DEV_BPMN_XML[workflowName.value] ?? '')

  return { xml }
}

// ─── Code Generation ───

export async function generateCrud(payload: {
  entityName: string
  tableName?: string
  packagePrefix?: string
  fields: { name: string; label: string; javaType: string; columnName: string; required: boolean; primaryKey: boolean; maxLength: number }[]
}) {
  try {
    const res = await apiPost<{ success: boolean; data: unknown; error: string | null }>(
      '/api/v1/generate/crud', payload
    )
    return res
  } catch {
    if (isDev()) {
      // Simulate generation delay
      await new Promise(r => setTimeout(r, 800))
      return {
        success: true,
        data: {
          files: [
            { path: `src/main/java/${payload.packagePrefix || 'com.aurora'}/entity/${payload.entityName}.java`, type: 'entity', checksum: 'a1b2c3d4' },
            { path: `src/main/java/${payload.packagePrefix || 'com.aurora'}/controller/${payload.entityName}Controller.java`, type: 'controller', checksum: 'e5f6g7h8' },
            { path: `src/main/java/${payload.packagePrefix || 'com.aurora'}/service/${payload.entityName}Service.java`, type: 'service', checksum: 'i9j0k1l2' },
            { path: `src/main/java/${payload.packagePrefix || 'com.aurora'}/repository/${payload.entityName}Repository.java`, type: 'repository', checksum: 'm3n4o5p6' },
            { path: `src/main/resources/db/migration/V2__create_${payload.tableName || payload.entityName.toLowerCase()}.sql`, type: 'ddl', checksum: 'q7r8s9t0' },
            { path: `src/test/java/${payload.packagePrefix || 'com.aurora'}/entity/${payload.entityName}Test.java`, type: 'test', checksum: 'u1v2w3x4' },
          ],
          commitHash: 'dev-simulated-' + Date.now().toString(36),
          duration: 824,
        },
        error: null,
      }
    }
    throw new Error('Backend unavailable')
  }
}

export async function generateApp(prompt: string) {
  try {
    const res = await apiPost<{ success: boolean; data: unknown; error: string | null }>(
      '/api/v1/generate/app', { prompt }
    )
    return res
  } catch {
    if (isDev()) {
      await new Promise(r => setTimeout(r, 1200))
      return {
        success: true,
        data: {
          success: true,
          requestId: 'dev-req-' + Date.now().toString(36),
          planName: prompt.slice(0, 40),
          completedAt: new Date().toISOString(),
          taskResultsByTaskId: {
            'T1': { success: true, output: { type: 'form', name: 'generated_form' }, errorMessage: null },
            'T2': { success: true, output: { type: 'workflow', name: 'generated_workflow' }, errorMessage: null },
            'T3': { success: true, output: { type: 'report', name: 'generated_report' }, errorMessage: null },
          },
          rollbackEntries: [],
          rollbackAuditMessages: [],
          errorMessage: null,
        },
        error: null,
      }
    }
    throw new Error('Backend unavailable')
  }
}
