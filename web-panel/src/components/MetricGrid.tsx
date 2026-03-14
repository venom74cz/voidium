import { useTr } from '../i18n'
import type { DashboardData } from '../types'

interface Props {
  data: DashboardData | null
}

export function MetricGrid({ data }: Props) {
  const tr = useTr()
  if (!data) return <section className="grid metrics" />

  const sys = data.systemInfo
  const cards: [string, string, string][] = [
    [tr('Players online', 'Hráči online'), `${data.onlinePlayers} / ${data.maxPlayers}`, data.serverName],
    [tr('Performance', 'Výkon'), `${data.tps} TPS`, `${data.mspt} MSPT`],
    [tr('Memory', 'Paměť'), `${data.memoryUsedMb} MB`, `${data.memoryUsagePercent}% of ${data.memoryMaxMb} MB`],
    [tr('Uptime', 'Uptime'), data.uptime, `${tr('Next restart', 'Další restart')}: ${data.nextRestart}`],
  ]

  const sysCards: [string, string, string][] = sys ? [
    [tr('CPU', 'CPU'), `${sys.cpuLoad ?? 0}%`, `${sys.availableProcessors ?? '?'} ${tr('cores', 'jader')} · ${tr('Process', 'Proces')}: ${sys.processCpuLoad ?? 0}%`],
    [tr('System RAM', 'Systém RAM'), `${sys.systemRamUsedMb ?? 0} MB`, `${sys.systemRamPercent ?? 0}% ${tr('of', 'z')} ${sys.systemRamTotalMb ?? 0} MB`],
    [tr('Disk', 'Disk'), `${sys.diskUsedGb ?? 0} GB`, `${sys.diskPercent ?? 0}% ${tr('of', 'z')} ${sys.diskTotalGb ?? 0} GB`],
  ] : []

  return (
    <>
      <section className="grid metrics">
        {cards.map(([label, value, sub]) => (
          <article className="metric-card" key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
            <small>{sub}</small>
          </article>
        ))}
      </section>
      {sysCards.length > 0 && (
        <section className="grid metrics-sys">
          {sysCards.map(([label, value, sub]) => (
            <article className="metric-card" key={label}>
              <span>{label}</span>
              <strong>{value}</strong>
              <small>{sub}</small>
            </article>
          ))}
        </section>
      )}
    </>
  )
}
