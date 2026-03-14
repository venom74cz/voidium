import { useTr } from '../i18n'
import { average, formatChartTime, formatNumber } from '../utils'
import type { HistoryPoint } from '../types'

interface Props {
  history: HistoryPoint[]
}

export function StatsCharts({ history }: Props) {
  const tr = useTr()
  const points = Array.isArray(history) ? history.slice(-180) : []

  const playerValues = points.map(p => Number(p.players || 0))
  const tpsValues = points.map(p => Number(p.tps || 0))

  if (!points.length) {
    return (
      <>
        <article className="panel panel-wide">
          <div className="panel-head">
            <div><span className="eyebrow">{tr('STATS', 'STATISTIKY')}</span><h2>{tr('Player and TPS history', 'Historie hráčů a TPS')}</h2></div>
          </div>
          <div className="chart-stack">
            <div className="chart-card">
              <div className="chart-head"><strong>Players</strong><span>{tr('Waiting for samples…', 'Čekám na vzorky…')}</span></div>
              <svg className="chart-svg" viewBox="0 0 560 180" preserveAspectRatio="none">
                <text x="24" y="90" className="chart-empty">{tr('No stats history yet.', 'Zatím není žádná historie statistik.')}</text>
              </svg>
            </div>
            <div className="chart-card">
              <div className="chart-head"><strong>TPS</strong><span>{tr('Waiting for samples…', 'Čekám na vzorky…')}</span></div>
              <svg className="chart-svg" viewBox="0 0 560 180" preserveAspectRatio="none">
                <text x="24" y="90" className="chart-empty">{tr('No TPS history yet.', 'Zatím není žádná TPS historie.')}</text>
              </svg>
            </div>
          </div>
        </article>
        <article className="panel">
          <div className="panel-head"><div><span className="eyebrow">{tr('STATS SUMMARY', 'SOUHRN STATISTIK')}</span><h2>{tr('Recent trend', 'Nedávný trend')}</h2></div></div>
          <div className="roadmap">
            <div>{tr('Stats collection starts after the first scheduled sample.', 'Sběr statistik začne po prvním naplánovaném vzorku.')}</div>
          </div>
        </article>
      </>
    )
  }

  const latest = points[points.length - 1]
  const avgPlayers = average(playerValues)
  const peakPlayers = Math.max(...playerValues, 0)
  const avgTps = average(tpsValues)
  const lowTps = Math.min(...tpsValues, 20)
  const firstStamp = formatChartTime(points[0].timestamp)
  const lastStamp = formatChartTime(latest.timestamp)

  return (
    <>
      <article className="panel panel-wide">
        <div className="panel-head">
          <div><span className="eyebrow">{tr('STATS', 'STATISTIKY')}</span><h2>{tr('Player and TPS history', 'Historie hráčů a TPS')}</h2></div>
        </div>
        <div className="chart-stack">
          <div className="chart-card">
            <div className="chart-head">
              <strong>Players</strong>
              <span>{tr('Window', 'Okno')}: {firstStamp} → {lastStamp}</span>
            </div>
            <ChartSvg values={playerValues} tone="players" maxValue={Math.max(10, peakPlayers)} />
          </div>
          <div className="chart-card">
            <div className="chart-head">
              <strong>TPS</strong>
              <span>{tr('Latest', 'Poslední')}: {formatNumber(latest.tps, 2)} TPS</span>
            </div>
            <ChartSvg values={tpsValues} tone="tps" maxValue={20} />
          </div>
        </div>
      </article>
      <article className="panel">
        <div className="panel-head"><div><span className="eyebrow">{tr('STATS SUMMARY', 'SOUHRN STATISTIK')}</span><h2>{tr('Recent trend', 'Nedávný trend')}</h2></div></div>
        <div className="roadmap">
          <div>{tr('Average players', 'Průměr hráčů')}: {formatNumber(avgPlayers, 1)}</div>
          <div>{tr('Peak players', 'Peak hráčů')}: {peakPlayers}</div>
          <div>{tr('Average TPS', 'Průměr TPS')}: {formatNumber(avgTps, 2)}</div>
          <div>{tr('Lowest TPS', 'Nejnižší TPS')}: {formatNumber(lowTps, 2)}</div>
          <div>{tr('Samples shown', 'Zobrazené vzorky')}: {points.length}</div>
        </div>
      </article>
    </>
  )
}

function ChartSvg({ values, tone, maxValue }: { values: number[], tone: string, maxValue: number }) {
  const width = 560, height = 180, padding = 16
  const drawW = width - padding * 2
  const drawH = height - padding * 2
  const safeMax = Math.max(1, maxValue)

  const pathData = values.map((v, i) => {
    const x = padding + (values.length <= 1 ? 0 : (i / (values.length - 1)) * drawW)
    const y = padding + drawH - (Math.max(0, Number(v || 0)) / safeMax) * drawH
    return `${i === 0 ? 'M' : 'L'}${x.toFixed(2)} ${y.toFixed(2)}`
  }).join(' ')

  const areaData = `${pathData} L${padding + drawW} ${height - padding} L${padding} ${height - padding} Z`

  return (
    <svg className="chart-svg" viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none">
      <line x1={padding} y1={padding} x2={padding} y2={height - padding} className="chart-gridline" />
      <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} className="chart-gridline" />
      <line x1={padding} y1={padding + drawH / 2} x2={width - padding} y2={padding + drawH / 2} className="chart-gridline" />
      <path d={areaData} className={`chart-area ${tone}`} />
      <path d={pathData} className={`chart-path ${tone}`} />
    </svg>
  )
}
