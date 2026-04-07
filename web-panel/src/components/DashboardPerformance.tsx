import { useTr } from '../i18n'
import { StatsCharts } from './StatsCharts'
import type { DashboardData } from '../types'

interface Props {
  data: DashboardData | null
}

export function DashboardPerformance({ data }: Props) {
  const tr = useTr()
  const sys = data?.systemInfo

  return (
    <>
      {sys && (
        <div className="page-grid-3">
          <article className="panel metric-highlight">
            <span className="eyebrow">{tr('CPU LOAD', 'CPU LOAD')}</span>
            <strong className="metric-big">{(sys.cpuLoad ?? 0).toFixed(1)}%</strong>
            <small>{sys.availableProcessors ?? '?'} {tr('cores', 'jader')} - {sys.osName ?? ''}</small>
          </article>
          <article className="panel metric-highlight">
            <span className="eyebrow">{tr('SYSTEM RAM', 'SYSTEM RAM')}</span>
            <strong className="metric-big">{sys.systemRamUsedMb ?? 0} / {sys.systemRamTotalMb ?? 0} MB</strong>
            <small>{(sys.systemRamPercent ?? 0).toFixed(1)}% {tr('used', 'vyuzito')}</small>
          </article>
          <article className="panel metric-highlight">
            <span className="eyebrow">{tr('DISK', 'DISK')}</span>
            <strong className="metric-big">{(sys.diskUsedGb ?? 0).toFixed(1)} / {(sys.diskTotalGb ?? 0).toFixed(1)} GB</strong>
            <small>{(sys.diskPercent ?? 0).toFixed(1)}% {tr('used', 'vyuzito')}</small>
          </article>
        </div>
      )}

      <div className="page-grid-2">
        <article className="panel metric-highlight">
          <span className="eyebrow">TPS</span>
          <strong className="metric-big">{(data?.tps ?? 20).toFixed(1)}</strong>
          <small>MSPT: {(data?.mspt ?? 0).toFixed(1)}ms</small>
        </article>
        <article className="panel metric-highlight">
          <span className="eyebrow">{tr('JVM MEMORY', 'JVM PAMET')}</span>
          <strong className="metric-big">{data?.memoryUsedMb ?? 0} / {data?.memoryMaxMb ?? 0} MB</strong>
          <small>{(data?.memoryUsagePercent ?? 0).toFixed(1)}% {tr('used', 'vyuzito')}</small>
        </article>
      </div>

      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('HISTORY', 'HISTORIE')}</span>
            <h2>{tr('Performance History', 'Historie vykonu')}</h2>
          </div>
        </div>
        <StatsCharts history={data?.history || []} />
      </article>
    </>
  )
}
