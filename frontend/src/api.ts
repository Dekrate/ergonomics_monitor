import type {
  AiAskResponse,
  AiLanguage,
  AiSettings,
  DashboardHealth,
  WeeklySummary,
} from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(`Request failed (${response.status}): ${body || response.statusText}`)
  }

  return (await response.json()) as T
}

export async function getWeeklySummary(userId: string): Promise<WeeklySummary> {
  return fetchJson<WeeklySummary>(`/api/dashboard/summary/week/${encodeURIComponent(userId)}`)
}

export async function getDashboardHealth(): Promise<DashboardHealth> {
  return fetchJson<DashboardHealth>('/api/dashboard/health')
}

export async function getAiSettings(): Promise<AiSettings> {
  return fetchJson<AiSettings>('/api/ai/settings')
}

export async function updateAiLanguage(language: AiLanguage): Promise<AiSettings> {
  return fetchJson<AiSettings>('/api/ai/settings', {
    method: 'PATCH',
    body: JSON.stringify({ language }),
  })
}

export async function askAi(userId: string, question: string): Promise<AiAskResponse> {
  return fetchJson<AiAskResponse>('/api/ai/ask', {
    method: 'POST',
    body: JSON.stringify({ userId, question }),
  })
}
