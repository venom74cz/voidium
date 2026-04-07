import { ConfigStudio } from '../components/ConfigStudio'
import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { TicketPanel } from '../components/TicketPanel'

export function TicketsPage() {
  const { data, fetchDashboard } = useDashboard()

  return (
    <>
      <PageHeader
        title="Tickets"
        titleCz="Tikety"
        description={`${data?.tickets?.open ?? 0} open tickets`}
        descriptionCz={`${data?.tickets?.open ?? 0} otevřených tiketů`}
      />
      <TicketPanel
        data={data?.tickets || { open: 0, items: [] }}
        onRefresh={fetchDashboard}
      />
      <ConfigStudio
        sectionFilter="tickets"
        title="Ticket workflow settings"
        titleCz="Nastavení ticket workflow"
        eyebrow="TICKET SETTINGS"
        eyebrowCz="NASTAVENÍ TIKETŮ"
      />
    </>
  )
}
