import { formatDurationCompact } from '../utils'
import type { Timer } from '../types'

interface Props {
  timers: Timer[]
}

export function Timeline({ timers }: Props) {
  if (!timers.length) return null

  const toneClass: Record<string, string> = { danger: 'danger', mint: 'mint', accent: 'cyan' }

  return (
    <section className="panel timeline-section" style={{ padding: 22, marginBottom: 20 }}>
      <div className="panel-head">
        <div>
          <span className="eyebrow">SCHEDULE</span>
          <h2>Task timeline</h2>
        </div>
      </div>
      <div className="timeline-container">
        {timers.map(timer => {
          const total = Number(timer.totalSeconds || 0)
          const remaining = Math.max(0, Number(timer.remainingSeconds || 0))
          const pct = total > 0
            ? Math.max(0, Math.min(100, ((total - remaining) / total) * 100))
            : 100
          const cls = toneClass[timer.tone] || 'cyan'
          return (
            <div className="timeline-row" key={timer.id}>
              <div className="timeline-label">{timer.title || ''}</div>
              <div className="timeline-bar-wrap">
                <div className={`timeline-bar ${cls}`} style={{ width: `${pct}%` }} />
              </div>
              <div className="timeline-time">{formatDurationCompact(remaining)}</div>
            </div>
          )
        })}
      </div>
    </section>
  )
}
