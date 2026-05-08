// frontend/src/api/admin-contract.ts
// Admin RBAC API contract — calls real /api/admin endpoints

import { getAuthToken, getTenantId } from './client'

const BASE = '/api/admin'

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

// ─── Users ───

export interface SysUser {
  id: string
  username: string
  email: string
  phone: string
  status: string
  roles: string[]
  createdAt: string
  updatedAt: string
}

export async function listUsers(): Promise<SysUser[]> {
  return request<SysUser[]>('/users')
}

export async function getUser(id: string): Promise<SysUser> {
  return request<SysUser>(`/users/${id}`)
}

export async function createUser(data: {
  username: string
  email?: string
  phone?: string
  password?: string
}): Promise<SysUser> {
  return request<SysUser>('/users', { method: 'POST', body: JSON.stringify(data) })
}

export async function updateUser(id: string, data: {
  username?: string
  email?: string
  phone?: string
  status?: string
}): Promise<SysUser> {
  return request<SysUser>(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export async function deleteUser(id: string): Promise<{ deleted: boolean }> {
  return request(`/users/${id}`, { method: 'DELETE' })
}

export async function assignUserRoles(id: string, roleIds: string[]): Promise<{ assigned: boolean }> {
  return request(`/users/${id}/roles`, { method: 'POST', body: JSON.stringify({ roleIds }) })
}

// ─── Roles ───

export interface SysRole {
  id: string
  roleCode: string
  roleName: string
  description: string
  status: string
  createdAt: string
  updatedAt: string
}

export async function listRoles(): Promise<SysRole[]> {
  return request<SysRole[]>('/roles')
}

export async function getRole(id: string): Promise<SysRole> {
  return request<SysRole>(`/roles/${id}`)
}

export async function createRole(data: {
  roleCode: string
  roleName: string
  description?: string
}): Promise<SysRole> {
  return request<SysRole>('/roles', { method: 'POST', body: JSON.stringify(data) })
}

export async function updateRole(id: string, data: {
  roleCode?: string
  roleName?: string
  description?: string
  status?: string
}): Promise<SysRole> {
  return request<SysRole>(`/roles/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export async function deleteRole(id: string): Promise<{ deleted: boolean }> {
  return request(`/roles/${id}`, { method: 'DELETE' })
}

export async function assignRoleMenus(id: string, menuIds: string[]): Promise<{ assigned: boolean }> {
  return request(`/roles/${id}/menus`, { method: 'POST', body: JSON.stringify({ menuIds }) })
}

export async function getRoleMenus(id: string): Promise<{ menuIds: string[] }> {
  return request(`/roles/${id}/menus`)
}

// ─── Menus ───

export interface SysMenu {
  id: string
  name: string
  path: string
  parentId: string
  sortOrder: number
  icon: string
  type: string
  permissionKey: string
  status: string
  createdAt: string
}

export async function listMenus(): Promise<SysMenu[]> {
  return request<SysMenu[]>('/menus')
}

export async function getMenu(id: string): Promise<SysMenu> {
  return request<SysMenu>(`/menus/${id}`)
}

export async function createMenu(data: {
  name: string
  path?: string
  parentId?: string
  sortOrder?: number
  icon?: string
  type?: string
  permissionKey?: string
}): Promise<SysMenu> {
  return request<SysMenu>('/menus', { method: 'POST', body: JSON.stringify(data) })
}

export async function updateMenu(id: string, data: {
  name?: string
  path?: string
  parentId?: string
  sortOrder?: number
  icon?: string
  type?: string
  permissionKey?: string
  status?: string
}): Promise<SysMenu> {
  return request<SysMenu>(`/menus/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export async function deleteMenu(id: string): Promise<{ deleted: boolean }> {
  return request(`/menus/${id}`, { method: 'DELETE' })
}

// ─── Button Permissions ───

export interface SysButton {
  id: string
  menuId: string
  buttonCode: string
  buttonName: string
  permissionKey: string
  status: string
  createdAt: string
}

export async function listButtons(): Promise<SysButton[]> {
  return request<SysButton[]>('/buttons')
}

export async function getButtonsByMenu(menuId: string): Promise<SysButton[]> {
  return request<SysButton[]>(`/buttons/menu/${menuId}`)
}

export async function createButton(data: {
  menuId: string
  buttonCode: string
  buttonName: string
  permissionKey?: string
}): Promise<SysButton> {
  return request<SysButton>('/buttons', { method: 'POST', body: JSON.stringify(data) })
}

export async function updateButton(id: string, data: {
  buttonCode?: string
  buttonName?: string
  permissionKey?: string
  status?: string
}): Promise<SysButton> {
  return request<SysButton>(`/buttons/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export async function deleteButton(id: string): Promise<{ deleted: boolean }> {
  return request(`/buttons/${id}`, { method: 'DELETE' })
}

// ─── Data Rules ───

export interface SysDataRule {
  id: string
  roleId: string
  resourceType: string
  ruleName: string
  ruleExpression: Record<string, unknown>
  status: string
  createdAt: string
  updatedAt: string
}

export async function listDataRules(): Promise<SysDataRule[]> {
  return request<SysDataRule[]>('/data-rules')
}

export async function getDataRulesByRole(roleId: string): Promise<SysDataRule[]> {
  return request<SysDataRule[]>(`/data-rules/role/${roleId}`)
}

export async function createDataRule(data: {
  roleId: string
  resourceType: string
  ruleName: string
  ruleExpression?: Record<string, unknown>
}): Promise<SysDataRule> {
  return request<SysDataRule>('/data-rules', { method: 'POST', body: JSON.stringify(data) })
}

export async function updateDataRule(id: string, data: {
  ruleName?: string
  ruleExpression?: Record<string, unknown>
  status?: string
}): Promise<SysDataRule> {
  return request<SysDataRule>(`/data-rules/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export async function deleteDataRule(id: string): Promise<{ deleted: boolean }> {
  return request(`/data-rules/${id}`, { method: 'DELETE' })
}
