import { useState, useEffect, useCallback, useRef } from 'react'
import { LocaleProvider } from './i18n'
import { ThemeProvider } from './theme'
import { ToastProvider } from './components/Toast'
import { api } from './api'
import { Hero } from './components/Hero'
import { MaintenanceBanner } from './components/MaintenanceBanner'
import { MetricGrid } from './components/MetricGrid'
import { TimerGrid } from './components/TimerGrid'
import { Timeline } from './components/Timeline'
import { QuickActions } from './components/QuickActions'
import { SecurityPanel } from './components/SecurityPanel'
import { PlayerRoster } from './components/PlayerRoster'
import { ModuleHealth } from './components/ModuleHealth'
import { VoteQueue } from './components/VoteQueue'
import { TicketPanel } from './components/TicketPanel'
import { StatsCharts } from './components/StatsCharts'
import { ConfigStudio } from './components/ConfigStudio'
import { ServerProperties } from './components/ServerProperties'
import { Console } from './components/Console'
import { LiveFeeds, AuditFeed } from './components/LiveFeeds'
import { AiPanel } from './components/AiPanel'
import type { DashboardData, Timer } from './types'
import './styles/app.css'

export default function App() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [timers, setTimers] = useState<Timer[]>([])
  const [locale, setLocale] = useState<'en' | 'cz'>(() => {
    const saved = localStorage.getItem('voidium_locale')
    if (saved === 'cz') return 'cz'
    const params = new URLSearchParams(window.location.search)
    return params.get('lang') === 'cz' ? 'cz' : 'en'
  })
  const [theme, setTheme] = useState<'dark' | 'light'>(() =>
    (localStorage.getItem('voidium_theme') as 'light') === 'light' ? 'light' : 'dark'
  )
  const timersRef = useRef(timers)
  timersRef.current = timers

  const applyDashboard = useCallback((dashboard: DashboardData) => {
    setData(dashboard)
    const newTimers = (dashboard.timers || []).map(t => ({
      ...t,
      remainingSeconds: Number(t.remainingSeconds || 0),
      totalSeconds: Number(t.totalSeconds || 0),
    }))
    setTimers(newTimers)
  }, [])

  const fetchDashboard = useCallback(async () => {
    try {
      applyDashboard(await api.dashboard())
    } catch {
      // Dashboard fetch failed, retain existing data
    }
  }, [applyDashboard])

  useEffect(() => {
    // SSE connection for real-time dashboard updates
    const source = new EventSource('/api/events')
    let fallbackInterval: ReturnType<typeof setInterval> | null = null

    source.addEventListener('dashboard', (e) => {
      try {
        applyDashboard(JSON.parse(e.data))
      } catch { /* ignore parse errors */ }
    })

    source.onerror = () => {
      // SSE failed or disconnected — fall back to polling until reconnected
      if (!fallbackInterval) {
        fallbackInterval = setInterval(fetchDashboard, 10000)
      }
    }

    source.onopen = () => {
      // SSE reconnected — stop fallback polling
      if (fallbackInterval) {
        clearInterval(fallbackInterval)
        fallbackInterval = null
      }
    }

    // Timer tick — client-side countdown between SSE pushes
    const timerInterval = setInterval(() => {
      setTimers(prev => prev.map(t => ({
        ...t,
        remainingSeconds: Math.max(0, t.remainingSeconds - 1),
      })))
    }, 1000)

    return () => {
      source.close()
      if (fallbackInterval) clearInterval(fallbackInterval)
      clearInterval(timerInterval)
    }
  }, [applyDashboard, fetchDashboard])

  useEffect(() => {
    localStorage.setItem('voidium_locale', locale)
  }, [locale])

  useEffect(() => {
    localStorage.setItem('voidium_theme', theme)
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  const toggleTheme = useCallback(() => setTheme(t => t === 'dark' ? 'light' : 'dark'), [])

  const consoleFeedLines = (data?.consoleFeed || []).slice(-30).map(e => `[${e.level}] ${e.logger}: ${e.message}`)
  const chatFeedLines = (data?.chatFeed || []).slice(-20).map(e => `${e.sender}: ${e.message}`)

  return (
    <ThemeProvider value={theme}>
    <LocaleProvider value={locale}>
    <ToastProvider>
      <div className="noise" />
      <main className="shell">
        <Hero data={data} />
        <MaintenanceBanner active={data?.maintenanceMode ?? false} onRefresh={fetchDashboard} />
        <MetricGrid data={data} />
        <TimerGrid timers={timers} />
        <Timeline timers={timers} />

        <section className="grid split">
          <QuickActions data={data} onRefresh={fetchDashboard} />
          <SecurityPanel data={data} locale={locale} setLocale={setLocale} theme={theme} toggleTheme={toggleTheme} />
        </section>

        <section className="grid split">
          <PlayerRoster players={data?.players || []} />
          <ModuleHealth modules={data?.modules || []} />
        </section>

        <section className="grid split">
          <VoteQueue data={data?.voteQueue || { total: 0, players: [] }} onRefresh={fetchDashboard} />
        </section>

        <section className="grid split">
          <TicketPanel data={data?.tickets || { open: 0, items: [] }} onRefresh={fetchDashboard} />
        </section>

        <section className="grid split">
          <StatsCharts history={data?.history || []} />
        </section>

        <section className="grid split">
          <ConfigStudio />
        </section>

        <section className="grid split">
          <ServerProperties />
        </section>

        <section className="grid split">
          <Console />
          <AuditFeed auditFeed={data?.auditFeed || []} />
        </section>

        <LiveFeeds
          chatFeed={data?.chatFeed || []}
          consoleFeed={data?.consoleFeed || []}
          auditFeed={data?.auditFeed || []}
          alerts={data?.alerts || []}
        />

        <section className="grid split">
          <AiPanel
            ai={data?.ai || null}
            consoleFeedLines={consoleFeedLines}
            chatFeedLines={chatFeedLines}
          />
        </section>
      </main>
    </ToastProvider>
    </LocaleProvider>
    </ThemeProvider>
  )
}
