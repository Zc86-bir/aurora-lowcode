// Auto-generated API types from backend controllers
// Source: ApiGatewayController, AuthController, KnowledgeBaseController,
//         WebhookController, ApiKeyController, I18nController, FileStorageController

// ─── Generic wrappers ───

export interface ApiResponse<T = unknown> {
  success: boolean
  data: T
  error: string | null
}

// ─── Auth ───

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  tenantId: string
  tenantCode: string
  userId: string
  username: string
  roles: string[]
}

export interface LogoutResponse {
  message: string
}

export interface MeResponse {
  userId: string
  tenantId: string
  authenticated: boolean
}

// ─── Metadata ───

export interface MetadataItem {
  id: string
  tenantId: string
  name: string
  type: string
  version: number
  status: string
  checksumSha256: string
  tags: string[]
  createdBy: string
  createdAt: string
  updatedBy: string | null
  updatedAt: string
}

export interface MetadataStats {
  totalReloads: number
  totalFailures: number
  cachedItems: number
  diffEntries: number
}

// ─── Forms ───

export interface FormRenderResponse {
  schema: FormSchema
  defaults: Record<string, unknown>
}

export interface FormSchema {
  name: string
  fields: FormField[]
}

export interface FormField {
  name: string
  type: string
  label?: string
  required?: boolean
  defaultValue?: unknown
  options?: { label: string; value: string }[]
  validation?: Record<string, unknown>
  visibilityRules?: unknown[]
  maxLength?: number
  min?: number
  max?: number
  pattern?: string
}

export interface FormValidationResult {
  valid: boolean
  errors: FormFieldError[]
}

export interface FormFieldError {
  field: string
  message: string
}

export interface FormVisibilityResult {
  visible: boolean
  hiddenFields: string[]
}

// ─── Reports ───

export interface ReportExecutionResult {
  data: Record<string, unknown>[]
  pagination: {
    total: number
    page: number
    totalPages: number
  }
  executionTime: number
}

export interface ReportSchema {
  name: string
  columns: ReportColumn[]
  dataSource: string
  defaultPageSize: number
}

export interface ReportColumn {
  name: string
  type: string
  label: string
  sortable: boolean
  filterable: boolean
}

// ─── Workflows ───

export interface WorkflowStartResponse {
  instanceId: string
  status: string
}

export interface WorkflowValidationResult {
  valid: boolean
  errors: string[]
}

export interface WorkflowCancelResponse {
  cancelled: boolean
}

// ─── Code generation ───

export interface FieldDefinition {
  name: string
  label: string
  javaType: string
  columnName: string
  required: boolean
  primaryKey: boolean
  maxLength: number
}

export interface GenerateCrudRequest {
  entityName: string
  tableName?: string
  packagePrefix?: string
  fields: FieldDefinition[]
}

export interface GeneratedFile {
  path: string
  type: string
  checksum: string
}

export interface GenerateCrudResponse {
  files: GeneratedFile[]
  commitHash: string
  duration: number
}

export interface GenerateAppRequest {
  prompt: string
}

export interface TaskResult {
  success: boolean
  output: Record<string, unknown>
  errorMessage: string | null
}

export interface RollbackEntry {
  taskId: string
  metadataName: string
  version: number
}

export interface GenerateAppResponse {
  success: boolean
  requestId: string
  planName: string
  completedAt: string
  taskResultsByTaskId: Record<string, TaskResult>
  rollbackEntries: RollbackEntry[]
  rollbackAuditMessages: string[]
  errorMessage: string | null
}

// ─── Hot-reload ───

export interface RollbackResult {
  success: boolean
  metadataName: string
  targetVersion: number
  errorMessage: string | null
  duration: number
}

// ─── Knowledge ───

export interface KnowledgeDocument {
  id: string
  title: string
  scope: string
  sourceType: string
  status: string
  failureMessage: string | null
  createdAt: string
}

export interface KnowledgeUploadResponse {
  documentId: string
  status: string
  checksum: string
  title: string
}

export interface KnowledgeImportUrlRequest {
  url: string
  scope?: string
  projectId?: string
  moduleId?: string
  visibilityPolicy?: string
}

// ─── Webhooks ───

export interface WebhookEndpoint {
  id: string
  url: string
  events: string
  active: boolean
  description: string | null
  successCount: number
  failureCount: number
  lastDeliveredAt: string | null
  lastFailureAt: string | null
  lastFailureMessage: string | null
  createdAt: string
}

export interface CreateWebhookRequest {
  url: string
  events?: string
  description?: string
}

export interface CreateWebhookResponse {
  id: string
  url: string
  secret: string
  events: string
  active: boolean
  description: string | null
  createdAt: string
  warning: string | null
}

export interface UpdateWebhookRequest {
  url?: string
  events?: string
  active?: boolean
  description?: string
}

// ─── API Keys ───

export interface ApiKey {
  id: string
  name: string
  scopes: string
  status: string
  lastUsedAt: string | null
  expiresAt: string | null
  createdAt: string
}

export interface CreateApiKeyRequest {
  name: string
  scopes?: string
}

export interface CreateApiKeyResponse {
  id: string
  name: string
  rawKey: string
  scopes: string
  status: string
  createdAt: string
  warning: string | null
}

// ─── Files ───

export interface FileUploadResponse {
  key: string
  url: string
  etag: string
}

export interface PresignedUrlResponse {
  url: string
}

// ─── Health ───

export interface HealthResponse {
  status: string
  timestamp: string
}

// ─── Pagination ───

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
}
