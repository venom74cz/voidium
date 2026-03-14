import { useTr } from '../i18n'
import type { Player } from '../types'

interface Props {
  players: Player[]
}

export function PlayerRoster({ players }: Props) {
  const tr = useTr()

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
            <tr><th>Name</th><th>UUID</th><th>Ping</th></tr>
          </thead>
          <tbody>
            {!players.length
              ? <tr><td colSpan={3}>{tr('No players online.', 'Žádní hráči online.')}</td></tr>
              : players.map(player => (
                <tr key={player.uuid}>
                  <td>{player.name}</td>
                  <td>{player.uuid}</td>
                  <td>{player.ping} ms</td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>
    </article>
  )
}
