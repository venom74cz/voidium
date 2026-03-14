import { useState, useEffect } from 'react'
import { useTr } from '../i18n'
import { useToast } from './Toast'
import { api } from '../api'

export function ServerProperties() {
  const tr = useTr()
  const { toast } = useToast()
  const [properties, setProperties] = useState<Record<string, string>>({})
  const [working, setWorking] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.serverProperties()
      .then(data => {
        setProperties(data.properties || {})
        setWorking({ ...(data.properties || {}) })
        setLoading(false)
      })
      .catch(() => {
        setError(tr('Failed to load server.properties', 'Nepodařilo se načíst server.properties'))
        setLoading(false)
      })
  }, [tr])

  const hasChanges = Object.keys(working).some(k => working[k] !== properties[k])

  const save = async () => {
    const changes: Record<string, string> = {}
    for (const key of Object.keys(working)) {
      if (working[key] !== properties[key]) {
        changes[key] = working[key]
      }
    }
    if (!Object.keys(changes).length) return
    try {
      const result = await api.serverPropertiesSave(changes)
      toast(result.message || tr('Saved.', 'Uloženo.'), 'success')
      setProperties({ ...working })
    } catch {
      toast(tr('Failed to save server.properties', 'Nepodařilo se uložit server.properties'), 'error')
    }
  }

  const reset = () => setWorking({ ...properties })

  if (error) {
    return (
      <article className="panel panel-wide">
        <div className="panel-head">
          <div><span className="eyebrow">SERVER.PROPERTIES</span><h2>server.properties</h2></div>
        </div>
        <div className="status-log">{error}</div>
      </article>
    )
  }

  if (loading) {
    return (
      <article className="panel panel-wide">
        <div className="panel-head">
          <div><span className="eyebrow">SERVER.PROPERTIES</span><h2>server.properties</h2></div>
        </div>
        <div className="status-log">{tr('Loading...', 'Načítání...')}</div>
      </article>
    )
  }

  const keys = Object.keys(working).sort()

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">SERVER.PROPERTIES</span>
          <h2>server.properties</h2>
        </div>
        <div className="button-row">
          <button className="ghost" type="button" onClick={reset} disabled={!hasChanges}>{tr('Reset', 'Resetovat')}</button>
          <button type="button" onClick={save} disabled={!hasChanges}>{tr('Save changes', 'Uložit změny')}</button>
        </div>
      </div>
      <div className="server-props-note">{tr('Changes require a server restart to take effect. Sensitive keys (rcon.password, server-ip) are hidden.', 'Změny vyžadují restart serveru. Citlivé klíče (rcon.password, server-ip) jsou skryté.')}</div>
      <div className="server-props-grid">
        {keys.map(key => {
          const masked = working[key] === '***'
          const changed = working[key] !== properties[key]
          return (
            <label className={`config-field${changed ? ' field-changed' : ''}`} key={key}>
              <span>{key}</span>
              <input
                type="text"
                value={working[key]}
                disabled={masked}
                onChange={e => setWorking(prev => ({ ...prev, [key]: e.target.value }))}
              />
            </label>
          )
        })}
      </div>
    </article>
  )
}
