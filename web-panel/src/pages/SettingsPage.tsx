import { Link } from 'react-router-dom'
import { useTr } from '../i18n'
import { ConfigStudio } from '../components/ConfigStudio'
import { useDashboard } from '../context/DashboardContext'
import { useLayoutContext } from '../components/Layout'
import { PageHeader } from '../components/PageHeader'
import { IconSun, IconMoon, IconGlobe } from '../components/Icons'

const RESOURCE_LINKS = [
  {
    href: 'https://github.com/venom74cz/voidium',
    label: 'GitHub',
    labelCz: 'GitHub',
    note: 'Repository, source code, and README',
    noteCz: 'Repozitar, zdrojovy kod a README',
  },
  {
    href: 'https://github.com/venom74cz/voidium/releases',
    label: 'Releases',
    labelCz: 'Releasy',
    note: 'Published builds and release notes',
    noteCz: 'Vydane buildy a release notes',
  },
  {
    href: 'https://github.com/venom74cz/voidium/issues',
    label: 'Issues',
    labelCz: 'Issues',
    note: 'Bug tracking and roadmap discussion',
    noteCz: 'Bug tracking a roadmap diskuze',
  },
  {
    href: 'https://github.com/venom74cz/voidium/blob/main/docs/INDEX_CZ.md',
    label: 'Wiki CZ',
    labelCz: 'Wiki CZ',
    note: 'Published Czech documentation entry point',
    noteCz: 'Cesky vstup do dokumentace',
  },
  {
    href: 'https://github.com/venom74cz/voidium/blob/main/docs/INDEX_EN.md',
    label: 'Wiki EN',
    labelCz: 'Wiki EN',
    note: 'Published English documentation entry point',
    noteCz: 'Anglicky vstup do dokumentace',
  },
  {
    href: 'https://discord.com/invite/3JYz3KWutJ',
    label: 'Discord',
    labelCz: 'Discord',
    note: 'Community support and live announcements',
    noteCz: 'Komunitni podpora a live oznameni',
  },
] as const

const FEATURE_ACCESS = [
  {
    title: 'Announcements',
    titleCz: 'Oznameni',
    note: 'Rotation settings, manual broadcasts, and message formatting are on Announcements.',
    noteCz: 'Rotace zprav, manualni broadcasty i formatovani zprav jsou na Announcements.',
    links: [
      { to: '/announcements', label: 'Open Announcements', labelCz: 'Otevrit Announcements' },
    ],
  },
  {
    title: 'Restarts',
    titleCz: 'Restarty',
    note: 'Restart schedules, warning messages, and manual restart control are on Restarts.',
    noteCz: 'Rozvrhy restartu, warning zpravy i manualni ovladani restartu jsou na Restarts.',
    links: [
      { to: '/restarts', label: 'Open Restarts', labelCz: 'Otevrit Restarts' },
    ],
  },
  {
    title: 'Entity Cleaner',
    titleCz: 'Entity Cleaner',
    note: 'Cleanup interval, protections, whitelists, and manual cleanup actions are on Entity Cleaner.',
    noteCz: 'Interval cisteni, ochrany, whitelisty i manualni akce jsou na Entity Cleaner.',
    links: [
      { to: '/entity-cleaner', label: 'Open Entity Cleaner', labelCz: 'Otevrit Entity Cleaner' },
    ],
  },
  {
    title: 'Players & TAB',
    titleCz: 'Hraci a TAB',
    note: 'Live player actions and TAB formatting settings stay on Players.',
    noteCz: 'Akce nad hraci a nastaveni TAB formatu zustavaji na Players.',
    links: [
      { to: '/players', label: 'Open Players', labelCz: 'Otevrit Players' },
    ],
  },
  {
    title: 'Discord',
    titleCz: 'Discord',
    note: 'Bot roles, bridge settings, and whitelist flow are on Discord. Ticket workflow lives on Tickets.',
    noteCz: 'Role bota, bridge nastaveni a whitelist flow jsou na Discord. Ticket workflow je na Tickets.',
    links: [
      { to: '/discord', label: 'Open Discord', labelCz: 'Otevrit Discord' },
      { to: '/tickets', label: 'Open Tickets', labelCz: 'Otevrit Tickets' },
    ],
  },
  {
    title: 'Votes',
    titleCz: 'Hlasy',
    note: 'Queue inspection, payout actions, and Votifier settings are on Votes.',
    noteCz: 'Kontrola fronty, payout akce i Votifier nastaveni jsou na Votes.',
    links: [
      { to: '/votes', label: 'Open Votes', labelCz: 'Otevrit Votes' },
    ],
  },
  {
    title: 'Ranks & Statistics',
    titleCz: 'Ranky a statistiky',
    note: 'Rank progression stays on Ranks. Statistics now focuses on daily report settings, while live metrics and history stay on the Dashboard.',
    noteCz: 'Postup ranku zustava na Ranks. Statistics se ted soustredi na nastaveni denniho reportu, zatimco zive metriky a historie jsou na Dashboardu.',
    links: [
      { to: '/ranks', label: 'Open Ranks', labelCz: 'Otevrit Ranks' },
      { to: '/stats', label: 'Open Statistics', labelCz: 'Otevrit Statistics' },
    ],
  },
  {
    title: 'Server',
    titleCz: 'Server',
    note: 'Host metrics and server.properties management are on Server.',
    noteCz: 'Metriky hostu a sprava server.properties jsou na Server.',
    links: [
      { to: '/server', label: 'Open Server', labelCz: 'Otevrit Server' },
    ],
  },
  {
    title: 'AI & Modules',
    titleCz: 'AI a moduly',
    note: 'Admin AI tools stay on AI. Current module status stays on Modules.',
    noteCz: 'Admin AI nastroje zustavaji na AI. Aktualni stav modulu zustava na Modules.',
    links: [
      { to: '/ai', label: 'Open AI', labelCz: 'Otevrit AI' },
      { to: '/modules', label: 'Open Modules', labelCz: 'Otevrit Modules' },
    ],
  },
] as const

export function SettingsPage() {
  const tr = useTr()
  const { data } = useDashboard()
  const { locale, setLocale, theme, toggleTheme } = useLayoutContext()

  return (
    <>
      <PageHeader
        title="Settings"
        titleCz="Nastaveni"
        description="Panel preferences, security, and access configuration"
        descriptionCz="Predvolby panelu, bezpecnost a konfigurace pristupu"
      />

      <div className="page-grid-2">
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('APPEARANCE', 'VZHLED')}</span>
              <h2>{tr('Theme & Language', 'Tema a jazyk')}</h2>
            </div>
          </div>
          <div className="settings-grid">
            <div className="setting-item">
              <div className="setting-info">
                {theme === 'dark' ? <IconMoon size={20} /> : <IconSun size={20} />}
                <div>
                  <strong>{tr('Theme', 'Tema')}</strong>
                  <span>{theme === 'dark' ? tr('Dark mode', 'Tmavy rezim') : tr('Light mode', 'Svetly rezim')}</span>
                </div>
              </div>
              <button className="ghost" type="button" onClick={toggleTheme}>
                {theme === 'dark' ? tr('Switch to Light', 'Prepnout na svetly') : tr('Switch to Dark', 'Prepnout na tmavy')}
              </button>
            </div>
            <div className="setting-item">
              <div className="setting-info">
                <IconGlobe size={20} />
                <div>
                  <strong>{tr('Language', 'Jazyk')}</strong>
                  <span>{tr('Interface language', 'Jazyk rozhrani')}</span>
                </div>
              </div>
              <select value={locale} onChange={event => setLocale(event.target.value as 'en' | 'cz')}>
                <option value="en">English</option>
                <option value="cz">Cestina</option>
              </select>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('SECURITY', 'BEZPECNOST')}</span>
              <h2>{tr('Access Profile', 'Pristupovy profil')}</h2>
            </div>
          </div>
          <div className="security-grid">
            <div>
              <span>{tr('Session model', 'Model sezeni')}</span>
              <strong>{tr('HTTP-only cookie session', 'HTTP-only cookie sezeni')}</strong>
            </div>
            <div>
              <span>{tr('One-time access', 'Jednorazovy pristup')}</span>
              <strong>{tr('Via /voidium web token links', 'Pres token linky /voidium web')}</strong>
            </div>
            <div>
              <span>{tr('Persistent access', 'Trvaly pristup')}</span>
              <strong>{data?.publicAccessUrl || '-'}</strong>
            </div>
            <div>
              <span>{tr('Base URL', 'Zakladni URL')}</span>
              <strong>{data?.baseUrl || '-'}</strong>
            </div>
          </div>
        </article>
      </div>

      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('SERVER', 'SERVER')}</span>
            <h2>{tr('Server Information', 'Informace o serveru')}</h2>
          </div>
        </div>
        <div className="security-grid">
          <div>
            <span>{tr('Server name', 'Nazev serveru')}</span>
            <strong>{data?.serverName || '-'}</strong>
          </div>
          <div>
            <span>{tr('Version', 'Verze')}</span>
            <strong>v{data?.version || '?'}</strong>
          </div>
          <div>
            <span>{tr('Uptime', 'Doba behu')}</span>
            <strong>{data?.uptime || '-'}</strong>
          </div>
          <div>
            <span>{tr('Next restart', 'Dalsi restart')}</span>
            <strong>{data?.nextRestart || '-'}</strong>
          </div>
        </div>
      </article>

      <div className="page-grid-2">
        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('FEATURE MAP', 'MAPA FUNKCI')}</span>
              <h2>{tr('Open the right page for each module', 'Otevri spravnou stranku pro kazdy modul')}</h2>
            </div>
          </div>
          <div className="feature-access-grid">
            {FEATURE_ACCESS.map(feature => (
              <div key={feature.title} className="feature-access-card">
                <div className="feature-access-copy">
                  <strong>{tr(feature.title, feature.titleCz)}</strong>
                  <p>{tr(feature.note, feature.noteCz)}</p>
                </div>
                <div className="feature-access-links">
                  {feature.links.map(link => (
                    <Link key={`${feature.title}-${link.to}`} className="feature-access-link" to={link.to}>
                      {tr(link.label, link.labelCz)}
                    </Link>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">{tr('RESOURCES', 'ZDROJE')}</span>
              <h2>{tr('Wiki, GitHub, releases, and support', 'Wiki, GitHub, releasy a podpora')}</h2>
            </div>
          </div>
          <div className="resource-grid">
            {RESOURCE_LINKS.map(resource => (
              <a
                key={resource.href}
                className="resource-card"
                href={resource.href}
                target="_blank"
                rel="noreferrer"
              >
                <span className="resource-label">{tr(resource.label, resource.labelCz)}</span>
                <span className="resource-note">{tr(resource.note, resource.noteCz)}</span>
              </a>
            ))}
          </div>
        </article>
      </div>

      <ConfigStudio
        sectionFilter="general"
        title="Core module settings"
        titleCz="Nastaveni hlavniho modulu"
        eyebrow="CORE SETTINGS"
        eyebrowCz="CORE SETTINGS"
      />
      <ConfigStudio
        sectionFilter="web"
        title="Web panel settings"
        titleCz="Nastaveni web panelu"
        eyebrow="WEB PANEL"
        eyebrowCz="WEB PANEL"
      />
    </>
  )
}
