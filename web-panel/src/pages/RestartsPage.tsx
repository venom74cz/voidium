import { useEffect, useState } from 'react'
import { api } from '../api'
import { useTr } from '../i18n'
import { useDashboard } from '../context/DashboardContext'
import { useToast } from '../components/Toast'
import { ConfigStudio } from '../components/ConfigStudio'
import { PageHeader } from '../components/PageHeader'
import { formatDurationCompact } from '../utils'
import type { ConfigValues, Module } from '../types'

function findModule(modules: Module[], names: string[]) {
  return modules.find(module => names.some(name => module.name.toLowerCase().includes(name.toLowerCase())))
}

function listValue(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(item => String(item ?? '').trim()).filter(Boolean)
  if (typeof value === 'string') {
    return value
      .split(/\r?\n/g)
      .map(item => item.trim())
      .filter(Boolean)
  }
  return []
}

function modeLabel(mode: string, tr: (en: string, cz: string) => string) {
  if (mode === 'INTERVAL') return tr('Interval schedule', 'Intervalovy plan')
  if (mode === 'DELAY') return tr('Delay schedule', 'Zpozdeny plan')
  return tr('Fixed times', 'Pevne casy')
}

export function RestartsPage() {
  const tr = useTr()
  const { data, timers, fetchDashboard } = useDashboard()
  const { toast } = useToast()
  const [config, setConfig] = useState<ConfigValues | null>(null)
  const [loading, setLoading] = useState(true)
  const [scheduling, setScheduling] = useState(false)

  useEffect(() => {
    let active = true

    async function load() {
      try {
        const values = await api.configValues()
        if (active) setConfig(values)
      } finally {
        if (active) setLoading(false)
      }
    }

    load()
    return () => {
      active = false
    }
  }, [])

  const module = findModule(data?.modules || [], ['restart', 'restarty'])
  const section = config?.restart || {}
  const general = config?.general || {}
  const fixedTimes = listValue(section.fixedRestartTimes)
  const restartTimer = timers.find(timer => timer.id === 'restart')
  const mode = String(section.restartType || 'FIXED_TIME')
  const enabled = Boolean(general.enableRestarts)

  async function scheduleRestart() {
    setScheduling(true)
    toast(tr('Scheduling restart...', 'Planuju restart...'), 'info')
    try {
      const result = await api.action('restart')
      toast(result.message || tr('Restart scheduled.', 'Restart naplanovan.'), 'success')
      fetchDashboard()
    } catch (error) {
      toast(error instanceof Error ? error.message : tr('Restart scheduling failed.', 'Planovani restartu selhalo.'), 'error')
    } finally {
      setScheduling(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Restarts"
        titleCz="Restarty"
        description="Restart schedule, warning messages, and manual restart control"
        descriptionCz="Restart plan, warning zpravy a rucni restart ovladani"
      />

      <div className="page-grid-2">
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('SCHEDULE', 'PLAN')}</span>
              <h2>{tr('Restart Overview', 'Prehled restartu')}</h2>
            </div>
          </div>
          <div className="overview-grid">
            <div className="overview-card">
              <span>{tr('Module', 'Modul')}</span>
              <strong className={module?.enabled && enabled ? 'text-ok' : 'text-off'}>
                {module?.enabled && enabled ? tr('Active', 'Aktivni') : tr('Disabled', 'Vypnuto')}
              </strong>
              <small>{module?.detail || tr('Restart manager is not available.', 'Restart manager neni dostupny.')}</small>
            </div>
            <div className="overview-card">
              <span>{tr('Mode', 'Rezim')}</span>
              <strong>{modeLabel(mode, tr)}</strong>
              <small>{mode}</small>
            </div>
            <div className="overview-card">
              <span>{tr('Next restart', 'Dalsi restart')}</span>
              <strong>{data?.nextRestart || '—'}</strong>
              <small>
                {restartTimer
                  ? tr('Countdown: ', 'Odpocet: ') + formatDurationCompact(restartTimer.remainingSeconds)
                  : tr('No live countdown is active.', 'Neni aktivni zadny live odpocet.')}
              </small>
            </div>
            <div className="overview-card">
              <span>{tr('Configured times', 'Nastavene casy')}</span>
              <strong>{loading ? '...' : fixedTimes.length}</strong>
              <small>{tr('Fixed HH:MM slots currently configured.', 'Aktualne nastavene pevne casy HH:MM.')}</small>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('MANUAL', 'RUCNE')}</span>
              <h2>{tr('Manual Restart', 'Rucni restart')}</h2>
            </div>
          </div>
          <div className="action-stack">
            <p className="feature-note">
              {tr(
                'Use this for an operator-triggered restart. Scheduled cycles and warning timing stay controlled by the settings below.',
                'Tohle pouzij pro restart spusteny obsluhou. Planovane cykly a warning casovani zustavaji v nastaveni niz.'
              )}
            </p>
            <div className="button-row">
              <button className="danger" type="button" onClick={scheduleRestart} disabled={scheduling}>
                {scheduling ? tr('Scheduling...', 'Planuju...') : tr('Restart in 5 min', 'Restart za 5 min')}
              </button>
              <button className="ghost" type="button" onClick={() => fetchDashboard()}>
                {tr('Refresh data', 'Obnovit data')}
              </button>
            </div>
            <div className="feature-stat-list">
              <div>
                <span>{tr('Warning message', 'Warning zprava')}</span>
                <strong>{String(section.warningMessage || '—')}</strong>
              </div>
              <div>
                <span>{tr('Restart now message', 'Zprava pri restartu')}</span>
                <strong>{String(section.restartingNowMessage || '—')}</strong>
              </div>
              <div>
                <span>{tr('Kick message', 'Kick zprava')}</span>
                <strong>{String(section.kickMessage || '—')}</strong>
              </div>
            </div>
          </div>
        </article>
      </div>

      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('TIMETABLE', 'ROZPIS')}</span>
            <h2>{tr('Configured Restart Times', 'Nastavene casy restartu')}</h2>
          </div>
          <span className="role-count">{loading ? '...' : fixedTimes.length}</span>
        </div>
        {loading ? (
          <div className="loading-state">{tr('Loading restart settings...', 'Nacitam nastaveni restartu...')}</div>
        ) : mode !== 'FIXED_TIME' ? (
          <div className="empty-state">
            {mode === 'INTERVAL'
              ? tr('This server uses interval-based restarts.', 'Server pouziva intervalove restarty.')
              : tr('This server uses delayed restarts.', 'Server pouziva zpozdene restarty.')}
          </div>
        ) : fixedTimes.length === 0 ? (
          <div className="empty-state">{tr('No fixed restart times are configured.', 'Nejsou nastavene zadne pevne casy restartu.')}</div>
        ) : (
          <div className="feature-chip-grid">
            {fixedTimes.map(time => (
              <span key={time} className="config-chip">{time}</span>
            ))}
          </div>
        )}
      </article>

      <ConfigStudio
        sectionFilter="restart"
        title="Restart settings"
        titleCz="Nastaveni restartu"
        eyebrow="RESTARTS"
        eyebrowCz="RESTARTY"
      />
    </>
  )
}
