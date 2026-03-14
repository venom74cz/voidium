import { useTr } from '../i18n'
import type { ChatEntry, ConsoleEntry, AuditEntry } from '../types'

interface Props {
  chatFeed: ChatEntry[]
  consoleFeed: ConsoleEntry[]
  auditFeed: AuditEntry[]
  alerts: string[]
}

export function LiveFeeds({ chatFeed, consoleFeed, auditFeed, alerts }: Props) {
  const tr = useTr()

  return (
    <>
      {/* Alerts */}
      <section className="grid split">
        <article className="panel panel-wide">
          <div className="panel-head">
            <div><span className="eyebrow">{tr('SIGNALS', 'SIGNÁLY')}</span><h2>{tr('Live alerts', 'Živé výstrahy')}</h2></div>
          </div>
          <ul className="alert-list">
            {alerts.map((alert, i) => <li key={i}>{alert}</li>)}
          </ul>
        </article>
      </section>

      {/* Chat + Console feeds */}
      <section className="grid split live-feeds">
        <article className="panel panel-wide">
          <div className="panel-head">
            <div><span className="eyebrow">{tr('SERVER CHAT', 'SERVER CHAT')}</span><h2>{tr('Live conversation feed', 'Živý konverzační feed')}</h2></div>
          </div>
          <div className="feed-list">
            {!chatFeed.length
              ? <div className="feed-item">{tr('No chat captured yet.', 'Zatím nebyl zachycen žádný chat.')}</div>
              : chatFeed.slice(-20).reverse().map((item, i) => (
                <div className="feed-item" key={i}>
                  <strong>{item.sender || 'Unknown'}</strong>
                  <div>{item.message || ''}</div>
                </div>
              ))
            }
          </div>
        </article>
        <article className="panel">
          <div className="panel-head">
            <div><span className="eyebrow">{tr('SERVER CONSOLE', 'SERVER KONZOLE')}</span><h2>{tr('Live log tail', 'Živý log')}</h2></div>
          </div>
          <div className="feed-list console-feed">
            {!consoleFeed.length
              ? <div className="feed-item">{tr('No console entries captured yet.', 'Zatím nebyly zachyceny žádné záznamy konzole.')}</div>
              : consoleFeed.slice(-30).reverse().map((item, i) => (
                <div className="feed-item" key={i}>
                  <strong>[{item.level || 'INFO'}] {item.logger || 'root'}</strong>
                  <div>{item.message || ''}</div>
                </div>
              ))
            }
          </div>
        </article>
      </section>

      {/* Audit feed in Console section */}
      <section className="grid split" style={{ display: 'none' }}>
        {/* Audit is rendered as part of the Console section below */}
      </section>
    </>
  )
}

export function AuditFeed({ auditFeed }: { auditFeed: AuditEntry[] }) {
  const tr = useTr()
  return (
    <article className="panel">
      <div className="panel-head">
        <div><span className="eyebrow">{tr('AUDIT', 'AUDIT')}</span><h2>{tr('Recent sensitive actions', 'Nedávné citlivé akce')}</h2></div>
      </div>
      <div className="feed-list">
        {!auditFeed.length
          ? <div className="feed-item">{tr('No audited actions yet.', 'Zatím nejsou žádné auditované akce.')}</div>
          : auditFeed.slice(-20).reverse().map((item, i) => (
            <div className="feed-item" key={i}>
              <strong>{item.action || 'action'} · {item.success ? 'OK' : 'FAILED'}</strong>
              <div>{item.detail || ''}</div>
              <div>{item.timestamp || ''} · {item.source || 'unknown'}</div>
            </div>
          ))
        }
      </div>
    </article>
  )
}
