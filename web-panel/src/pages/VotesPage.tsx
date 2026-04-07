import { ConfigStudio } from '../components/ConfigStudio'
import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { VoteQueue } from '../components/VoteQueue'

export function VotesPage() {
  const { data, fetchDashboard } = useDashboard()

  return (
    <>
      <PageHeader
        title="Votes"
        titleCz="Hlasy"
        description={`${data?.voteQueue?.total ?? 0} pending votes in queue`}
        descriptionCz={`${data?.voteQueue?.total ?? 0} čekajících hlasů ve frontě`}
      />
      <VoteQueue
        data={data?.voteQueue || { total: 0, players: [] }}
        onRefresh={fetchDashboard}
      />
      <ConfigStudio
        sectionFilter="vote"
        title="Vote system settings"
        titleCz="Nastavení vote systému"
        eyebrow="VOTE SETTINGS"
        eyebrowCz="NASTAVENÍ HLASŮ"
      />
    </>
  )
}
