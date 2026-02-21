import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

vi.mock('./components/IntensityChart', () => ({
  IntensityChart: () => <div data-testid="intensity-chart" />,
}))

class MockEventSource {
  onmessage: ((event: MessageEvent<string>) => void) | null = null
  onerror: (() => void) | null = null
  constructor(url: string) {
    void url
  }
  close() {}
}

describe('App', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(() => ({
        matches: false,
        media: '(prefers-color-scheme: dark)',
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      })),
    })

    vi.stubGlobal('EventSource', MockEventSource)
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const path = input.toString()
        if (path.includes('/api/ai/settings')) {
          return new Response(
            JSON.stringify({ language: 'EN', supportedLanguages: ['PL', 'EN'] }),
            { status: 200 },
          )
        }
        if (path.includes('/api/dashboard/summary/week/')) {
          return new Response(
            JSON.stringify({
              period: 'week',
              startDate: '2026-02-15',
              endDate: '2026-02-21',
              daysWithData: 1,
              totalEvents: 100,
              dailyMetrics: [],
            }),
            { status: 200 },
          )
        }
        if (path.includes('/api/dashboard/health')) {
          return new Response(
            JSON.stringify({
              activeWebSocketConnections: 0,
              status: 'healthy',
            }),
            { status: 200 },
          )
        }
        return new Response('{}', { status: 200 })
      }),
    )
  })

  it('renders dashboard heading and theme controls', async () => {
    render(<App />)
    expect(screen.getByText(/Interactive Operations Dashboard/i)).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /light/i })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /dark/i })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /system/i })).toBeInTheDocument()
  })
})
