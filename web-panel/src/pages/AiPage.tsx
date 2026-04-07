import { ConfigStudio } from '../components/ConfigStudio'
import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { AiPanel } from '../components/AiPanel'

export function AiPage() {
  const { data } = useDashboard()
  const consoleFeedLines = (data?.consoleFeed || []).slice(-30).map(e => `[${e.level}] ${e.logger}: ${e.message}`)
  const chatFeedLines = (data?.chatFeed || []).slice(-20).map(e => `${e.sender}: ${e.message}`)

  return (
    <>
      <PageHeader
        title="AI Assistant"
        titleCz="AI Asistent"
        description="AI-powered server analysis, tuning suggestions, and config automation"
        descriptionCz="AI analýza serveru, návrhy ladění a automatizace konfigurace"
      />
      <AiPanel
        ai={data?.ai || null}
        consoleFeedLines={consoleFeedLines}
        chatFeedLines={chatFeedLines}
      />
      <ConfigStudio
        sectionFilter="ai"
        title="AI module settings"
        titleCz="Nastavení AI modulu"
        eyebrow="AI SETTINGS"
        eyebrowCz="NASTAVENÍ AI"
      />
    </>
  )
}
