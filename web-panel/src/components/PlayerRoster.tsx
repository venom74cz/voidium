import { useState } from 'react'
import { useTr } from '../i18n'
import { api } from '../api'
import { useToast } from './Toast'
import type { Player } from '../types'

interface Props {
  players: Player[]
  onRefresh?: () => Promise<void>
}

export function PlayerRoster({ players, onRefresh }: Props) {
  const tr = useTr()
  const { toast } = useToast()
  const [busyPlayer, setBusyPlayer] = useState<string | null>(null)

  async function runPlayerAction(player: Player, action: 'player_kick' | 'player_ban' | 'player_unlink') {
    if (busyPlayer) return

    if (action === 'player_unlink' && !player.linked) {
      toast(tr('This player is not linked.', 'Tento hráč není propojený.'), 'warning')
      return
    }

    let params: Record<string, string> = { player: player.name, uuid: player.uuid }

    if (action === 'player_kick') {
      const reason = window.prompt(tr(`Kick ${player.name} reason`, `Důvod kicku pro ${player.name}`), tr('Removed by web panel', 'Odebrán přes web panel'))
      if (reason === null) return
      params = { ...params, reason: reason.trim() || tr('Removed by web panel', 'Odebrán přes web panel') }
    }

    if (action === 'player_ban') {
      const reason = window.prompt(tr(`Ban ${player.name} reason`, `Důvod banu pro ${player.name}`), tr('Banned by web panel', 'Zabanován přes web panel'))
      if (reason === null) return
      params = { ...params, reason: reason.trim() || tr('Banned by web panel', 'Zabanován přes web panel') }
    }

    if (action === 'player_unlink') {
      const confirmed = window.confirm(tr(
        `Unlink Discord account from ${player.name}?`,
        `Odpojit Discord účet od hráče ${player.name}?`
      ))
      if (!confirmed) return
    }

    setBusyPlayer(player.uuid)
    try {
      const result = await api.action(action, params)
      toast(result.message, 'success')
      await onRefresh?.()
    } catch (error) {
      const message = error instanceof Error ? error.message : tr('Player action failed.', 'Akce nad hráčem selhala.')
      toast(message, 'error')
    } finally {
      setBusyPlayer(null)
    }
  }

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('PLAYERS', 'HRÁČI')}</span>
          <h2>{tr('Live roster', 'Živý přehled')}</h2>
        </div>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>{tr('Name', 'Jméno')}</th><th>{tr('UUID', 'UUID')}</th><th>{tr('Link', 'Link')}</th><th>{tr('Ping', 'Ping')}</th><th>{tr('Actions', 'Akce')}</th></tr>
          </thead>
          <tbody>
            {!players.length
              ? <tr><td colSpan={5}>{tr('No players online.', 'Žádní hráči online.')}</td></tr>
              : players.map(player => (
                <tr key={player.uuid}>
                  <td>
                    <div className="table-cell-stack">
                      <strong>{player.name}</strong>
                      {player.discordId && <span>{player.discordId}</span>}
                    </div>
                  </td>
                  <td>{player.uuid}</td>
                  <td>
                    <span className={`status-pill ${player.linked ? 'ok' : 'off'}`}>
                      {player.linked ? tr('Linked', 'Propojeno') : tr('Unlinked', 'Nepropojeno')}
                    </span>
                  </td>
                  <td>{player.ping} ms</td>
                  <td>
                    <div className="player-actions">
                      <button
                        className="ghost small"
                        type="button"
                        disabled={busyPlayer === player.uuid}
                        onClick={() => runPlayerAction(player, 'player_kick')}
                      >
                        {tr('Kick', 'Kick')}
                      </button>
                      <button
                        className="danger small"
                        type="button"
                        disabled={busyPlayer === player.uuid}
                        onClick={() => runPlayerAction(player, 'player_ban')}
                      >
                        {tr('Ban', 'Ban')}
                      </button>
                      <button
                        className="ghost small"
                        type="button"
                        disabled={busyPlayer === player.uuid || !player.linked}
                        onClick={() => runPlayerAction(player, 'player_unlink')}
                      >
                        {tr('Unlink', 'Odpojit')}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>
    </article>
  )
}
