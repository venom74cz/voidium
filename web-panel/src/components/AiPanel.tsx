import { useState, useRef } from 'react'
import { useTr } from '../i18n'
import { api } from '../api'
import { deepClone } from '../utils'
import type { AiCapabilities, AiHistoryEntry, PlayerAiConversation, ConfigValues } from '../types'

interface Props {
  ai: AiCapabilities | null
  consoleFeedLines: string[]
  chatFeedLines: string[]
}

export function AiPanel({ ai, consoleFeedLines, chatFeedLines }: Props) {
  const tr = useTr()
  const [output, setOutput] = useState(tr('Admin AI is ready when configured in ai.json.', 'Admin AI je připraven, pokud je nakonfigurován v ai.json.'))
  const [diffOutput, setDiffOutput] = useState(tr('No config suggestion requested yet.', 'Zatím nebyl vyžádán žádný návrh configu.'))
  const [playerHistory, setPlayerHistory] = useState<PlayerAiConversation[]>([])
  const [stagedValues, setStagedValues] = useState<ConfigValues | null>(null)
  const formRef = useRef<HTMLFormElement>(null)

  const buildPayload = () => {
    if (!formRef.current) return null
    const form = new FormData(formRef.current)
    return {
      message: form.get('message') as string,
      includeServer: form.get('includeServer') === 'on',
      includePlayers: form.get('includePlayers') === 'on',
      includeModules: form.get('includeModules') === 'on',
      includeConfigs: form.get('includeConfigs') === 'on',
      configFiles: String(form.get('configFiles') || '').split(',').map(s => s.trim()).filter(Boolean),
    }
  }

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const payload = buildPayload()
    if (!payload) return
    setOutput('Admin AI is thinking...')
    const result = await api.aiAdmin(payload)
    setOutput(result.answer || result.message || 'No answer.')
  }

  const handleSuggest = async () => {
    const payload = buildPayload()
    if (!payload) return
    setDiffOutput('Generating config suggestions...')
    const result = await api.aiSuggest(payload)
    const warnings = Array.isArray(result.warnings) && result.warnings.length
      ? '\n\nWarnings:\n- ' + result.warnings.join('\n- ') : ''
    const staged = result.staged
      ? `\n\nStaged config diff:\n${result.stagedDiffPreview || result.stagedSummary || 'No staged diff.'}`
      : ''
    setDiffOutput(`${result.summary || 'No summary.'}\n\n${result.diffPreview || 'No diff preview.'}${staged}${warnings}`)
    if (result.staged && result.values) {
      setStagedValues(deepClone(result.values))
    } else {
      setStagedValues(null)
    }
  }

  const handleIncidentReview = () => {
    const context = tr(
      'Review the following recent server logs for potential incidents, anomalies, errors, or security concerns. Summarize findings and suggest actions.',
      'Zkontroluj následující nedávné server logy na potenciální incidenty, anomálie, chyby nebo bezpečnostní rizika. Shrň nálezy a navrhni akce.'
    )
      + '\n\n--- CONSOLE (last ' + consoleFeedLines.length + ' lines) ---\n' + (consoleFeedLines.join('\n') || '(empty)')
      + '\n\n--- CHAT (last ' + chatFeedLines.length + ' lines) ---\n' + (chatFeedLines.join('\n') || '(empty)')
    if (formRef.current) {
      const textarea = formRef.current.querySelector('textarea[name="message"]') as HTMLTextAreaElement | null
      if (textarea) textarea.value = context
      const serverCheck = formRef.current.querySelector('input[name="includeServer"]') as HTMLInputElement | null
      const modulesCheck = formRef.current.querySelector('input[name="includeModules"]') as HTMLInputElement | null
      if (serverCheck) serverCheck.checked = true
      if (modulesCheck) modulesCheck.checked = true
    }
    setOutput(tr('Incident review prompt loaded. Click "Ask admin AI" to analyze.', 'Prompt pro kontrolu incidentů načten. Klikni "Ask admin AI" pro analýzu.'))
  }

  const handleApplyAiSuggestion = async () => {
    if (!stagedValues) return
    if (!confirm(tr('Apply the staged AI suggestion to config files?', 'Použít staged AI návrh do config souborů?'))) return
    await api.configApply(stagedValues, 'ai-suggestion-confirmed')
    setStagedValues(null)
  }

  const loadPlayerHistory = async () => {
    const data = await api.aiPlayers()
    setPlayerHistory(data.conversations || [])
  }

  const history = ai?.history || []
  const files = Array.isArray(ai?.configFiles) ? ai.configFiles.slice(0, 8).join(', ') : tr('No config files detected', 'Nebyly nalezeny žádné config soubory')

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{tr('ADMIN AI', 'ADMIN AI')}</span>
          <h2>{tr('Control panel copilot', 'Copilot řídicího panelu')}</h2>
        </div>
      </div>
      <form className="ai-form" ref={formRef} onSubmit={handleSubmit}>
        <label>
          <span>{tr('Ask the admin assistant', 'Zeptej se admin asistenta')}</span>
          <textarea
            name="message"
            rows={6}
            placeholder={tr(
              'Ask for tuning advice, config review, incident analysis, or improvement ideas.',
              'Ptej se na tuning, review configu, analýzu incidentů nebo nápady na zlepšení.'
            )}
            required
          />
        </label>
        <div className="toggle-grid">
          <label className="toggle"><input type="checkbox" name="includeServer" defaultChecked /><span>{tr('Server snapshot', 'Snímek serveru')}</span></label>
          <label className="toggle"><input type="checkbox" name="includePlayers" /><span>{tr('Online players', 'Online hráči')}</span></label>
          <label className="toggle"><input type="checkbox" name="includeModules" defaultChecked /><span>{tr('Module health', 'Zdraví modulů')}</span></label>
          <label className="toggle"><input type="checkbox" name="includeConfigs" defaultChecked /><span>{tr('Config files', 'Config soubory')}</span></label>
        </div>
        <label>
          <span>{tr('Config files to include', 'Config soubory k zahrnutí')}</span>
          <input type="text" name="configFiles" placeholder="general.json, web.json, discord.json" />
        </label>
        <div className="button-row">
          <button type="submit">{tr('Ask admin AI', 'Zeptat se admin AI')}</button>
          <button type="button" className="ghost" onClick={handleSuggest}>{tr('Suggest config changes', 'Navrhnout změny configu')}</button>
          <button type="button" className="ghost" onClick={handleIncidentReview}>{tr('Review incidents', 'Kontrola incidentů')}</button>
          <button type="button" className="ghost" disabled={!stagedValues} onClick={handleApplyAiSuggestion}>
            {tr('Apply staged AI suggestion', 'Použít staged AI návrh')}
          </button>
        </div>
      </form>
      <div className="status-log ai-output" style={{ whiteSpace: 'pre-wrap' }}>{output}</div>
      <div className="status-log ai-output" style={{ whiteSpace: 'pre-wrap' }}>{diffOutput}</div>
      <div className="ai-history">
        {!history.length
          ? <div className="history-item">{tr('No admin AI history yet.', 'Zatím není žádná historie admin AI.')}</div>
          : history.slice(-8).reverse().map((item: AiHistoryEntry, i: number) => (
            <div className="history-item" key={i}>
              <strong>{item.role || 'assistant'}</strong>
              <div>{item.content || ''}</div>
            </div>
          ))
        }
      </div>
      <div className="roadmap">
        <div>{tr('Admin AI', 'Admin AI')}: {ai?.enabled ? tr('enabled', 'zapnuto') : tr('disabled in ai.json', 'vypnuto v ai.json')}</div>
        <div>{tr('Redaction', 'Redakce')}: {ai?.redactsSensitiveValues ? tr('enabled', 'zapnuto') : tr('disabled', 'vypnuto')}</div>
        <div>{tr('Available config files', 'Dostupné config soubory')}: {files || tr('none', 'žádné')}</div>
      </div>
      <div className="panel-head" style={{ marginTop: 20 }}>
        <div>
          <span className="eyebrow">{tr('PLAYER AI HISTORY', 'HISTORIE HRÁČSKÉHO AI')}</span>
          <h2>{tr('Recent player conversations', 'Nedávné hráčské konverzace')}</h2>
        </div>
        <button className="ghost" type="button" onClick={loadPlayerHistory}>{tr('Load', 'Načíst')}</button>
      </div>
      <div className="ai-history">
        {!playerHistory.length
          ? <div className="feed-item">{tr('No player AI conversations recorded.', 'Žádné hráčské AI konverzace.')}</div>
          : playerHistory.map((conv, i) => (
            <div className="feed-item" key={i}>
              <strong>{conv.uuid}</strong> ({conv.turns} {tr('turns', 'otoček')})
              <div className="ai-history" style={{ maxHeight: 200 }}>
                {(conv.history || []).map((turn, j) => (
                  <div className="history-item" key={j}><strong>{turn.role}</strong>{turn.content}</div>
                ))}
              </div>
            </div>
          ))
        }
      </div>
    </article>
  )
}
