import { useEffect, useState } from 'react'
import { api } from '../api'
import { useTr } from '../i18n'
import { useDashboard } from '../context/DashboardContext'
import { useToast } from '../components/Toast'
import { PageHeader } from '../components/PageHeader'
import { ConfigStudio } from '../components/ConfigStudio'
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

export function AnnouncementsPage() {
  const tr = useTr()
  const { data, fetchDashboard } = useDashboard()
  const { toast } = useToast()
  const [config, setConfig] = useState<ConfigValues | null>(null)
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)

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

  const section = config?.announcements || {}
  const general = config?.general || {}
  const module = findModule(data?.modules || [], ['announcement', 'oznam'])
  const lines = listValue(section.announcements)
  const interval = Number(section.announcementIntervalMinutes || 0)
  const enabled = Boolean(general.enableAnnouncements)

  async function handleBroadcast(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const message = String(form.get('message') || '').trim()
    if (!message) return

    setSending(true)
    toast(tr('Sending announcement...', 'Odesilam oznameni...'), 'info')
    try {
      const result = await api.action('announce', { message })
      toast(result.message || tr('Announcement sent.', 'Oznameni odeslano.'), 'success')
      formElement.reset()
      fetchDashboard()
    } catch (error) {
      toast(error instanceof Error ? error.message : tr('Announcement failed.', 'Oznameni selhalo.'), 'error')
    } finally {
      setSending(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Announcements"
        titleCz="Oznameni"
        description="Scheduled announcements, manual broadcasts, and message formatting"
        descriptionCz="Planovana oznameni, rucni broadcasty a formatovani zprav"
      />

      <div className="page-grid-2">
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('OVERVIEW', 'PREHLED')}</span>
              <h2>{tr('Announcement Overview', 'Prehled oznameni')}</h2>
            </div>
          </div>
          <div className="overview-grid">
            <div className="overview-card">
              <span>{tr('Module', 'Modul')}</span>
              <strong className={module?.enabled && enabled ? 'text-ok' : 'text-off'}>
                {module?.enabled && enabled ? tr('Active', 'Aktivni') : tr('Disabled', 'Vypnuto')}
              </strong>
              <small>{module?.detail || tr('Rotation is not available.', 'Rotace neni dostupna.')}</small>
            </div>
            <div className="overview-card">
              <span>{tr('Interval', 'Interval')}</span>
              <strong>{interval > 0 ? `${interval} min` : tr('Manual only', 'Jen rucne')}</strong>
              <small>{tr('0 disables automated rotation.', '0 vypina automatickou rotaci.')}</small>
            </div>
            <div className="overview-card">
              <span>{tr('Configured lines', 'Nastavene radky')}</span>
              <strong>{loading ? '...' : lines.length}</strong>
              <small>{tr('Messages currently included in the rotation.', 'Zpravy, ktere jsou ted v rotaci.')}</small>
            </div>
            <div className="overview-card">
              <span>{tr('Prefix', 'Prefix')}</span>
              <strong>{String(section.prefix || '—')}</strong>
              <small>{tr('Injected before each rotated line.', 'Pridava se pred kazdy rotovany radek.')}</small>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('LIVE PUSH', 'LIVE PUSH')}</span>
              <h2>{tr('Send an Announcement Now', 'Odeslat oznameni hned')}</h2>
            </div>
          </div>
          <form className="action-stack" onSubmit={handleBroadcast}>
            <label>
              <span>{tr('Broadcast message', 'Broadcast zprava')}</span>
              <input
                type="text"
                name="message"
                placeholder={tr('Type a live announcement for all players', 'Napis zive oznameni pro vsechny hrace')}
                disabled={sending}
                required
              />
            </label>
            <div className="button-row">
              <button type="submit" disabled={sending}>
                {sending ? tr('Sending...', 'Odesilam...') : tr('Broadcast now', 'Odeslat ted')}
              </button>
              <button className="ghost" type="button" onClick={() => fetchDashboard()}>
                {tr('Refresh dashboard data', 'Obnovit dashboard data')}
              </button>
            </div>
            <p className="feature-note">
              {tr(
                'Use this for a one-time broadcast. The rotating list stays managed in the announcement settings below.',
                'Tohle pouzij pro jednorazovy broadcast. Rotacni seznam zustava v nastaveni oznameni niz.'
              )}
            </p>
          </form>
        </article>
      </div>

      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('ROTATION', 'ROTACE')}</span>
            <h2>{tr('Configured Announcement Lines', 'Nastavene radky oznameni')}</h2>
          </div>
          <span className="role-count">{loading ? '...' : lines.length}</span>
        </div>
        {loading ? (
          <div className="loading-state">{tr('Loading announcement list...', 'Nacitam seznam oznameni...')}</div>
        ) : lines.length === 0 ? (
          <div className="empty-state">{tr('No rotating announcement lines configured.', 'Nejsou nastavene zadne rotacni radky.')}</div>
        ) : (
          <div className="feature-line-list">
            {lines.map((line, index) => (
              <div key={`${index}-${line}`} className="feature-line-item">
                <strong>#{index + 1}</strong>
                <span>{line}</span>
              </div>
            ))}
          </div>
        )}
      </article>

      <ConfigStudio
        sectionFilter="announcements"
        title="Announcement settings"
        titleCz="Nastaveni oznameni"
        eyebrow="ANNOUNCEMENTS"
        eyebrowCz="OZNAMENI"
      />
    </>
  )
}
