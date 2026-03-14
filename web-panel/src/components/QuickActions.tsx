import { useState } from 'react'
import { useTr } from '../i18n'
import { useToast } from './Toast'
import { api } from '../api'
import type { DashboardData, DimensionHeatmap } from '../types'

interface Props {
  data: DashboardData | null
  onRefresh: () => void
}

export function QuickActions({ data, onRefresh }: Props) {
  const tr = useTr()
  const { toast } = useToast()
  const [heatmap, setHeatmap] = useState<DimensionHeatmap | null>(null)
  const [showHeatmap, setShowHeatmap] = useState(false)

  const sendAction = async (action: string, extra: Record<string, string> = {}) => {
    toast(tr('Processing...', 'Zpracovávám...'), 'info')
    try {
      const result = await api.action(action, extra)
      toast(result.message || tr('Action completed.', 'Akce dokončena.'), 'success')
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : tr('Action failed.', 'Akce selhala.'), 'error')
    }
  }

  const handleAnnounce = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    const message = form.get('message') as string
    await sendAction('announce', { message })
    e.currentTarget.reset()
  }

  const handleDimHeatmap = async () => {
    toast(tr('Loading dimension heatmap...', 'Načítání dimension heatmapy...'), 'info')
    try {
      const result = await api.actionJson('entitycleaner_preview_dimensions')
      if (result.dimensions) {
        setHeatmap(result.dimensions)
        setShowHeatmap(true)
        toast(tr('Dimension heatmap loaded.', 'Dimension heatmapa načtena.'), 'success')
      } else {
        toast(result.message || tr('Failed to load heatmap.', 'Nepodařilo se načíst heatmapu.'), 'error')
      }
    } catch (e) {
      toast(e instanceof Error ? e.message : tr('Failed to load heatmap.', 'Nepodařilo se načíst heatmapu.'), 'error')
    }
  }

  const maintenanceMode = data?.maintenanceMode ?? false

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('OPERATIONS', 'OPERACE')}</span>
          <h2>{tr('Quick actions', 'Rychlé akce')}</h2>
        </div>
        <button className="ghost" onClick={onRefresh}>{tr('Refresh data', 'Obnovit data')}</button>
      </div>
      <div className="action-stack">
        <form className="action-row" onSubmit={handleAnnounce}>
          <label>
            <span>{tr('Broadcast message', 'Broadcast zpráva')}</span>
            <input type="text" name="message" placeholder={tr('Type a live announcement for all players', 'Napiš živé oznámení pro všechny hráče')} required />
          </label>
          <button type="submit">{tr('Broadcast', 'Odeslat')}</button>
        </form>
        <div className="button-row">
          <button className="danger" onClick={() => { if (confirm(tr('Schedule a restart in 5 minutes?', 'Naplánovat restart za 5 minut?'))) sendAction('restart') }}>
            {tr('Restart in 5 min', 'Restart za 5 min')}
          </button>
          <button className="ghost" onClick={() => sendAction('reload')}>
            {tr('Reload configs', 'Reload configů')}
          </button>
        </div>
        <div className="button-row">
          <button className="ghost" onClick={() => sendAction('entitycleaner_preview')}>
            {tr('EntityCleaner preview', 'Náhled EntityCleaneru')}
          </button>
          <button className="ghost" onClick={() => { if (confirm(tr('Run full entity cleanup now?', 'Opravdu teď spustit plný entity cleanup?'))) sendAction('entitycleaner_all') }}>
            {tr('Run full cleanup', 'Spustit plný cleanup')}
          </button>
        </div>
        <div className="button-row">
          <button className="ghost" onClick={() => sendAction('entitycleaner_items')}>{tr('Clear items', 'Smazat itemy')}</button>
          <button className="ghost" onClick={() => sendAction('entitycleaner_mobs')}>{tr('Clear mobs', 'Smazat moby')}</button>
          <button className="ghost" onClick={() => sendAction('entitycleaner_xp')}>{tr('Clear XP', 'Smazat XP')}</button>
          <button className="ghost" onClick={() => sendAction('entitycleaner_arrows')}>{tr('Clear arrows', 'Smazat šípy')}</button>
        </div>
        <div className="button-row">
          <button className="ghost" type="button" onClick={handleDimHeatmap}>{tr('Dimension heatmap', 'Dimension heatmapa')}</button>
          <button className="ghost" type="button" onClick={() => sendAction(maintenanceMode ? 'maintenance_off' : 'maintenance_on')}>
            {maintenanceMode ? tr('Maintenance OFF', 'Údržba VYP') : tr('Maintenance ON', 'Údržba ZAP')}
          </button>
        </div>
      </div>
      {showHeatmap && heatmap && <DimHeatmapView heatmap={heatmap} />}
    </article>
  )
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
