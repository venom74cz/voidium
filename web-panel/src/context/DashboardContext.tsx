import { createContext, useContext, useState, useEffect, useCallback, useRef, type ReactNode } from 'react'
import { api } from '../api'
import type { DashboardData, Timer } from '../types'

interface DashboardContextValue {
  data: DashboardData | null
  timers: Timer[]
  fetchDashboard: () => Promise<void>
}

const DashboardContext = createContext<DashboardContextValue>({
  data: null,
  timers: [],
  fetchDashboard: async () => {},
})

export function useDashboard() {
  return useContext(DashboardContext)
}

export function DashboardProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState<DashboardData | null>(null)
  const [timers, setTimers] = useState<Timer[]>([])
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
      // retain existing data on failure
    }
  }, [applyDashboard])

  useEffect(() => {
    fetchDashboard()

    let source: EventSource | null = null
    let fallbackInterval: ReturnType<typeof setInterval> | null = null


    if (!api.isDemoMode()) {
      source = new EventSource('/api/events')

      source.addEventListener('dashboard', (e) => {
        try {
          applyDashboard(JSON.parse(e.data))
        } catch { /* ignore parse errors */ }
      })

      source.onerror = () => {
        if (!fallbackInterval) {
          fallbackInterval = setInterval(fetchDashboard, 10000)
        }
      }

      source.onopen = () => {
        if (fallbackInterval) {
          clearInterval(fallbackInterval)
          fallbackInterval = null
        }
      }
    }

    const timerInterval = setInterval(() => {
      setTimers(prev => prev.map(t => ({
        ...t,
        remainingSeconds: Math.max(0, t.remainingSeconds - 1),
      })))
    }, 1000)

    return () => {
      source?.close()
      if (fallbackInterval) clearInterval(fallbackInterval)
      clearInterval(timerInterval)
    }
  }, [applyDashboard, fetchDashboard])

  return (
    <DashboardContext.Provider value={{ data, timers, fetchDashboard }}>
      {children}
    </DashboardContext.Provider>
  )
}
