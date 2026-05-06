// Remote Registry — static remote registration for Module Federation host.
// This is the single source of truth for what the host may load.
// DYNAMIC MODEL IS RESERVED FOR FUTURE VERSIONS ONLY.

export interface RemoteDefinition {
  remoteId: string
  remoteName: string
  entryUrl: string
  routeBase: string
  displayName: string
  exposes: Record<string, string>
  requiredCapabilities: string[]
  allowedTenants?: string[]
  compatibilityRange?: string
  enabled: boolean
}

export const remoteRegistry: RemoteDefinition[] = []

export function getEnabledRemotes(): RemoteDefinition[] {
  return remoteRegistry.filter((r) => r.enabled)
}

export function getRemotesForTenant(tenantId: string): RemoteDefinition[] {
  return remoteRegistry.filter(
    (r) => r.enabled && (!r.allowedTenants || r.allowedTenants.includes(tenantId))
  )
}
