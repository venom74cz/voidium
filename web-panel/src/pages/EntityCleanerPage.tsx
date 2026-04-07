import { useEffect, useState } from 'react'
import { api } from '../api'
import { useTr } from '../i18n'
import { useDashboard } from '../context/DashboardContext'
import { useToast } from '../components/Toast'
import { PageHeader } from '../components/PageHeader'
import { ConfigStudio } from '../components/ConfigStudio'
import { formatDurationCompact } from '../utils'
import type { ConfigValues, DimensionHeatmap, Module } from '../types'

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

function DimHeatmapView({ heatmap }: { heatmap: DimensionHeatmap }) {
  const dimNames: Record<string, string> = { overworld: 'Overworld', the_nether: 'Nether', the_end: 'The End' }
  return (
    <div className="dim-heatmap" style={{ display: 'block' }}>
      <div className="dim-heatmap-grid">
        {Object.entries(heatmap).map(([dim, counts]) => {
          const name = dimNames[dim] || dim
          const total = counts.total || 0
          const heat = total > 200 ? 'hot' : total > 50 ? 'warm' : 'cool'
          return (
            <div className={`dim-cell ${heat}`} key={dim}>
              <strong>{name}</strong>
              <span>Items: {counts.items}</span>
              <span>Mobs: {counts.mobs}</span>
              <span>XP: {counts.xpOrbs}</span>
              <span>Arrows: {counts.arrows}</span>
              <span className="total">Total: {total}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export function EntityCleanerPage() {
  const tr = useTr()
  const { data, timers, fetchDashboard } = useDashboard()
  const { toast } = useToast()
  const [config, setConfig] = useState<ConfigValues | null>(null)
  const [loading, setLoading] = useState(true)
  const [heatmap, setHeatmap] = useState<DimensionHeatmap | null>(null)
  const [running, setRunning] = useState<string | null>(null)

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

  const section = config?.entitycleaner || {}
  const module = findModule(data?.modules || [], ['entity', 'cleaner'])
  const timer = timers.find(entry => entry.id === 'entitycleaner')
  const warningTimes = listValue(section.warningTimes)

  async function runAction(action: string) {
    setRunning(action)
    toast(tr('Processing action...', 'Zpracovavam akci...'), 'info')
    try {
      const result = await api.action(action)
      toast(result.message || tr('Action completed.', 'Akce dokoncena.'), 'success')
      fetchDashboard()
    } catch (error) {
      toast(error instanceof Error ? error.message : tr('Action failed.', 'Akce selhala.'), 'error')
    } finally {
      setRunning(null)
    }
  }

  async function loadHeatmap() {
    setRunning('entitycleaner_preview_dimensions')
    toast(tr('Loading dimension overview...', 'Nacitam prehled dimenzi...'), 'info')
    try {
      const result = await api.actionJson('entitycleaner_preview_dimensions')
      setHeatmap(result.dimensions || null)
      toast(result.message || tr('Overview loaded.', 'Prehled nacten.'), 'success')
      fetchDashboard()
    } catch (error) {
      toast(error instanceof Error ? error.message : tr('Loading failed.', 'Nacteni selhalo.'), 'error')
    } finally {
      setRunning(null)
    }
  }

  return (
    <>
      <PageHeader
        title="Entity Cleaner"
        titleCz="Entity Cleaner"
        description="Cleanup timing, protections, whitelists, and manual cleanup actions"
        descriptionCz="Casovani cisteni, ochrany, whitelisty a rucni cleanup akce"
      />

      <div className="page-grid-2">
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('OVERVIEW', 'PREHLED')}</span>
              <h2>{tr('Cleanup Overview', 'Prehled cisteni')}</h2>
            </div>
          </div>
          <div className="overview-grid">
            <div className="overview-card">
              <span>{tr('Module', 'Modul')}</span>
              <strong className={Boolean(section.enabled) ? 'text-ok' : 'text-off'}>
                {Boolean(section.enabled) ? tr('Active', 'Aktivni') : tr('Disabled', 'Vypnuto')}
              </strong>
              <small>{module?.detail || tr('Cleanup service is not available.', 'Cleanup sluzba neni dostupna.')}</small>
            </div>
            <div className="overview-card">
              <span>{tr('Interval', 'Interval')}</span>
              <strong>{Number(section.cleanupIntervalSeconds || 0)}s</strong>
              <small>
                {timer
                  ? tr('Next cleanup in ', 'Dalsi cleanup za ') + formatDurationCompact(timer.remainingSeconds)
                  : tr('No active cleanup timer is available.', 'Neni dostupny zadny aktivni cleanup timer.')}
              </small>
            </div>
            <div className="overview-card">
              <span>{tr('Warnings', 'Warningy')}</span>
              <strong>{loading ? '...' : warningTimes.length}</strong>
              <small>{tr('Configured warning checkpoints before cleanup.', 'Nastavene warning checkpointy pred cistenim.')}</small>
            </div>
            <div className="overview-card">
              <span>{tr('Boss protection', 'Ochrana bossu')}</span>
              <strong>{Boolean(section.protectBosses) ? tr('Enabled', 'Zapnuta') : tr('Disabled', 'Vypnuta')}</strong>
              <small>{tr('Named entities and whitelist rules stay below in settings.', 'Pojmenovane entity a whitelist pravidla zustavaji v nastaveni niz.')}</small>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('ACTIONS', 'AKCE')}</span>
              <h2>{tr('Manual Cleanup', 'Rucni cleanup')}</h2>
            </div>
          </div>
          <div className="action-stack">
            <div className="button-row">
              <button className="ghost" type="button" onClick={() => runAction('entitycleaner_preview')} disabled={running !== null}>
                {tr('Preview cleanup', 'Nahled cleanupu')}
              </button>
              <button className="danger" type="button" onClick={() => runAction('entitycleaner_all')} disabled={running !== null}>
                {running === 'entitycleaner_all' ? tr('Running...', 'Spoustim...') : tr('Run full cleanup', 'Spustit plny cleanup')}
              </button>
            </div>
            <div className="button-row">
              <button className="ghost" type="button" onClick={() => runAction('entitycleaner_items')} disabled={running !== null}>
                {tr('Clear items', 'Smazat itemy')}
              </button>
              <button className="ghost" type="button" onClick={() => runAction('entitycleaner_mobs')} disabled={running !== null}>
                {tr('Clear mobs', 'Smazat moby')}
              </button>
              <button className="ghost" type="button" onClick={() => runAction('entitycleaner_xp')} disabled={running !== null}>
                {tr('Clear XP', 'Smazat XP')}
              </button>
              <button className="ghost" type="button" onClick={() => runAction('entitycleaner_arrows')} disabled={running !== null}>
                {tr('Clear arrows', 'Smazat sipy')}
              </button>
            </div>
            <div className="button-row">
              <button className="ghost" type="button" onClick={loadHeatmap} disabled={running !== null}>
                {running === 'entitycleaner_preview_dimensions' ? tr('Loading...', 'Nacitam...') : tr('Dimension overview', 'Prehled dimenzi')}
              </button>
              <button className="ghost" type="button" onClick={() => fetchDashboard()}>
                {tr('Refresh data', 'Obnovit data')}
              </button>
            </div>
          </div>
        </article>
      </div>

      {heatmap && (
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('DIMENSIONS', 'DIMENZE')}</span>
              <h2>{tr('Entity Overview by Dimension', 'Prehled entit podle dimenze')}</h2>
            </div>
          </div>
          <DimHeatmapView heatmap={heatmap} />
        </article>
      )}

      <ConfigStudio
        sectionFilter="entitycleaner"
        title="Entity Cleaner settings"
        titleCz="Nastaveni Entity Cleaneru"
        eyebrow="ENTITY CLEANER"
        eyebrowCz="ENTITY CLEANER"
      />
    </>
  )
}
