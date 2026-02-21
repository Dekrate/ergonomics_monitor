import { useEffect, useMemo, useRef, useState } from 'react'
import { getDashboardHealth, getWeeklySummary } from '../api'
import type { DashboardHealth, RealtimeDashboardUpdate, WeeklySummary } from '../types'

export interface DashboardState {
  summary: WeeklySummary | null
  health: DashboardHealth | null
  liveEvents: RealtimeDashboardUpdate[]
  loading: boolean
  error: string | null
}

interface DashboardDataOptions {
  maxLiveEvents: number
  minRenderGapMs: number
}

export function useDashboardData(userId: string, options: DashboardDataOptions) {
  const [state, setState] = useState<DashboardState>({
    summary: null,
    health: null,
    liveEvents: [],
    loading: true,
    error: null,
  })
  const lastAcceptedAtRef = useRef(0)

  useEffect(() => {
    let cancelled = false

    async function loadInitialData() {
      setState((prev) => ({ ...prev, loading: true, error: null }))
      try {
        const [summary, health] = await Promise.all([
          getWeeklySummary(userId),
          getDashboardHealth(),
        ])
        if (!cancelled) {
          setState((prev) => ({ ...prev, summary, health, loading: false, error: null }))
        }
      } catch (error) {
        if (!cancelled) {
          setState((prev) => ({
            ...prev,
            loading: false,
            error: error instanceof Error ? error.message : 'Unknown error',
          }))
        }
      }
    }

    loadInitialData()
    return () => {
      cancelled = true
    }
  }, [userId])

  useEffect(() => {
    let disposed = false
    const eventSource = new EventSource(`/api/dashboard/stream/${encodeURIComponent(userId)}`)

    eventSource.onopen = () => {
      if (disposed) {
        return
      }
      setState((prev) => ({ ...prev, error: null }))
    }

    eventSource.onmessage = (event) => {
      if (disposed) {
        return
      }
      try {
        const update = JSON.parse(event.data) as RealtimeDashboardUpdate
        const now = Date.now()
        if (now - lastAcceptedAtRef.current < options.minRenderGapMs) {
          return
        }
        lastAcceptedAtRef.current = now

        setState((prev) => ({
          ...prev,
          liveEvents: [update, ...prev.liveEvents].slice(0, options.maxLiveEvents),
        }))
      } catch {
        setState((prev) => ({ ...prev, error: 'Realtime update is temporarily unavailable' }))
      }
    }

    eventSource.onerror = () => {
      if (disposed) {
        return
      }
      setState((prev) => ({ ...prev, error: 'Realtime connection interrupted, retrying...' }))
    }

    return () => {
      disposed = true
      eventSource.close()
    }
  }, [options.maxLiveEvents, options.minRenderGapMs, userId])

  const chartSeries = useMemo(
    () =>
      [...state.liveEvents]
        .reverse()
        .map((event) => ({
          label: new Date(event.timestamp).toLocaleTimeString(),
          value: event.currentMetrics.avgIntensity,
        })),
    [state.liveEvents],
  )

  return { state, chartSeries }
}
