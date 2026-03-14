import { useState, useEffect, useCallback, useRef } from 'react'
import { useTr } from '../i18n'
import { useToast } from './Toast'
import { api } from '../api'
import { deepClone, extractLastHexColor, toVoidiumHex, formatValue } from '../utils'
import { McTextPreview } from './McTextPreview'
import type { ConfigSchema, ConfigValues, ConfigField, ConfigDiffChange, DiscordRole } from '../types'

export function ConfigStudio() {
  const tr = useTr()
  const { toast } = useToast()
  const [schema, setSchema] = useState<ConfigSchema>({})
  const [values, setValues] = useState<ConfigValues>({})
  const [working, setWorking] = useState<ConfigValues>({})
  const [activeSection, setActiveSection] = useState('general')
  const [status, setStatus] = useState(tr('Config studio ready.', 'Config studio připraveno.'))
  const [diffChanges, setDiffChanges] = useState<ConfigDiffChange[]>([])
  const [diffSummary, setDiffSummary] = useState('')
  const [diffMeta, setDiffMeta] = useState<{ requiresWebRestart?: boolean; action?: string }>({})
  const [previewErrors, setPreviewErrors] = useState<string[]>([])
  const [preview, setPreview] = useState(tr('No server-side preview yet.', 'Zatím žádný server-side náhled.'))
  const [stagedAi, setStagedAi] = useState<ConfigValues | null>(null)

  const fetchStudio = useCallback(async () => {
    try {
      const [s, v] = await Promise.all([api.configSchema(), api.configValues()])
      setSchema(s)
      setValues(v)
      setWorking(deepClone(v))
      setStagedAi(null)
      if (!s[activeSection]) setActiveSection(Object.keys(s)[0] || 'general')
      setStatus(tr('Config studio synced with current files.', 'Config studio je synchronizované s aktuálními soubory.'))
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Failed to load config'
      setStatus(msg)
      toast(msg, 'error')
    }
  }, [activeSection, tr])

  useEffect(() => { fetchStudio() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const updateField = (key: string, value: unknown) => {
    setWorking(prev => {
      const next = deepClone(prev)
      if (!next[activeSection]) next[activeSection] = {}
      next[activeSection][key] = value
      return next
    })
    setStatus(`Unsaved changes in ${activeSection}.`)
  }

  const previewDiff = async () => {
    const [diffRes, previewRes] = await Promise.all([
      api.configDiff(working),
      api.configPreview(working),
    ])
    const changes = Array.isArray(diffRes.changes) ? diffRes.changes : []
    setDiffChanges(changes)
    setDiffSummary(diffRes.summary || (changes.length ? `${changes.length} change(s) detected` : tr('No changes detected.', 'Žádné změny nenalezeny.')))
    setDiffMeta({ requiresWebRestart: diffRes.requiresWebRestart, action: diffRes.recommendedAction?.label })
    setPreviewErrors(Array.isArray(previewRes.validationErrors) ? previewRes.validationErrors : [])
    setPreview(previewRes.previews?.[activeSection] || tr('No preview available for this section.', 'Žádný náhled pro tuto sekci.'))
    toast(changes.length ? tr(`${changes.length} change(s) found.`, `${changes.length} změn(a) nalezeno.`) : tr('No changes.', 'Žádné změny.'), changes.length ? 'info' : 'success')
  }

  const applyConfig = async (source = 'manual-web') => {
    const result = await api.configApply(working, source)
    const errors = Array.isArray(result.validationErrors) && result.validationErrors.length ? ' Validation: ' + result.validationErrors.join(' | ') : ''
    const action = result.recommendedAction?.label ? ' Next: ' + result.recommendedAction.label : ''
    const msg = (result.message || 'Apply finished.') + action + errors
    setStatus(msg)
    toast(result.applied ? tr('Config applied successfully.', 'Config úspěšně aplikován.') : msg, result.applied ? 'success' : 'warning')
    if (result.applied) {
      setStagedAi(null)
      setDiffChanges([])
      setDiffSummary('')
      setDiffMeta({})
      setPreviewErrors([])
      await fetchStudio()
    }
  }

  const restoreDefaults = async () => {
    const result = await api.configDefaults(activeSection, working)
    if (result.values) { setWorking(result.values) }
    setStatus(result.message || 'Defaults restored.')
    toast(result.message || tr('Defaults restored.', 'Výchozí hodnoty obnoveny.'), 'success')
  }

  const applyLocale = async (locale: string) => {
    const result = await api.configLocale(activeSection, locale, working)
    if (result.values) { setWorking(result.values) }
    setStatus(result.message || `Applied ${locale.toUpperCase()} preset.`)
    toast(result.message || `Applied ${locale.toUpperCase()} preset.`, 'success')
  }

  const rollback = async () => {
    if (!confirm(tr('Rollback to the last backup? This will overwrite current config files.', 'Obnovit ze zálohy? Toto přepíše aktuální config soubory.'))) return
    const result = await api.configRollback()
    setStatus(result.message || 'Rollback finished.')
    toast(result.message || tr('Rollback finished.', 'Rollback dokončen.'), result.rolledBack ? 'success' : 'warning')
    setDiffChanges([])
    setDiffSummary('')
    setDiffMeta({})
    setPreviewErrors([])
    await fetchStudio()
  }

  const reload = async () => {
    const result = await api.configReload()
    setStatus(result.message || 'Reload dispatched.')
    toast(result.message || tr('Reload dispatched.', 'Reload odeslán.'), 'success')
    setDiffChanges([])
    setDiffSummary('')
    setDiffMeta({})
    setPreviewErrors([])
  }

  const applyAiSuggestion = async () => {
    if (!stagedAi) return
    if (!confirm(tr('Apply the staged AI suggestion to config files?', 'Použít staged AI návrh do config souborů?'))) return
    setWorking(deepClone(stagedAi))
    await applyConfig('ai-suggestion-confirmed')
  }

  const section = schema[activeSection]
  const sectionValues = working[activeSection] || {}

  return (
    <>
      <article className="panel panel-wide">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('CONFIG STUDIO', 'CONFIG STUDIO')}</span>
            <h2>{Object.keys(schema).map(k => schema[k].label || k).join(', ')}</h2>
          </div>
          <div className="button-row">
            <button className="ghost" type="button" onClick={restoreDefaults}>{tr('Defaults', 'Výchozí')}</button>
            <button className="ghost" type="button" onClick={() => applyLocale('en')}>EN preset</button>
            <button className="ghost" type="button" onClick={() => applyLocale('cz')}>CZ preset</button>
            <button className="ghost" type="button" onClick={previewDiff}>{tr('Preview diff', 'Náhled diffu')}</button>
            <button type="button" onClick={() => applyConfig()}>{tr('Apply changes', 'Použít změny')}</button>
          </div>
        </div>
        <div className="config-shell">
          <div className="config-nav">
            {Object.entries(schema).map(([key, sec]) => (
              <button
                key={key}
                type="button"
                className={key === activeSection ? 'active' : ''}
                onClick={() => setActiveSection(key)}
              >
                <strong>{sec.label || key}</strong>
              </button>
            ))}
          </div>
          <div className="config-editor">
            {section && (
              <>
                <div className="config-meta">
                  <div className="config-chip">Section: {section.label || activeSection}</div>
                  <div className="config-chip">{section.restartRequired ? 'Some changes may require web restart' : 'Live-safe section'}</div>
                </div>
                {(section.fields || []).map(field => (
                  <ConfigFieldEditor
                    key={field.key}
                    field={field}
                    value={sectionValues[field.key]}
                    onChange={v => updateField(field.key, v)}
                    onStatusChange={setStatus}
                    activeSection={activeSection}
                  />
                ))}
              </>
            )}
          </div>
        </div>
      </article>
      <article className="panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">{tr('CONFIG STATE', 'STAV CONFIGU')}</span>
            <h2>{tr('Diff and recovery', 'Diff a obnova')}</h2>
          </div>
        </div>
        <div className="button-row">
          <button className="ghost" type="button" onClick={reload}>{tr('Run reload', 'Spustit reload')}</button>
          <button className="danger" type="button" onClick={rollback}>{tr('Rollback to last backup', 'Rollback na poslední zálohu')}</button>
        </div>
        <div className="status-log ai-output">{status}</div>
        {diffSummary && <div className="diff-summary">{diffSummary}</div>}
        {diffMeta.requiresWebRestart && <div className="diff-warning">⚠ {tr('Web restart required for port/bind changes.', 'Pro změny portu/bind je potřeba restart webu.')}</div>}
        {diffMeta.action && <div className="diff-action">{tr('Recommended', 'Doporučeno')}: {diffMeta.action}</div>}
        {diffChanges.length > 0 && (
          <div className="diff-list">
            {diffChanges.map((c, i) => (
              <div className="diff-item" key={i}>
                <div className="diff-field">{c.section}.{c.field} <span className="diff-impact">{c.impactLabel || c.impact || ''}</span></div>
                <div className="diff-line diff-old">- {formatValue(c.current)}</div>
                <div className="diff-line diff-new">+ {formatValue(c.proposed)}</div>
              </div>
            ))}
          </div>
        )}
        {previewErrors.length > 0 && (
          <div className="diff-errors">
            <strong>{tr('Validation errors', 'Validační chyby')}:</strong>
            <ul>{previewErrors.map((e, i) => <li key={i}>{e}</li>)}</ul>
          </div>
        )}
        <div className="status-log ai-output" style={{ whiteSpace: 'pre-wrap' }}>{preview}</div>
        {stagedAi && (
          <button type="button" onClick={applyAiSuggestion} style={{ marginTop: 12 }}>
            {tr('Apply staged AI suggestion', 'Použít staged AI návrh')}
          </button>
        )}
      </article>
    </>
  )
}

interface FieldProps {
  field: ConfigField
  value: unknown
  onChange: (value: unknown) => void
  onStatusChange: (s: string) => void
  activeSection: string
}

function ConfigFieldEditor({ field, value, onChange, onStatusChange, activeSection }: FieldProps) {
  const showColorHelper = supportsColorHelper(field)

  if (field.type === 'boolean') {
    return (
      <label className="config-field checkbox-row">
        <input type="checkbox" checked={!!value} onChange={e => onChange(e.target.checked)} />
        <div>
          <span>{field.label || field.key}</span>
          <div className="field-description">{field.description || ''}</div>
        </div>
      </label>
    )
  }

  if (field.type === 'select') {
    return (
      <label className="config-field">
        <span>{field.label || field.key}</span>
        <select value={String(value ?? '')} onChange={e => onChange(e.target.value)}>
          {(field.options || []).map(opt => <option key={opt} value={opt}>{opt}</option>)}
        </select>
        <div className="field-description">{field.description || ''}</div>
      </label>
    )
  }

  if (field.type === 'rank-list') {
    return (
      <RankListEditor
        field={field}
        value={value}
        onChange={onChange}
        onStatusChange={onStatusChange}
        activeSection={activeSection}
      />
    )
  }

  if (field.type === 'discord-role-style-list') {
    return (
      <RoleStyleEditor
        field={field}
        value={value}
        onChange={onChange}
        onStatusChange={onStatusChange}
        activeSection={activeSection}
      />
    )
  }

  if (field.type === 'multiline-list' || field.type === 'json') {
    const text = field.type === 'multiline-list'
      ? (Array.isArray(value) ? (value as string[]).join('\n') : '')
      : String(value ?? '')
    const rows = field.type === 'json' ? 12 : 8
    return (
      <label className="config-field">
        <span>{field.label || field.key}</span>
        <textarea
          rows={rows}
          value={text}
          data-config-key={field.key}
          onChange={e => {
            if (field.type === 'multiline-list') {
              onChange(e.target.value.split('\n').map(l => l.trim()).filter(Boolean))
            } else {
              onChange(e.target.value)
            }
          }}
        />
        {showColorHelper && <ColorHelper fieldKey={field.key} rawValue={text} onInsert={v => {
          if (field.type === 'multiline-list') {
            onChange(v.split('\n').map(l => l.trim()).filter(Boolean))
          } else {
            onChange(v)
          }
        }} />}
        {showColorHelper && text.split('\n').map((line, li) => <McTextPreview key={li} text={line} />)}
        <div className="field-description">{field.description || ''}</div>
      </label>
    )
  }

  const inputType = field.type === 'number' ? 'number' : (field.type === 'secret' ? 'password' : 'text')
  return (
    <label className="config-field">
      <span>{field.label || field.key}</span>
      <input
        type={inputType}
        value={String(value ?? '')}
        data-config-key={field.key}
        onChange={e => onChange(field.type === 'number' ? Number(e.target.value || 0) : e.target.value)}
      />
      {showColorHelper && <ColorHelper fieldKey={field.key} rawValue={String(value ?? '')} onInsert={v => onChange(v)} />}
      {showColorHelper && <McTextPreview text={String(value ?? '')} />}
      <div className="field-description">{field.description || ''}</div>
    </label>
  )
}

function supportsColorHelper(field: ConfigField): boolean {
  if (!['text', 'multiline-list'].includes(field.type)) return false
  const key = (field.key || '').toLowerCase()
  const label = (field.label || '').toLowerCase()
  const desc = (field.description || '').toLowerCase()
  const blocked = ['token', 'apikey', 'api key', 'key path', 'guild id', 'channel id', 'role id', 'webhook', 'url', 'hostname', 'bind address', 'host', 'model', 'endpoint', 'secret', 'uuid']
  if (blocked.some(p => key.includes(p.replaceAll(' ', '')) || label.includes(p) || desc.includes(p))) return false
  const positive = ['message', 'format', 'prefix', 'suffix', 'header', 'footer', 'title', 'topic', 'tooltip', 'line', 'announcement', 'label', 'kick', 'hint']
  return desc.includes('color code') || desc.includes('placeholder') || positive.some(p => key.includes(p) || label.includes(p))
}

function ColorHelper({ fieldKey, rawValue, onInsert }: { fieldKey: string, rawValue: string, onInsert: (hex: string) => void }) {
  const [color, setColor] = useState(extractLastHexColor(rawValue) || '#6fe8ff')
  return (
    <div className="color-helper">
      <div className="color-helper-row">
        <input type="color" value={color} onChange={e => setColor(e.target.value)} />
        <button
          type="button"
          className="ghost small"
          onClick={() => {
            const input = document.querySelector(`[data-config-key="${fieldKey}"]`) as HTMLInputElement | HTMLTextAreaElement | null
            const hex = toVoidiumHex(color)
            if (input) {
              const start = input.selectionStart ?? input.value.length
              const end = input.selectionEnd ?? input.value.length
              const newValue = input.value.slice(0, start) + hex + input.value.slice(end)
              onInsert(newValue)
            } else {
              onInsert(rawValue + hex)
            }
          }}
        >Insert RGB</button>
        <div className="color-helper-code">{toVoidiumHex(color)}</div>
      </div>
      <div className="color-helper-note">Inserts Voidium hex format like &amp;#RRGGBB at the cursor.</div>
    </div>
  )
}

interface RankDef {
  type: string
  value: string
  hours: number
  customConditionsText: string
}

function parseRanks(raw: unknown): RankDef[] {
  try {
    const parsed = JSON.parse(String(raw || '[]'))
    if (!Array.isArray(parsed)) return []
    return parsed.map((r: Record<string, unknown>) => ({
      type: String(r.type || 'PREFIX'),
      value: String(r.value || ''),
      hours: Number(r.hours || 0),
      customConditionsText: JSON.stringify(Array.isArray(r.customConditions) ? r.customConditions : [], null, 2),
    }))
  } catch { return [] }
}

function serializeRanks(ranks: RankDef[]): string {
  return JSON.stringify(ranks.map(r => ({
    type: r.type,
    value: r.value,
    hours: Number(r.hours || 0),
    customConditions: JSON.parse(r.customConditionsText || '[]'),
  })), null, 2)
}

function RankListEditor({ field, value, onChange, onStatusChange, activeSection }: FieldProps) {
  const tr = useTr()
  const ranks = parseRanks(value)

  const updateRanks = (newRanks: RankDef[]) => {
    onChange(serializeRanks(newRanks))
    onStatusChange(`Unsaved changes in ${activeSection}.`)
  }

  const addRank = () => {
    updateRanks([...ranks, { type: 'PREFIX', value: '&7[Member] ', hours: 10, customConditionsText: '[]' }])
  }

  const removeRank = (index: number) => {
    updateRanks(ranks.filter((_, i) => i !== index))
  }

  const updateRankProp = (index: number, prop: keyof RankDef, val: string | number) => {
    const updated = [...ranks]
    updated[index] = { ...updated[index], [prop]: val }
    updateRanks(updated)
  }

  return (
    <div className="config-field">
      <span>{field.label || field.key}</span>
      <div className="rank-actions">
        <span className="rank-index">Structured rank editor</span>
        <button type="button" className="ghost small" onClick={addRank}>Add rank</button>
      </div>
      <div className="rank-list">
        {!ranks.length
          ? <div className="feed-item">No rank entries configured.</div>
          : ranks.map((rank, i) => (
            <div className="rank-row" key={i}>
              <div className="rank-actions">
                <span className="rank-index">Rank {i + 1}</span>
                <button type="button" className="ghost small" onClick={() => removeRank(i)}>Remove</button>
              </div>
              <div className="rank-grid">
                <label><span>Type</span>
                  <select value={rank.type} onChange={e => updateRankProp(i, 'type', e.target.value)}>
                    <option value="PREFIX">PREFIX</option>
                    <option value="SUFFIX">SUFFIX</option>
                  </select>
                </label>
                <label><span>Value</span>
                  <input type="text" value={rank.value} onChange={e => updateRankProp(i, 'value', e.target.value)} />
                </label>
                <label><span>Hours</span>
                  <input type="number" value={rank.hours} onChange={e => updateRankProp(i, 'hours', Number(e.target.value))} />
                </label>
              </div>
              <label><span>Custom conditions JSON</span>
                <textarea rows={6} value={rank.customConditionsText} placeholder='[{"type":"KILL","count":100}]' onChange={e => {
                  try { JSON.parse(e.target.value); updateRankProp(i, 'customConditionsText', e.target.value) }
                  catch { onStatusChange('Invalid custom conditions JSON in ranks editor.') }
                }} />
              </label>
              <div className="field-description">
                {tr(
                  'JSON array of extra conditions. Each object: {"type": "...", "count": N}. Types: KILL (mob kills), VISIT (biome visits), BREAK (blocks broken), PLACE (blocks placed). All conditions must be met together with the hours requirement.',
                  'JSON pole extra podmínek. Každý objekt: {"type": "...", "count": N}. Typy: KILL (zabití mobů), VISIT (návštěvy biomů), BREAK (rozbité bloky), PLACE (položené bloky). Všechny podmínky musí být splněny společně s požadavkem na hodiny.'
                )}
              </div>
            </div>
          ))
        }
      </div>
      <div className="field-description">{field.description || ''}</div>
    </div>
  )
}

interface RoleStyle {
  roleId: string
  prefix: string
  suffix: string
  color: string
  priority: number
}

function parseStyles(raw: unknown): RoleStyle[] {
  try {
    const parsed = JSON.parse(String(raw || '{}'))
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return []
    return Object.entries(parsed).map(([roleId, style]) => ({
      roleId,
      prefix: (style as Record<string, unknown>)?.prefix as string || '',
      suffix: (style as Record<string, unknown>)?.suffix as string || '',
      color: (style as Record<string, unknown>)?.color as string || '',
      priority: Number((style as Record<string, unknown>)?.priority || 0),
    }))
  } catch { return [] }
}

function serializeStyles(styles: RoleStyle[]): string {
  const map: Record<string, Omit<RoleStyle, 'roleId'>> = {}
  styles.forEach(s => {
    if (!s.roleId) return
    map[s.roleId] = { prefix: s.prefix, suffix: s.suffix, color: s.color, priority: s.priority }
  })
  return JSON.stringify(map, null, 2)
}

function RoleStyleEditor({ field, value, onChange, onStatusChange, activeSection }: FieldProps) {
  const styles = parseStyles(value)
  const [roles, setRoles] = useState<DiscordRole[]>([])

  useEffect(() => {
    api.discordRoles().then(r => setRoles(r.roles || [])).catch(() => {})
  }, [])

  const updateStyles = (newStyles: RoleStyle[]) => {
    onChange(serializeStyles(newStyles))
    onStatusChange(`Unsaved changes in ${activeSection}.`)
  }

  const addStyle = () => {
    updateStyles([...styles, { roleId: '', prefix: '', suffix: '', color: '', priority: 0 }])
  }

  const removeStyle = (index: number) => {
    updateStyles(styles.filter((_, i) => i !== index))
  }

  const updateStyleProp = (index: number, prop: keyof RoleStyle, val: string | number) => {
    const updated = [...styles]
    updated[index] = { ...updated[index], [prop]: val }
    updateStyles(updated)
  }

  const roleNameById = (id: string) => roles.find(r => r.id === id)?.name || ''

  return (
    <div className="config-field">
      <span>{field.label || field.key}</span>
      <div className="rank-actions">
        <span className="rank-index">Structured Discord role style editor</span>
        <button type="button" className="ghost small" onClick={addStyle}>Add role style</button>
      </div>
      <div className="rank-list">
        {!styles.length
          ? <div className="feed-item">No Discord role styles configured.</div>
          : styles.map((style, i) => {
            const roleName = roleNameById(style.roleId)
            const roleLabel = roleName ? ` (${roleName})` : ''
            return (
              <div className="rank-row" key={i}>
                <div className="rank-actions">
                  <span className="rank-index">Role style {i + 1}{roleLabel}</span>
                  <button type="button" className="ghost small" onClick={() => removeStyle(i)}>Remove</button>
                </div>
                <div className="rank-grid">
                  <label><span>Role ID</span>
                    <input type="text" value={style.roleId} onChange={e => updateStyleProp(i, 'roleId', e.target.value)} />
                  </label>
                  <label><span>Prefix</span>
                    <input type="text" value={style.prefix} onChange={e => updateStyleProp(i, 'prefix', e.target.value)} />
                  </label>
                  <label><span>Priority</span>
                    <input type="number" value={style.priority} onChange={e => updateStyleProp(i, 'priority', Number(e.target.value))} />
                  </label>
                </div>
                <div className="rank-grid">
                  <label><span>Suffix</span>
                    <input type="text" value={style.suffix} onChange={e => updateStyleProp(i, 'suffix', e.target.value)} />
                  </label>
                  <label><span>Color</span>
                    <input type="text" value={style.color} onChange={e => updateStyleProp(i, 'color', e.target.value)} />
                  </label>
                  <div />
                </div>
              </div>
            )
          })
        }
      </div>
      <div className="field-description">{field.description || ''}</div>
    </div>
  )
}
