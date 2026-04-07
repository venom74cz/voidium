import { useEffect, useState } from 'react'
import { api } from '../api'
import { useTr } from '../i18n'
import { ConfigStudio } from '../components/ConfigStudio'
import { PageHeader } from '../components/PageHeader'

function stringValue(value: unknown, fallback: string) {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function boolValue(value: unknown) {
  return value === true || value === 'true'
}

export function StatsPage() {
  const tr = useTr()
  const [statsConfig, setStatsConfig] = useState<Record<string, unknown>>({})

  useEffect(() => {
    let active = true

    api.configValues()
      .then(values => {
        if (active) {
          setStatsConfig((values.stats || {}) as Record<string, unknown>)
        }
      })
      .catch(() => {})

    return () => {
      active = false
    }
  }, [])

  const reportsEnabled = boolValue(statsConfig.enableStats)
  const reportChannel = stringValue(statsConfig.reportChannelId, tr('Not set', 'Nenastaveno'))
  const reportTime = stringValue(statsConfig.reportTime, '08:00:00')
  const reportTitle = stringValue(statsConfig.reportTitle, tr('No title configured', 'Titulek neni nastaven'))
  const reportFooter = stringValue(statsConfig.reportFooter, tr('No footer configured', 'Paticka neni nastavena'))

  return (
    <>
      <PageHeader
        title="Statistics"
        titleCz="Statistiky"
        description="Daily Discord report settings for the statistics module"
        descriptionCz="Nastaveni denniho Discord reportu pro statistikovy modul"
      />

      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('DAILY REPORTS', 'DENNI REPORTY')}</span>
            <h2>{tr('Statistics module settings', 'Nastaveni statistik modulu')}</h2>
          </div>
        </div>
        <p className="feature-note">
          {tr(
            'Live performance cards and history charts are now on the Dashboard. This page stays focused on daily report configuration and the stats module settings stored in config/voidium/stats.json.',
            'Zive vykonnostni karty a historicke grafy jsou ted na Dashboardu. Tahle stranka zustava zamerena na konfiguraci denniho reportu a nastaveni stats modulu v config/voidium/stats.json.'
          )}
        </p>
        <div className="overview-grid">
          <div className="overview-card">
            <span>{tr('Reports', 'Reporty')}</span>
            <strong>{reportsEnabled ? tr('Enabled', 'Zapnuto') : tr('Disabled', 'Vypnuto')}</strong>
            <small>{tr('Matches stats.enableStats in config.', 'Odpovida stats.enableStats v configu.')}</small>
          </div>
          <div className="overview-card">
            <span>{tr('Channel', 'Kanal')}</span>
            <strong>{reportChannel}</strong>
            <small>{tr('Discord channel for the daily summary.', 'Discord kanal pro denni souhrn.')}</small>
          </div>
          <div className="overview-card">
            <span>{tr('Report time', 'Cas reportu')}</span>
            <strong>{reportTime}</strong>
            <small>{tr('Runs once per day at the configured time.', 'Bezi jednou denne v nastaveny cas.')}</small>
          </div>
          <div className="overview-card">
            <span>{tr('Copy', 'Text reportu')}</span>
            <strong>{reportTitle}</strong>
            <small>{reportFooter}</small>
          </div>
        </div>
      </article>

      <ConfigStudio
        sectionFilter="stats"
        title="Statistics module settings"
        titleCz="Nastaveni statistik modulu"
        eyebrow="STATS SETTINGS"
        eyebrowCz="NASTAVENI STATISTIK"
      />
    </>
  )
}
