export type ThemeMode = 'light' | 'dark' | 'system'

export type AiLanguage = 'PL' | 'EN'

export interface DashboardMetrics {
  totalEvents: number
  avgIntensity: number
}

export interface RealtimeDashboardUpdate {
  type: string
  timestamp: string
  userId: string
  currentMetrics: DashboardMetrics
}

export interface DailyMetrics {
  metricDate: string
  totalEvents: number
  avgIntensity: number
}

export interface WeeklySummary {
  period: string
  startDate: string
  endDate: string
  daysWithData: number
  totalEvents: number
  dailyMetrics: DailyMetrics[]
}

export interface DashboardHealth {
  activeWebSocketConnections: number
  status: string
}

export interface AiSettings {
  language: AiLanguage
  supportedLanguages: AiLanguage[]
}

export interface AiAskResponse {
  answer: string
  language: AiLanguage
  userId: string
  timestamp: string
}
