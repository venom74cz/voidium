import { useTr } from '../i18n'
import type { DashboardData } from '../types'
import { api } from '../api'

interface Props {
  data: DashboardData | null
  locale: 'en' | 'cz'
  setLocale: (locale: 'en' | 'cz') => void
  theme: 'dark' | 'light'
  toggleTheme: () => void
}

export function SecurityPanel({ data, locale, setLocale, theme, toggleTheme }: Props) {
  const tr = useTr()

  const handleLogout = async () => {
    await api.logout()
    window.location.reload()
  }

  return (
    <article className="panel">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('SECURITY', 'BEZPEČNOST')}</span>
          <h2>{tr('Access profile', 'Přístupový profil')}</h2>
        </div>
        <button className="ghost" type="button" onClick={handleLogout}>{tr('Logout', 'Odhlásit')}</button>
      </div>
      <div className="security-grid">
        <div><span>{tr('Session model', 'Model sezení')}</span><strong>{tr('HTTP-only cookie session', 'HTTP-only cookie sezení')}</strong></div>
        <div><span>{tr('One-time access', 'Jednorázový přístup')}</span><strong>{tr('Via /voidium web token links', 'Přes token linky /voidium web')}</strong></div>
        <div><span>{tr('Persistent access', 'Trvalý přístup')}</span><strong>{data?.publicAccessUrl || ''}</strong></div>
        <div><span>{tr('Base URL', 'Základní URL')}</span><strong>{data?.baseUrl || ''}</strong></div>
      </div>
      <div className="preference-row">
        <div className="preference-item">
          <span>{tr('Language', 'Jazyk')}</span>
          <select value={locale} onChange={e => setLocale(e.target.value as 'en' | 'cz')}>
            <option value="en">English</option>
            <option value="cz">Čeština</option>
          </select>
        </div>
        <div className="preference-item">
          <span>{tr('Theme', 'Téma')}</span>
          <button className="ghost" type="button" onClick={toggleTheme}>
            {theme === 'dark' ? tr('☀ Light mode', '☀ Světlý režim') : tr('🌙 Dark mode', '🌙 Tmavý režim')}
          </button>
        </div>
      </div>
    </article>
  )
}
