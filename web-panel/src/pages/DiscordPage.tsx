import { useState, useEffect } from 'react'
import { useTr } from '../i18n'
import { useDashboard } from '../context/DashboardContext'
import { ConfigStudio } from '../components/ConfigStudio'
import { PageHeader } from '../components/PageHeader'
import { api } from '../api'
import type { DiscordRole } from '../types'

export function DiscordPage() {
  const tr = useTr()
  const { data } = useDashboard()
  const [roles, setRoles] = useState<DiscordRole[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const discordModule = (data?.modules || []).find(m =>
    m.name.toLowerCase().includes('discord')
  )

  useEffect(() => {
    async function loadRoles() {
      try {
        const res = await api.discordRoles()
        setRoles(res.roles || [])
      } catch (e) {
        setError(tr('Failed to load Discord roles', 'Nepodařilo se načíst Discord role'))
      } finally {
        setLoading(false)
      }
    }
    loadRoles()
  }, [])

  return (
    <>
      <PageHeader
        title="Discord Integration"
        titleCz="Discord integrace"
        description="Bot connection, roles, and channel management"
        descriptionCz="Připojení bota, role a správa kanálů"
      />

      <div className="page-grid-2">
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('CONNECTION', 'PŘIPOJENÍ')}</span>
              <h2>{tr('Bot Status', 'Stav bota')}</h2>
            </div>
          </div>
          <div className="discord-status-grid">
            <div className="discord-status-card">
              <span>{tr('Module', 'Modul')}</span>
              <strong className={discordModule?.enabled ? 'text-ok' : 'text-off'}>
                {discordModule
                  ? (discordModule.enabled ? tr('Active', 'Aktivní') : tr('Disabled', 'Deaktivován'))
                  : tr('Not found', 'Nenalezen')
                }
              </strong>
            </div>
            <div className="discord-status-card">
              <span>{tr('Detail', 'Detail')}</span>
              <strong>{discordModule?.detail || '—'}</strong>
            </div>
            <div className="discord-status-card">
              <span>{tr('Roles loaded', 'Načtené role')}</span>
              <strong>{loading ? '...' : roles.length}</strong>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('ROLES', 'ROLE')}</span>
              <h2>{tr('Server Roles', 'Serverové role')}</h2>
            </div>
            <span className="role-count">{roles.length}</span>
          </div>
          {loading ? (
            <div className="loading-state">{tr('Loading roles...', 'Načítání rolí...')}</div>
          ) : error ? (
            <div className="error-state">{error}</div>
          ) : roles.length === 0 ? (
            <div className="empty-state">{tr('No roles found. Is the Discord bot connected?', 'Žádné role nenalezeny. Je Discord bot připojen?')}</div>
          ) : (
            <div className="role-list">
              {roles.map(role => (
                <div key={role.id} className="role-item">
                  <div className="role-dot" />
                  <div className="role-info">
                    <strong>{role.name}</strong>
                    <span className="role-id">{role.id}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </article>
      </div>

      <ConfigStudio
        sectionFilter="discord"
        title="Discord module settings"
        titleCz="Nastavení Discord modulu"
        eyebrow="DISCORD SETTINGS"
        eyebrowCz="NASTAVENÍ DISCORDU"
      />
    </>
  )
}
