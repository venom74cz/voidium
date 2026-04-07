import { useState } from 'react'
import { useTr } from '../i18n'
import { api } from '../api'

export function Console() {
  const tr = useTr()
  const [status, setStatus] = useState(tr(
    'Console ready. Only approved command families are allowed from the web panel.',
    'Konzole je pripravena. Z web panelu jsou povolene jen schvalene skupiny prikazu.'
  ))

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formElement = e.currentTarget
    const form = new FormData(formElement)
    const command = form.get('command') as string
    const result = await api.consoleExecute(command)
    setStatus(result.message || 'Command queued.')
    if (result.message && !result.message.includes('blocked')) {
      formElement.reset()
    }
  }

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('WEB CONSOLE', 'WEB KONZOLE')}</span>
          <h2>{tr('Run approved server commands', 'Spustit povolene serverove prikazy')}</h2>
        </div>
      </div>
      <form className="action-row" onSubmit={handleSubmit}>
        <label>
          <span>{tr('Command', 'Prikaz')}</span>
          <input type="text" name="command" placeholder={tr('say Web panel online', 'say Web panel online')} required />
        </label>
        <div className="button-row">
          <button type="submit">{tr('Run command', 'Spustit prikaz')}</button>
        </div>
      </form>
      <div className="status-log">{status}</div>
    </article>
  )
}
