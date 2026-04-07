import { useDashboard } from '../context/DashboardContext'
import { Hero } from '../components/Hero'
import { MaintenanceBanner } from '../components/MaintenanceBanner'
import { TimerGrid } from '../components/TimerGrid'
import { Timeline } from '../components/Timeline'
import { QuickActions } from '../components/QuickActions'
import { DashboardPerformance } from '../components/DashboardPerformance'

export function DashboardPage() {
  const { data, timers, fetchDashboard } = useDashboard()

  return (
    <>
      <Hero data={data} />
      <MaintenanceBanner active={data?.maintenanceMode ?? false} onRefresh={fetchDashboard} />
      <DashboardPerformance data={data} />
      <TimerGrid timers={timers} />
      <Timeline timers={timers} />
      <QuickActions data={data} onRefresh={fetchDashboard} />
    </>
  )
}
