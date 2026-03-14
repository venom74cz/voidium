import { useTr } from '../i18n'
import { api } from '../api'
import type { TicketData } from '../types'

interface Props {
  data: TicketData
  onRefresh: () => void
}

export function TicketPanel({ data, onRefresh }: Props) {
  const tr = useTr()
  const items = Array.isArray(data.items) ? data.items : []

  const handleAction = async (action: string, channelId: string) => {
    if (action === 'note') {
      const message = prompt(tr('Enter the note to send into the ticket.', 'Zadej poznámku, která se pošle do ticketu.'))
      if (!message) return
      await api.action('ticket_note', { channelId, message })
      return
    }
    if (action === 'transcript') {
      const result = await api.actionJson('ticket_transcript', { channelId })
      const lines = Array.isArray(result.lines) ? result.lines : []
      if (!lines.length) { alert(tr('No transcript lines cached.', 'Žádné řádky přepisu v cache.')); return }
      const blob = new Blob([lines.join('\n')], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `transcript-${result.player || 'ticket'}.txt`
      document.body.appendChild(a); a.click(); a.remove()
      URL.revokeObjectURL(url)
      return
    }
    if (!confirm(tr('Close this ticket from the web panel?', 'Zavřít tento ticket z web panelu?'))) return
    await api.action('ticket_close', { channelId })
    onRefresh()
  }

  return (
    <>
      <article className="panel panel-wide">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('TICKETS', 'TICKETY')}</span>
            <h2>{tr('Open ticket snapshot', 'Přehled otevřených ticketů')}</h2>
          </div>
        </div>
        <div className="vote-queue-list">
          {!items.length
            ? <div className="feed-item">{tr('No open tickets tracked right now.', 'Právě nejsou sledovány žádné otevřené tickety.')}</div>
            : items.map(item => (
              <div className="vote-row" key={item.channelId}>
                <div className="vote-row-meta">
                  <strong>{item.player || 'unknown'}</strong>
                  <div className="vote-row-sub">Channel: {item.channelId || '-'}</div>
                </div>
                <div>
                  <div className="vote-badge">{item.cachedMessages || 0} {tr('lines', 'řádků')}</div>
                </div>
                <div className="vote-row-meta">
                  {(item.previewLines || []).length
                    ? item.previewLines.map((line, i) => <div className="vote-row-sub" key={i}>{line}</div>)
                    : <div className="vote-row-sub">{tr('No cached transcript lines yet.', 'Zatím nejsou žádné řádky transcript cache.')}</div>
                  }
                </div>
                <div className="button-row">
                  <button type="button" className="ghost small" onClick={() => handleAction('note', item.channelId)}>{tr('Add note', 'Přidat poznámku')}</button>
                  <button type="button" className="ghost small" onClick={() => handleAction('transcript', item.channelId)}>{tr('Transcript', 'Přepis')}</button>
                  <button type="button" className="ghost small" onClick={() => handleAction('close', item.channelId)}>{tr('Close', 'Zavřít')}</button>
                </div>
              </div>
            ))
          }
        </div>
      </article>
      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('TRANSCRIPTS', 'PŘEPISY')}</span>
            <h2>{tr('Cached preview', 'Náhled cache')}</h2>
          </div>
        </div>
        <div className="roadmap">
          <div>{tr('Open tickets', 'Otevřené tickety')}: {data.open || 0}</div>
          <div>{tr('Transcript cache lines', 'Řádky transcript cache')}: {items.reduce((sum, item) => sum + Number(item.cachedMessages || 0), 0)}</div>
        </div>
      </article>
    </>
  )
}
