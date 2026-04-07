import { useEffect, useState } from 'react'
import { useTr } from '../i18n'
import type { DashboardData } from '../types'

interface Props {
  data: DashboardData | null
}

const VOIDIUM_FALLBACK_ICON = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2264%22 height=%2264%22 viewBox=%220 0 64 64%22%3E%3Cdefs%3E%3ClinearGradient id=%22g%22 x1=%220%22 x2=%221%22 y1=%220%22 y2=%221%22%3E%3Cstop stop-color=%22%23a855f7%22/%3E%3Cstop offset=%221%22 stop-color=%22%236366f1%22/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect width=%2264%22 height=%2264%22 rx=%2214%22 fill=%22%230f0a1d%22/%3E%3Crect x=%224%22 y=%224%22 width=%2256%22 height=%2256%22 rx=%2210%22 fill=%22url(%23g)%22 opacity=%220.22%22/%3E%3Cpath d=%22M20 18h24v8H20zm0 10h10v18H20zm14 0h10v18H34z%22 fill=%22%23f0e7ff%22/%3E%3C/svg%3E'

export function Hero({ data }: Props) {
  const tr = useTr()
  const [iconError, setIconError] = useState(false)
  const preferredIcon = data?.serverIconUrl || null
  const iconSrc = !iconError && preferredIcon ? preferredIcon : VOIDIUM_FALLBACK_ICON

  useEffect(() => {
    setIconError(false)
  }, [preferredIcon])

  return (
    <section className="hero">
      <div className="hero-info">
        <img
          className="server-icon"
          src={iconSrc}
          alt={preferredIcon && !iconError ? tr('Server icon', 'Ikona serveru') : 'VOIDIUM'}
          width={64}
          height={64}
          onError={() => setIconError(true)}
        />
        <div className="hero-copy-wrap">
          <span className="eyebrow">VOIDIUM CONTROL NEXUS</span>
          <h1>{data?.serverName || 'Voidium Server'}</h1>
          <p className="hero-copy">v{data?.version || '?'} · {data?.onlinePlayers ?? 0}/{data?.maxPlayers ?? 0} {tr('players', 'hráčů')} · {data?.uptime || '--'}</p>
          {data?.updateAvailable && data.latestVersion && (
            <a
              className="hero-update-badge"
              href={data.updateUrl || 'https://github.com/venom74cz/voidium/releases/latest'}
              target="_blank"
              rel="noreferrer"
            >
              {tr(`Update available: v${data.latestVersion}`, `Dostupná novější verze: v${data.latestVersion}`)}
            </a>
          )}
        </div>
      </div>
      <div className="hero-panel">
        <div className="hero-badge">{tr('Live', 'Živě')}</div>
        <div className="hero-stat"><span>TPS</span><strong>{(data?.tps ?? 20).toFixed(1)}</strong></div>
        <div className="hero-stat"><span>MSPT</span><strong>{(data?.mspt ?? 0).toFixed(1)}</strong></div>
        <div className="hero-stat"><span>{tr('Memory', 'Paměť')}</span><strong>{data?.memoryUsedMb ?? 0}/{data?.memoryMaxMb ?? 0} MB</strong></div>
      </div>
    </section>
  )
}
