import { useTr } from '../i18n'
import { formatDurationCompact, formatTimerVerbose } from '../utils'
import type { Timer } from '../types'

interface Props {
  timers: Timer[]
}

export function TimerGrid({ timers }: Props) {
  const tr = useTr()
  if (!timers.length) return null

  return (
    <section className="grid timers">
      {timers.map(timer => {
        const total = Number(timer.totalSeconds || 0)
        const remaining = Math.max(0, Number(timer.remainingSeconds || 0))
        const progress = total > 0
          ? Math.max(0, Math.min(100, ((total - remaining) / total) * 100))
          : 100
        return (
          <article className={`timer-card ${timer.tone || 'accent'}`} key={timer.id}>
            <div className="timer-kicker">{timer.title || tr('Countdown', 'Odpočet')}</div>
            <div className="timer-value">{formatDurationCompact(remaining)}</div>
            <div className="timer-subtitle">{timer.subtitle || ''}</div>
            <div className="timer-rail">
              <div className="timer-fill" style={{ width: `${progress}%` }} />
            </div>
            <div className="timer-meta">
              <span>{tr('Remaining', 'Zbývá')}</span>
              <span>{formatTimerVerbose(remaining)}</span>
            </div>
          </article>
        )
      })}
    </section>
  )
}
