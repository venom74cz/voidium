import { useState, useEffect } from 'react'
import { useTr } from '../i18n'
import { ConfigStudio } from '../components/ConfigStudio'
import { PageHeader } from '../components/PageHeader'
import { api } from '../api'
import type { ConfigSchema, ConfigValues } from '../types'

export function RanksPage() {
  const tr = useTr()
  const [schema, setSchema] = useState<ConfigSchema | null>(null)
  const [values, setValues] = useState<ConfigValues | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const [s, v] = await Promise.all([api.configSchema(), api.configValues()])
        setSchema(s)
        setValues(v)
      } catch {
        // config not available
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const rankSections = schema
    ? Object.entries(schema).filter(([, section]) =>
        section.fields.some(f =>
          f.type === 'rank-list' ||
          f.key.toLowerCase().includes('rank') ||
          f.label.toLowerCase().includes('rank')
        )
      )
    : []

  const rankFields = rankSections.flatMap(([sectionKey, section]) =>
    section.fields
      .filter(f => f.type === 'rank-list' || f.key.toLowerCase().includes('rank'))
      .map(f => ({
        sectionKey,
        sectionLabel: section.label,
        field: f,
        value: values?.[sectionKey]?.[f.key],
      }))
  )

  type RankEntry = {
    type: string
    value: string
    hours: number
    customConditions: unknown[]
  }

  function parseRankList(value: unknown): RankEntry[] {
    try {
      const parsed = typeof value === 'string' ? JSON.parse(value) : value
      if (!Array.isArray(parsed)) return []
      return parsed.map(rank => ({
        type: String((rank as Record<string, unknown>)?.type || 'PREFIX'),
        value: String((rank as Record<string, unknown>)?.value || ''),
        hours: Number((rank as Record<string, unknown>)?.hours || 0),
        customConditions: Array.isArray((rank as Record<string, unknown>)?.customConditions)
          ? ((rank as Record<string, unknown>).customConditions as unknown[])
          : [],
      }))
    } catch {
      return []
    }
  }

  return (
    <>
      <PageHeader
        title="Ranks & Progression"
        titleCz="Ranky a postup"
        description="Player rank hierarchy based on playtime milestones"
        descriptionCz="Hierarchie ranků hráčů podle odehraných hodin"
      />

      {loading ? (
        <div className="panel"><div className="loading-state">{tr('Loading rank configuration...', 'Načítání konfigurace ranků...')}</div></div>
      ) : rankFields.length === 0 ? (
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('RANKS', 'RANKY')}</span>
              <h2>{tr('No rank configuration found', 'Konfigurace ranků nenalezena')}</h2>
            </div>
          </div>
          <p style={{ color: 'var(--muted)' }}>
            {tr(
              'Rank fields will appear here once configured in the Voidium config.',
              'Pole ranků se zde zobrazí po konfiguraci v nastavení Voidium.'
            )}
          </p>
        </article>
      ) : (
        rankFields.map(({ sectionKey, sectionLabel, field, value }) => {
          const ranks = parseRankList(value)
          return (
            <article key={`${sectionKey}-${field.key}`} className="panel">
              <div className="panel-head">
                <div>
                  <span className="eyebrow">{sectionLabel.toUpperCase()}</span>
                  <h2>{field.label}</h2>
                </div>
                <span className="role-count">{ranks.length} {tr('ranks', 'ranků')}</span>
              </div>
              {field.description && (
                <p className="field-description">{field.description}</p>
              )}
              {ranks.length > 0 ? (
                <div className="rank-progression">
                  {ranks.map((rank, i) => (
                    <div key={i} className="rank-card">
                      <div className="rank-card-index">#{i + 1}</div>
                      <div className="rank-card-info">
                        <strong className="rank-card-name">
                          {rank.type === 'SUFFIX' ? tr('Suffix rank', 'Suffix rank') : tr('Prefix rank', 'Prefix rank')}
                        </strong>
                        {rank.value && (
                          <span className="rank-card-prefix">{rank.value}</span>
                        )}
                        {rank.hours > 0 && (
                          <span className="rank-card-hours">
                            {rank.hours}h {tr('required', 'potřeba')}
                          </span>
                        )}
                        {rank.customConditions.length > 0 && (
                          <span className="rank-card-hours">
                            {rank.customConditions.length} {tr('extra conditions', 'extra podmínek')}
                          </span>
                        )}
                      </div>
                      {i < ranks.length - 1 && (
                        <div className="rank-card-arrow">
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M5 12h14M12 5l7 7-7 7" />
                          </svg>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-state">{tr('No ranks defined yet.', 'Zatím nejsou definovány žádné ranky.')}</div>
              )}
            </article>
          )
        })
      )}

      <ConfigStudio
        sectionFilter="ranks"
        title="Rank progression settings"
        titleCz="Nastavení rank postupu"
        eyebrow="RANK SETTINGS"
        eyebrowCz="NASTAVENÍ RANKŮ"
      />
    </>
  )
}
