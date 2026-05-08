// frontend/src/api/ai-contract.ts
// AI Model Configuration API contract — calls real /api/ai/models endpoints

import { getAuthToken, getTenantId } from './client'

const BASE = '/api/ai/models'

function headers(): Record<string, string> {
  const h: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getAuthToken()
  if (token) h['Authorization'] = `Bearer ${token}`
  const tenant = getTenantId()
  if (tenant) h['X-Tenant-Id'] = tenant
  return h
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { ...headers(), ...(init?.headers as Record<string, string>) },
  })
  const body = await res.json() as { success: boolean; data: T; error?: string }
  if (!body.success) throw new Error(body.error ?? 'Request failed')
  return body.data
}

export interface AiModelConfig {
  id: string
  tenantId: string
  modelId: string
  requestUrl: string
  displayName: string
  provider: string
  status: string
  isDefault: boolean
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

export interface TestConnectionResult {
  success: boolean
  message: string
  latencyMs: number
}

export async function listModelConfigs(): Promise<AiModelConfig[]> {
  return request<AiModelConfig[]>('')
}

export async function getModelConfig(id: string): Promise<AiModelConfig> {
  return request<AiModelConfig>(`/${id}`)
}

export async function createModelConfig(data: {
  modelId: string
  apiKey?: string
  requestUrl?: string
  displayName?: string
  provider?: string
  isDefault?: boolean
}): Promise<AiModelConfig> {
  return request<AiModelConfig>('', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateModelConfig(id: string, data: {
  modelId?: string
  apiKey?: string
  requestUrl?: string
  displayName?: string
}): Promise<AiModelConfig> {
  return request<AiModelConfig>(`/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteModelConfig(id: string): Promise<{ deleted: boolean }> {
  return request<{ deleted: boolean }>(`/${id}`, { method: 'DELETE' })
}

export async function setDefaultModelConfig(id: string): Promise<AiModelConfig> {
  return request<AiModelConfig>(`/${id}/default`, { method: 'POST' })
}

export async function toggleModelConfigStatus(id: string): Promise<AiModelConfig> {
  return request<AiModelConfig>(`/${id}/toggle`, { method: 'POST' })
}

export async function testConnection(data: {
  requestUrl: string
  apiKey?: string
}): Promise<TestConnectionResult> {
  return request<TestConnectionResult>('/test-connection', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function getModelStats(): Promise<{
  total: number
  enabled: number
  disabled: number
  hasDefault: boolean
}> {
  return request('/stats')
}
