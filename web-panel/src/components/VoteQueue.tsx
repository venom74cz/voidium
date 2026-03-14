import { useTr } from '../i18n'
import { api } from '../api'
import type { VoteQueueData } from '../types'

interface Props {
  data: VoteQueueData
  onRefresh: () => void
}

export function VoteQueue({ data, onRefresh }: Props) {
  const tr = useTr()
  const rows = Array.isArray(data.players) ? data.players : []

  const handleVoteAction = async (action: string, player: string) => {
    if (action === 'clear' && !confirm(tr('Remove pending votes for this player?', 'Odebrat čekající hlasy tomuto hráči?'))) return
    await api.action(action === 'payout' ? 'vote_payout_player' : 'vote_clear_player', { player })
    onRefresh()
  }

  const handlePayoutAll = async () => {
    if (!confirm(tr('Deliver pending votes to all online players?', 'Doručit čekající hlasy všem online hráčům?'))) return
    await api.action('vote_payout_all_online')
    onRefresh()
  }

  const handleClearAll = async () => {
    if (!confirm(tr('Clear all pending votes?', 'Smazat všechny čekající hlasy?'))) return
    await api.action('vote_clear_all')
    onRefresh()
  }

  return (
    <>
      <article className="panel panel-wide">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('VOTE QUEUE', 'VOTE QUEUE')}</span>
            <h2>{tr('Pending vote inspector', 'Inspektor čekajících hlasů')}</h2>
          </div>
          <div className="button-row">
            <button className="ghost" type="button" onClick={handlePayoutAll}>{tr('Payout all online', 'Payout všem online')}</button>
            <button className="ghost" type="button" onClick={handleClearAll}>{tr('Clear all pending', 'Smazat vše čekající')}</button>
          </div>
        </div>
        <div className="vote-queue-list">
          {!rows.length
            ? <div className="feed-item">{tr('No pending votes queued.', 'Ve frontě nejsou žádné čekající hlasy.')}</div>
            : rows.map(item => (
              <div className="vote-row" key={item.player}>
                <div className="vote-row-meta">
                  <strong>{item.player}</strong>
                  <div className="vote-row-sub">{tr('Latest service', 'Poslední služba')}: {item.latestService || '-'}</div>
                  <div className="vote-row-sub">{tr('Queued at', 'Zařazeno')}: {item.latestQueuedAt || '-'}</div>
                </div>
                <div>
                  <div className={`vote-badge ${item.online ? '' : 'off'}`}>
                    {item.online ? tr('ONLINE', 'ONLINE') : tr('OFFLINE', 'OFFLINE')}
                  </div>
                </div>
                <div className="vote-row-meta">
                  <strong>{item.count || 0} {tr('vote(s)', 'hlasů')}</strong>
                  <div className="vote-row-sub">{tr('Vote time', 'Čas hlasu')}: {item.latestVoteAt || '-'}</div>
                </div>
                <div className="button-row">
                  <button type="button" className="ghost small" onClick={() => handleVoteAction('payout', item.player)}>{tr('Payout', 'Payout')}</button>
                  <button type="button" className="ghost small" onClick={() => handleVoteAction('clear', item.player)}>{tr('Clear', 'Smazat')}</button>
                </div>
              </div>
            ))
          }
        </div>
      </article>
      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('VOTE SUMMARY', 'SOUHRN HLASŮ')}</span>
            <h2>{tr('Queue status', 'Stav fronty')}</h2>
          </div>
        </div>
        <div className="roadmap">
          <div>{tr('Total pending votes', 'Celkem čekajících hlasů')}: {data.total || 0}</div>
          <div>{tr('Players in queue', 'Hráči ve frontě')}: {rows.length}</div>
        </div>
      </article>
    </>
  )
}
