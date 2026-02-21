import { useEffect, useMemo, useState } from 'react'
import './App.css'
import { askAi, getAiSettings, updateAiLanguage } from './api'
import { AiLanguagePanel } from './components/AiLanguagePanel'
import { IntensityChart } from './components/IntensityChart'
import { MetricCard } from './components/MetricCard'
import { ThemeModeSwitch } from './components/ThemeModeSwitch'
import { useDashboardData } from './hooks/useDashboardData'
import { useTheme } from './hooks/useTheme'
import type { AiLanguage } from './types'

const DEFAULT_USER_ID = '00000000-0000-0000-0000-000000000001'

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function App() {
  const [userIdInput, setUserIdInput] = useState(DEFAULT_USER_ID)
  const [activeUserId, setActiveUserId] = useState(DEFAULT_USER_ID)
  const [aiLanguage, setAiLanguage] = useState<AiLanguage | null>(null)
  const [supportedLanguages, setSupportedLanguages] = useState<AiLanguage[]>(['PL', 'EN'])
  const [languageBusy, setLanguageBusy] = useState(false)
  const [languageError, setLanguageError] = useState<string | null>(null)
  const [maxLivePoints, setMaxLivePoints] = useState(24)
  const [minRenderGapMs, setMinRenderGapMs] = useState(1500)
  const [askQuestion, setAskQuestion] = useState('')
  const [askAnswer, setAskAnswer] = useState<string | null>(null)
  const [askBusy, setAskBusy] = useState(false)
  const [askError, setAskError] = useState<string | null>(null)

  const { mode, setMode } = useTheme()
  const { state, chartSeries } = useDashboardData(activeUserId, {
    maxLiveEvents: maxLivePoints,
    minRenderGapMs,
  })

  useEffect(() => {
    let cancelled = false
    getAiSettings()
      .then((settings) => {
        if (cancelled) {
          return
        }
        setAiLanguage(settings.language)
        setSupportedLanguages(settings.supportedLanguages)
      })
      .catch((error) => {
        if (!cancelled) {
          setLanguageError(error instanceof Error ? error.message : 'Failed to load AI settings')
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  async function handleLanguageChange(language: AiLanguage) {
    setLanguageBusy(true)
    setLanguageError(null)
    try {
      const settings = await updateAiLanguage(language)
      setAiLanguage(settings.language)
    } catch (error) {
      setLanguageError(error instanceof Error ? error.message : 'Failed to update AI language')
    } finally {
      setLanguageBusy(false)
    }
  }

  async function handleAskAi() {
    if (!askQuestion.trim()) {
      setAskError('Question cannot be empty.')
      return
    }

    setAskBusy(true)
    setAskError(null)
    try {
      const response = await askAi(activeUserId, askQuestion.trim())
      setAskAnswer(response.answer)
    } catch (error) {
      setAskError(error instanceof Error ? error.message : 'Failed to ask AI')
    } finally {
      setAskBusy(false)
    }
  }

  const userIdError = useMemo(() => {
    if (UUID_PATTERN.test(userIdInput)) {
      return null
    }
    return 'Enter a valid UUID user id.'
  }, [userIdInput])

  const latestLive = state.liveEvents[0]
  const latestIntensity = latestLive?.currentMetrics.avgIntensity ?? 0
  const latestEvents = latestLive?.currentMetrics.totalEvents ?? 0

  return (
    <div className="app-shell">
      <div className="orb orb--one" />
      <div className="orb orb--two" />
      <header className="hero">
        <div className="hero__heading">
          <p className="eyebrow">Ergonomics Monitor</p>
          <h1>Interactive Operations Dashboard</h1>
          <p className="hero__sub">
            Real-time activity monitoring with clear feedback loops for AI-driven break coaching.
          </p>
        </div>
        <ThemeModeSwitch mode={mode} onChange={setMode} />
      </header>

      <main className="content-grid">
        <section className="panel">
          <header className="panel__header">
            <h3>Session Context</h3>
            <p>Select the active user context for dashboard analytics.</p>
          </header>
          <div className="row">
            <label htmlFor="user-id">User UUID</label>
            <input
              id="user-id"
              value={userIdInput}
              onChange={(event) => setUserIdInput(event.target.value.trim())}
              aria-invalid={userIdError ? 'true' : 'false'}
            />
          </div>
          {userIdError && <p className="status status--error">{userIdError}</p>}
          <button
            type="button"
            className="button-primary"
            disabled={Boolean(userIdError)}
            onClick={() => setActiveUserId(userIdInput)}
          >
            Apply user context
          </button>
        </section>

        <AiLanguagePanel
          language={aiLanguage}
          supportedLanguages={supportedLanguages}
          updating={languageBusy}
          onChange={handleLanguageChange}
        />

        <section className="metrics-grid">
          <MetricCard
            title="Total Events"
            value={latestEvents.toLocaleString()}
            hint="From latest live update"
            accent="calm"
            icon={<span>⌁</span>}
          />
          <MetricCard
            title="Avg Intensity"
            value={latestIntensity.toFixed(1)}
            hint="Events per minute"
            accent={latestIntensity > 120 ? 'warning' : 'neutral'}
            icon={<span>◉</span>}
          />
          <MetricCard
            title="Weekly Total"
            value={(state.summary?.totalEvents ?? 0).toLocaleString()}
            hint={`Tracked days: ${state.summary?.daysWithData ?? 0}`}
            icon={<span>▣</span>}
          />
        </section>

        <IntensityChart dataPoints={chartSeries} />

        <section className="panel">
          <header className="panel__header">
            <h3>Runtime Health</h3>
            <p>Backend operational indicators and stream feedback.</p>
          </header>
          {state.loading && <p className="status">Loading dashboard metrics...</p>}
          {state.error && <p className="status status--error">{state.error}</p>}
          {languageError && <p className="status status--error">{languageError}</p>}
          {state.health && (
            <ul className="status-list">
              <li>Status: {state.health.status}</li>
              <li>Active WebSocket connections: {state.health.activeWebSocketConnections}</li>
              <li>Live points buffered: {state.liveEvents.length}</li>
            </ul>
          )}
        </section>

        <section className="panel">
          <header className="panel__header">
            <h3>Live Stream Configuration</h3>
            <p>Tune how many points are rendered and how frequently updates repaint the chart.</p>
          </header>
          <div className="row">
            <label htmlFor="max-points">Max visible points</label>
            <select
              id="max-points"
              value={maxLivePoints}
              onChange={(event) => setMaxLivePoints(Number(event.target.value))}
            >
              <option value={12}>12</option>
              <option value={24}>24</option>
              <option value={36}>36</option>
              <option value={60}>60</option>
            </select>
          </div>
          <div className="row">
            <label htmlFor="render-gap">Minimal render gap (ms)</label>
            <select
              id="render-gap"
              value={minRenderGapMs}
              onChange={(event) => setMinRenderGapMs(Number(event.target.value))}
            >
              <option value={500}>500</option>
              <option value={1500}>1500</option>
              <option value={3000}>3000</option>
              <option value={5000}>5000</option>
            </select>
          </div>
        </section>

        <section className="panel panel--full">
          <header className="panel__header">
            <h3>Ask AI</h3>
            <p>Ask contextual questions based on recent activity data for the active user.</p>
          </header>
          <div className="row">
            <label htmlFor="ask-ai-input">Question</label>
            <textarea
              id="ask-ai-input"
              rows={4}
              maxLength={800}
              value={askQuestion}
              onChange={(event) => setAskQuestion(event.target.value)}
              placeholder="Example: Do my current metrics indicate overuse risk this hour?"
            />
          </div>
          <button type="button" className="button-primary" onClick={handleAskAi} disabled={askBusy}>
            {askBusy ? 'Asking AI...' : 'Ask AI'}
          </button>
          {askError && <p className="status status--error">{askError}</p>}
          {askAnswer && (
            <div className="ai-answer">
              <p>{askAnswer}</p>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default App
