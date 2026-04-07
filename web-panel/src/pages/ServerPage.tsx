import { useTr } from '../i18n'
import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { ServerProperties } from '../components/ServerProperties'

export function ServerPage() {
  const tr = useTr()
  const { data } = useDashboard()
  const sys = data?.systemInfo

  return (
    <>
      <PageHeader
        title="Server"
        titleCz="Server"
        description="Host information and server.properties management"
        descriptionCz="Informace o hostu a sprava server.properties"
      />

      {sys && (
        <div className="page-grid-3">
          <article className="panel metric-highlight">
            <span className="eyebrow">{tr('HOST OS', 'HOST OS')}</span>
            <strong className="metric-big">{sys.osName || 'Unknown'}</strong>
            <small>{sys.osArch || '-'} - {sys.availableProcessors ?? '?'} {tr('cores', 'jader')}</small>
          </article>
          <article className="panel metric-highlight">
            <span className="eyebrow">{tr('PROCESS', 'PROCESS')}</span>
            <strong className="metric-big">{(sys.processCpuLoad ?? 0).toFixed(1)}%</strong>
            <small>{tr('Current JVM process CPU load', 'Aktualni CPU load JVM procesu')}</small>
          </article>
          <article className="panel metric-highlight">
            <span className="eyebrow">{tr('PUBLIC URL', 'PUBLIC URL')}</span>
            <strong className="metric-big">{data?.publicAccessUrl || '-'}</strong>
            <small>{tr('Persistent panel address exposed by the server', 'Trvala adresa panelu vystavena serverem')}</small>
          </article>
        </div>
      )}

      <ServerProperties />
    </>
  )
}
