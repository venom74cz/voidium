import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { LiveFeeds } from '../components/LiveFeeds'

export function FeedsPage() {
  const { data } = useDashboard()

  return (
    <>
      <PageHeader
        title="Live Feeds"
        titleCz="Živé kanály"
        description="Real-time chat, console output, audit trail, and alerts"
        descriptionCz="Chat v reálném čase, výstup konzole, audit a upozornění"
      />
      <LiveFeeds
        chatFeed={data?.chatFeed || []}
        consoleFeed={data?.consoleFeed || []}
        auditFeed={data?.auditFeed || []}
        alerts={data?.alerts || []}
      />
    </>
  )
}
