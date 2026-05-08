export interface OnlineNamingRule {
  id: string
  name: string
  pattern: string
  status: string
}

export interface OnlineValidationRule {
  id: string
  name: string
  targetType: string
  expression: string
  status: string
}

export interface OnlineDashboardEntry {
  id: string
  name: string
  status: string
  widgetCount: number
}
