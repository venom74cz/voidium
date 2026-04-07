import { Link } from 'react-router-dom'
import { useTr } from '../i18n'
import { useToast } from './Toast'
import { api } from '../api'
import type { DashboardData } from '../types'

interface Props {
  data: DashboardData | null
  onRefresh: () => void
}

const FEATURE_LINKS = [
  {
    to: '/announcements',
    titleEn: 'Announcements',
    titleCz: 'Oznameni',
    noteEn: 'Scheduled announcements and manual broadcasts.',
    noteCz: 'Planovana oznameni a rucni broadcasty.',
  },
  {
    to: '/restarts',
    titleEn: 'Restarts',
    titleCz: 'Restarty',
    noteEn: 'Schedules, warning messages, and restart controls.',
    noteCz: 'Plany, warning zpravy a restart ovladani.',
  },
  {
    to: '/entity-cleaner',
    titleEn: 'Entity Cleaner',
    titleCz: 'Entity Cleaner',
    noteEn: 'Cleanup timing, protections, and manual cleanup actions.',
    noteCz: 'Casovani cisteni, ochrany a rucni cleanup akce.',
  },
  {
    to: '/stats',
    titleEn: 'Statistics',
    titleCz: 'Statistiky',
    noteEn: 'Daily report timing, channel, and statistics module settings.',
    noteCz: 'Cas denniho reportu, cilovy kanal a nastaveni statistik modulu.',
  },
] as const

export function QuickActions({ data, onRefresh }: Props) {
  const tr = useTr()
  const { toast } = useToast()
  const maintenanceMode = data?.maintenanceMode ?? false

  const sendAction = async (action: string) => {
    toast(tr('Processing...', 'Zpracovavam...'), 'info')
    try {
      const result = await api.action(action)
      toast(result.message || tr('Action completed.', 'Akce dokoncena.'), 'success')
      onRefresh()
    } catch (error) {
      toast(error instanceof Error ? error.message : tr('Action failed.', 'Akce selhala.'), 'error')
    }
  }

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('QUICK ACCESS', 'RYCHLY PRISTUP')}</span>
          <h2>{tr('Common actions', 'Nejcastejsi akce')}</h2>
        </div>
        <button className="ghost" type="button" onClick={onRefresh}>
          {tr('Refresh data', 'Obnovit data')}
        </button>
      </div>

      <div className="quick-link-grid">
        {FEATURE_LINKS.map(link => (
          <Link key={link.to} className="quick-link-card" to={link.to}>
            <strong>{tr(link.titleEn, link.titleCz)}</strong>
            <span>{tr(link.noteEn, link.noteCz)}</span>
          </Link>
        ))}
      </div>

      <div className="button-row">
        <button className="ghost" type="button" onClick={() => sendAction('reload')}>
          {tr('Reload configs', 'Reload configu')}
        </button>
        <button className="ghost" type="button" onClick={() => sendAction(maintenanceMode ? 'maintenance_off' : 'maintenance_on')}>
          {maintenanceMode ? tr('Maintenance OFF', 'Udrzba VYP') : tr('Maintenance ON', 'Udrzba ZAP')}
        </button>
      </div>
    </article>
  )
}
