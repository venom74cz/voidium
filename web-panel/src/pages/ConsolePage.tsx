import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { Console } from '../components/Console'
import { AuditFeed } from '../components/LiveFeeds'

export function ConsolePage() {
  const { data } = useDashboard()

  return (
    <>
      <PageHeader
        title="Console"
        titleCz="Konzole"
        description="Execute server commands and view audit trail"
        descriptionCz="Provádějte serverové příkazy a sledujte audit"
      />
      <div className="page-grid-2">
        <Console />
        <AuditFeed auditFeed={data?.auditFeed || []} />
      </div>
    </>
  )
}
