import { useState } from 'react'
import { useTr } from '../i18n'
import type { DashboardData } from '../types'

interface Props {
  data: DashboardData | null
}

export function Hero({ data }: Props) {
  const tr = useTr()
  const [iconError, setIconError] = useState(false)
  return (
    <section className="hero">
      <div className="hero-info">
        {!iconError && (
          <img
            className="server-icon"
            src="/api/server-icon"
            alt=""
            width={64}
            height={64}
            onError={() => setIconError(true)}
          />
        )}
        <div>
          <span className="eyebrow">VOIDIUM CONTROL NEXUS</span>
          <h1>{data?.serverName || 'Voidium Server'}</h1>
          <p className="hero-copy">v{data?.version || '?'} · {data?.onlinePlayers ?? 0}/{data?.maxPlayers ?? 0} {tr('players', 'hráčů')} · {data?.uptime || '--'}</p>
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
