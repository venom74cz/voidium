import { useState } from 'react'
import { useTr } from '../i18n'
import { api } from '../api'

export function Console() {
  const tr = useTr()
  const [status, setStatus] = useState(tr(
    'Console ready. Only approved command families are allowed from the web panel.',
    'Konzole připravena. Z web panelu jsou povoleny pouze schválené příkazové rodiny.'
  ))

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    const command = form.get('command') as string
    const result = await api.consoleExecute(command)
    setStatus(result.message || 'Command dispatched.')
    if (result.message && !result.message.includes('blocked')) {
      e.currentTarget.reset()
    }
  }

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('WEB CONSOLE', 'WEB KONZOLE')}</span>
          <h2>{tr('Dispatch safe server commands', 'Odeslat bezpečné serverové příkazy')}</h2>
        </div>
      </div>
      <form className="action-row" onSubmit={handleSubmit}>
        <label>
          <span>{tr('Command', 'Příkaz')}</span>
          <input type="text" name="command" placeholder={tr('say Web console online', 'say Web konzole online')} required />
        </label>
        <div className="button-row">
          <button type="submit">{tr('Run command', 'Spustit příkaz')}</button>
        </div>
      </form>
      <div className="status-log">{status}</div>
    </article>
  )
}
