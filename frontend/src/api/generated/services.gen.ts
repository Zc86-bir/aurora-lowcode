// Auto-generated service layer — typed composables wrapping useGet/usePost/usePut/useDelete
// Each function MUST be called inside <script setup> or setup()

import { useGet, usePost, usePut, useDelete } from '@/composables/useServerState'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  LogoutResponse,
  MeResponse,
  MetadataItem,
  MetadataStats,
  FormRenderResponse,
  FormValidationResult,
  FormVisibilityResult,
  ReportExecutionResult,
  ReportSchema,
  WorkflowStartResponse,
  WorkflowValidationResult,
  WorkflowCancelResponse,
  GenerateCrudRequest,
  GenerateCrudResponse,
  GenerateAppRequest,
  GenerateAppResponse,
  RollbackResult,
  KnowledgeDocument,
  KnowledgeUploadResponse,
  KnowledgeImportUrlRequest,
  WebhookEndpoint,
  CreateWebhookRequest,
  CreateWebhookResponse,
  UpdateWebhookRequest,
  ApiKey,
  CreateApiKeyRequest,
  CreateApiKeyResponse,
  FileUploadResponse,
  PresignedUrlResponse,
  HealthResponse,
  PaginatedResponse,
} from './types.gen'

const API = '/api/v1'

// ─── Auth ───

export function useLogin() {
  return usePost<LoginResponse, LoginRequest>('/auth/login')
}

export function useLogout() {
  return usePost<LogoutResponse, void>('/auth/logout')
}

export function useMe() {
  return useGet<MeResponse>('me', '/auth/me')
}

// ─── Forms ───

export function useFormRender(formName: string, enabled?: boolean | import('vue').Ref<boolean>) {
  return useGet<ApiResponse<FormRenderResponse>>(`form-${formName}`, `${API}/forms/${formName}/render`, { enabled })
}

export function useFormValidate(formName: string) {
  return usePost<ApiResponse<FormValidationResult>, Record<string, unknown>>(`${API}/forms/${formName}/validate`)
}

export function useFormVisibility(formName: string) {
  return usePost<ApiResponse<FormVisibilityResult>, Record<string, unknown>>(`${API}/forms/${formName}/visibility`)
}

export function useFormDefaults(formName: string) {
  return useGet<ApiResponse<Record<string, unknown>>>(`form-defaults-${formName}`, `${API}/forms/${formName}/defaults`)
}

// ─── Reports ───

export function useReportExecute(reportName: string, filters?: Record<string, string | number | boolean>) {
  return useGet<ApiResponse<ReportExecutionResult>>(`report-${reportName}`, `${API}/reports/${reportName}/execute`, {
    params: filters as Record<string, string | number | boolean>,
  })
}

export function useReportSchema(reportName: string) {
  return useGet<ApiResponse<ReportSchema>>(`report-schema-${reportName}`, `${API}/reports/${reportName}/schema`)
}

export function useReportExport(reportName: string, filters?: Record<string, string | number | boolean>) {
  const query = new URLSearchParams(filters as Record<string, string> | undefined).toString()
  return `${API}/reports/${reportName}/export${query ? `?${query}` : ''}`
}

// ─── Workflows ───

export function useWorkflowStart(workflowName: string) {
  return usePost<ApiResponse<WorkflowStartResponse>, Record<string, unknown>>(`${API}/workflows/${workflowName}/start`)
}

export function useWorkflowValidate(workflowName: string) {
  return usePost<ApiResponse<WorkflowValidationResult>>(`${API}/workflows/${workflowName}/validate`)
}

export function useWorkflowCancel(instanceId: string) {
  return usePost<ApiResponse<WorkflowCancelResponse>>(`${API}/workflows/instances/${instanceId}/cancel`)
}

// ─── Metadata / hot-reload ───

export function useMetadataList(enabled?: boolean | import('vue').Ref<boolean>) {
  return useGet<ApiResponse<MetadataItem[]>>('metadata', `${API}/metadata/list`, { enabled })
}

export function useMetadataStats(enabled?: boolean | import('vue').Ref<boolean>) {
  return useGet<ApiResponse<MetadataStats>>('metadata-stats', `${API}/metadata/stats`, { enabled })
}

export function useMetadataReload() {
  return usePost<ApiResponse<unknown>, void>(`${API}/metadata/reload`)
}

export function useMetadataRollback() {
  return usePost<ApiResponse<RollbackResult>, { metadataName: string; targetVersion: number }>(
    `${API}/metadata/rollback`
  )
}

export function useHealth() {
  return useGet<ApiResponse<HealthResponse>>('health', `${API}/health`)
}

// ─── Code generation ───

export function useGenerateCrud() {
  return usePost<ApiResponse<GenerateCrudResponse>, GenerateCrudRequest>(`${API}/generate/crud`)
}

export function useGenerateApp() {
  return usePost<ApiResponse<GenerateAppResponse>, GenerateAppRequest>(`${API}/generate/app`)
}

// ─── Knowledge ───

export function useKnowledgeList(enabled?: boolean | import('vue').Ref<boolean>) {
  return useGet<KnowledgeDocument[]>('knowledge', `${API}/knowledge`, { enabled })
}

export function useKnowledgeUpload() {
  return usePost<KnowledgeUploadResponse, FormData>(`${API}/knowledge/upload`)
}

export function useKnowledgeImportUrl() {
  return usePost<{ documentId: string; status: string }, KnowledgeImportUrlRequest>(
    `${API}/knowledge/import-url`
  )
}

// ─── Webhooks ───

export function useWebhookList(enabled?: boolean | import('vue').Ref<boolean>) {
  return useGet<WebhookEndpoint[]>('webhooks', `${API}/webhooks`, { enabled })
}

export function useCreateWebhook() {
  return usePost<CreateWebhookResponse, CreateWebhookRequest>(`${API}/webhooks`)
}

export function useUpdateWebhook(endpointId: string) {
  return usePut<{ message: string; id: string }, UpdateWebhookRequest>(
    `${API}/webhooks/${endpointId}`
  )
}

export function useDeleteWebhook(endpointId: string) {
  return useDelete<{ message: string; id: string }, void>(`${API}/webhooks/${endpointId}`)
}

export function useRegenerateWebhookSecret(endpointId: string) {
  return usePost<{ message: string; id: string; secret: string; warning: string | null }, void>(
    `${API}/webhooks/${endpointId}/regenerate-secret`
  )
}

// ─── API Keys ───

export function useApiKeyList(enabled?: boolean | import('vue').Ref<boolean>) {
  return useGet<ApiKey[]>('apikeys', `${API}/apikeys`, { enabled })
}

export function useCreateApiKey() {
  return usePost<CreateApiKeyResponse, CreateApiKeyRequest>(`${API}/apikeys`)
}

export function useDeleteApiKey(keyId: string) {
  return useDelete<{ message: string; id: string }, void>(`${API}/apikeys/${keyId}`)
}

// ─── Files ───

export function useFileUpload() {
  return usePost<ApiResponse<FileUploadResponse>, FormData>(`${API}/files/upload`)
}

export function usePresignedUrl(key: string) {
  return useGet<ApiResponse<PresignedUrlResponse>>(`presigned-${key}`, `${API}/files/presigned-url`, {
    params: { key },
  })
}

export function useDeleteFile(key: string) {
  return useDelete<ApiResponse<{ deleted: string }>, void>(`${API}/files/${key}`)
}
