export type ConsoleDomain =
  | 'workbench'
  | 'system'
  | 'online'
  | 'ai'
  | 'analytics'
  | 'ops'
  | 'showcase'
  | 'advanced'
  | 'enterprise'

export interface ConsoleMenuItem {
  id: string
  code: string
  title: string
  route: string
  domain: ConsoleDomain
  icon?: string
  enabled?: boolean
  children: ConsoleMenuItem[]
}

export interface DomainMenuGroup {
  domain: ConsoleDomain
  title: string
  items: ConsoleMenuItem[]
}
