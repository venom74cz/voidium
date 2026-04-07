import { useDashboard } from '../context/DashboardContext'
import { PageHeader } from '../components/PageHeader'
import { ModuleHealth } from '../components/ModuleHealth'
import { useTr } from '../i18n'

export function ModulesPage() {
  const tr = useTr()
  const { data } = useDashboard()
  const enabledCount = (data?.modules || []).filter(module => module.enabled).length
  const totalCount = (data?.modules || []).length

  return (
    <>
      <PageHeader
        title="Modules"
        titleCz="Moduly"
        description={`${enabledCount}/${totalCount} modules enabled`}
        descriptionCz={`${enabledCount}/${totalCount} modulu zapnuto`}
      />
      <ModuleHealth modules={data?.modules || []} />
      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('SYSTEM OVERVIEW', 'SYSTEM OVERVIEW')}</span>
            <h2>{tr('Live module health', 'Aktualni stav modulu')}</h2>
          </div>
        </div>
        <p className="feature-note">
          {tr(
            'Feature settings were moved to dedicated pages. Modules now stays focused on current status, service connectivity, and whether the web panel is reachable.',
            'Nastaveni funkci jsou presunuta na samostatne stranky. Moduly se ted soustredi jen na aktualni stav, dostupnost sluzeb a to, jestli je web panel dostupny.'
          )}
        </p>
      </article>
    </>
  )
}
