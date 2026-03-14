import { useTr } from '../i18n'
import type { Module } from '../types'

interface Props {
  modules: Module[]
}

export function ModuleHealth({ modules }: Props) {
  const tr = useTr()

  return (
    <article className="panel">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('SYSTEMS', 'SYSTÉMY')}</span>
          <h2>{tr('Module health', 'Zdraví modulů')}</h2>
        </div>
      </div>
      <div className="module-list">
        {modules.map(mod => (
          <div className="module-item" key={mod.name}>
            <div>
              <strong>{mod.name}</strong>
              <span>{mod.detail}</span>
            </div>
            <strong className={mod.enabled ? 'ok' : 'off'}>
              {mod.enabled ? tr('ON', 'ZAP') : tr('OFF', 'VYP')}
            </strong>
          </div>
        ))}
      </div>
    </article>
  )
}
