import { NavLink } from 'react-router-dom'
import { useTr } from '../i18n'
import { useDashboard } from '../context/DashboardContext'
import {
  IconDashboard,
  IconPlayers,
  IconDiscord,
  IconTrophy,
  IconClock,
  IconVote,
  IconTicket,
  IconModules,
  IconChart,
  IconRadio,
  IconSliders,
  IconTerminal,
  IconSparkles,
  IconGear,
  IconX,
} from './Icons'

interface Props {
  open: boolean
  onClose: () => void
}

interface NavItem {
  to: string
  icon: React.ComponentType<{ size?: number }>
  labelEn: string
  labelCz: string
}

const mainNav: NavItem[] = [
  { to: '/', icon: IconDashboard, labelEn: 'Dashboard', labelCz: 'Prehled' },
  { to: '/players', icon: IconPlayers, labelEn: 'Players', labelCz: 'Hraci' },
]

const featureNav: NavItem[] = [
  { to: '/announcements', icon: IconRadio, labelEn: 'Announcements', labelCz: 'Oznameni' },
  { to: '/restarts', icon: IconClock, labelEn: 'Restarts', labelCz: 'Restarty' },
  { to: '/discord', icon: IconDiscord, labelEn: 'Discord', labelCz: 'Discord' },
  { to: '/ranks', icon: IconTrophy, labelEn: 'Ranks', labelCz: 'Ranky' },
  { to: '/stats', icon: IconChart, labelEn: 'Statistics', labelCz: 'Statistiky' },
  { to: '/votes', icon: IconVote, labelEn: 'Votes', labelCz: 'Hlasy' },
  { to: '/tickets', icon: IconTicket, labelEn: 'Tickets', labelCz: 'Tikety' },
  { to: '/entity-cleaner', icon: IconSliders, labelEn: 'Entity Cleaner', labelCz: 'Entity Cleaner' },
]

const systemNav: NavItem[] = [
  { to: '/modules', icon: IconModules, labelEn: 'Modules', labelCz: 'Moduly' },
  { to: '/server', icon: IconGear, labelEn: 'Server', labelCz: 'Server' },
  { to: '/feeds', icon: IconRadio, labelEn: 'Live Feeds', labelCz: 'Zive kanaly' },
  { to: '/console', icon: IconTerminal, labelEn: 'Console', labelCz: 'Konzole' },
  { to: '/ai', icon: IconSparkles, labelEn: 'AI Assistant', labelCz: 'AI Asistent' },
]

export function Sidebar({ open, onClose }: Props) {
  const tr = useTr()
  const { data } = useDashboard()

  const ticketBadge = data?.tickets?.open || 0
  const voteBadge = data?.voteQueue?.total || 0
  const playerBadge = data?.onlinePlayers || 0

  function getBadge(to: string): number | undefined {
    if (to === '/players' && playerBadge > 0) return playerBadge
    if (to === '/tickets' && ticketBadge > 0) return ticketBadge
    if (to === '/votes' && voteBadge > 0) return voteBadge
    return undefined
  }

  function renderGroup(label: string, labelCz: string, items: NavItem[]) {
    return (
      <div className="sidebar-group">
        <span className="sidebar-label">{tr(label, labelCz)}</span>
        {items.map(item => {
          const badge = getBadge(item.to)
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
              onClick={onClose}
            >
              <item.icon size={18} />
              <span>{tr(item.labelEn, item.labelCz)}</span>
              {badge !== undefined && <span className="sidebar-badge">{badge}</span>}
            </NavLink>
          )
        })}
      </div>
    )
  }

  return (
    <>
      {open && <div className="sidebar-overlay" onClick={onClose} />}
      <aside className={`sidebar${open ? ' open' : ''}`}>
        <div className="sidebar-header">
          <div className="sidebar-logo">
            <svg width="36" height="36" viewBox="0 0 100 100" fill="none">
              <defs>
                <linearGradient id="voidium-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#a855f7" />
                  <stop offset="100%" stopColor="#6366f1" />
                </linearGradient>
              </defs>
              <path d="M50 8L90 78H10L50 8Z" fill="url(#voidium-grad)" opacity="0.9" />
              <path d="M50 28L72 68H28L50 28Z" fill="url(#voidium-grad)" opacity="0.5" />
            </svg>
            <div className="sidebar-brand">
              <strong>VOIDIUM</strong>
              <span>Web Panel</span>
            </div>
          </div>
          <button className="sidebar-close" type="button" onClick={onClose} aria-label="Close menu">
            <IconX size={20} />
          </button>
        </div>

        <div className="sidebar-status">
          <div className={`status-dot${data ? ' online' : ''}`} />
          <span>{data ? tr('Server Online', 'Server online') : tr('Connecting...', 'Pripojovani...')}</span>
          {data && <span className="sidebar-tps">{(data.tps ?? 20).toFixed(1)} TPS</span>}
        </div>

        <nav className="sidebar-nav">
          {renderGroup('MAIN', 'HLAVNI', mainNav)}
          {renderGroup('FEATURES', 'FUNKCE', featureNav)}
          {renderGroup('SYSTEM', 'SYSTEM', systemNav)}
        </nav>

        <div className="sidebar-footer">
          <NavLink
            to="/settings"
            className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
            onClick={onClose}
          >
            <IconGear size={18} />
            <span>{tr('Settings', 'Nastaveni')}</span>
          </NavLink>
        </div>
      </aside>
    </>
  )
}
