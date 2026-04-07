import { ConfigStudio } from '../components/ConfigStudio'
import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { PlayerRoster } from '../components/PlayerRoster'

export function PlayersPage() {
  const { data, fetchDashboard } = useDashboard()

  return (
    <>
      <PageHeader
        title="Players"
        titleCz="Hráči"
        description={`${data?.onlinePlayers ?? 0}/${data?.maxPlayers ?? 0} online`}
        descriptionCz={`${data?.onlinePlayers ?? 0}/${data?.maxPlayers ?? 0} online`}
      />
      <PlayerRoster players={data?.players || []} onRefresh={fetchDashboard} />
      <ConfigStudio
        sectionFilter="playerlist"
        title="Player list settings"
        titleCz="Nastavení player listu"
        eyebrow="PLAYER LIST SETTINGS"
        eyebrowCz="NASTAVENÍ PLAYER LISTU"
      />
    </>
  )
}
