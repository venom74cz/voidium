import { useState, useEffect, useCallback, useRef } from 'react'
import { useTr } from '../i18n'
import { useToast } from './Toast'
import { api } from '../api'
import { deepClone, extractLastHexColor, toVoidiumHex, formatValue } from '../utils'
import { McTextPreview } from './McTextPreview'
import type { ConfigSchema, ConfigValues, ConfigField, ConfigDiffChange, DiscordRole } from '../types'

export interface ConfigStudioProps {
  sectionFilter: string
  title?: string
  titleCz?: string
  eyebrow?: string
  eyebrowCz?: string
}

export function ConfigStudio({ sectionFilter, title, titleCz, eyebrow, eyebrowCz }: ConfigStudioProps) {
  const tr = useTr()
  const { toast } = useToast()
  const sectionKey = sectionFilter
  const [schema, setSchema] = useState<ConfigSchema>({})
  const [values, setValues] = useState<ConfigValues>({})
  const [working, setWorking] = useState<ConfigValues>({})
  const [status, setStatus] = useState(tr('Settings loaded.', 'Nastavení načteno.'))
  const [diffChanges, setDiffChanges] = useState<ConfigDiffChange[]>([])
  const [diffSummary, setDiffSummary] = useState('')
  const [diffMeta, setDiffMeta] = useState<{ requiresWebRestart?: boolean; action?: string }>({})
  const [previewErrors, setPreviewErrors] = useState<string[]>([])
  const [preview, setPreview] = useState(tr('No preview generated yet.', 'Zatím nebyl vygenerován náhled.'))
  const [stagedAi, setStagedAi] = useState<ConfigValues | null>(null)
  const [loaded, setLoaded] = useState(false)

  const fetchStudio = useCallback(async () => {
    try {
      const [s, v] = await Promise.all([api.configSchema(), api.configValues()])
      setSchema(s)
      setValues(v)
      setWorking(deepClone(v))
      setStagedAi(null)
      setLoaded(true)
      if (!s[sectionKey]) {
        setStatus(tr(`Missing settings section: ${sectionKey}`, `Chybí sekce nastavení: ${sectionKey}`))
        return
      }
      setStatus(tr('Settings synced with current files.', 'Nastavení je synchronizované s aktuálními soubory.'))
    } catch (e) {
      setLoaded(true)
      const msg = e instanceof Error ? e.message : 'Failed to load config'
      setStatus(msg)
      toast(msg, 'error')
    }
  }, [sectionKey, toast, tr])

  useEffect(() => { fetchStudio() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const updateField = (key: string, value: unknown) => {
    setWorking(prev => {
      const next = deepClone(prev)
      if (!next[sectionKey]) next[sectionKey] = {}
      next[sectionKey][key] = value
      return next
    })
    setStatus(tr(`Unsaved changes in ${sectionKey}.`, `Neuložené změny v sekci ${sectionKey}.`))
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
    setPreview(previewRes.previews?.[sectionKey] || tr('No preview available for this section.', 'Žádný náhled pro tuto sekci.'))
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
    const result = await api.configDefaults(sectionKey, working)
    if (result.values) { setWorking(result.values) }
    setStatus(result.message || 'Defaults restored.')
    toast(result.message || tr('Defaults restored.', 'Výchozí hodnoty obnoveny.'), 'success')
  }

  const applyLocale = async (locale: string) => {
    const result = await api.configLocale(sectionKey, locale, working)
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
    setStatus(result.message || 'Reload requested.')
    toast(result.message || tr('Reload requested.', 'Reload vyzadan.'), 'success')
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

  const section = schema[sectionKey]
  const sectionValues = working[sectionKey] || {}
  const panelTitle = tr(title || `${section?.label || sectionKey} settings`, titleCz || `${section?.label || sectionKey}`)
  const panelEyebrow = tr(eyebrow || 'MODULE SETTINGS', eyebrowCz || 'NASTAVENÍ MODULU')

  return (
    <article className="panel panel-wide">
      <div className="panel-head">
        <div>
          <span className="eyebrow">{panelEyebrow}</span>
          <h2>{panelTitle}</h2>
        </div>
        <div className="button-row">
          <button className="ghost" type="button" onClick={restoreDefaults}>{tr('Restore defaults', 'Obnovit výchozí')}</button>
          <button className="ghost" type="button" onClick={() => applyLocale('en')}>EN preset</button>
          <button className="ghost" type="button" onClick={() => applyLocale('cz')}>CZ preset</button>
          <button className="ghost" type="button" onClick={previewDiff}>{tr('Preview changes', 'Náhled změn')}</button>
          <button type="button" onClick={() => applyConfig()}>{tr('Save settings', 'Uložit nastavení')}</button>
        </div>
      </div>

      {!loaded ? (
        <div className="loading-state">{tr('Loading settings...', 'Načítání nastavení...')}</div>
      ) : !section ? (
        <div className="empty-state">{tr(`Settings section ${sectionKey} is not available.`, `Sekce nastavení ${sectionKey} není dostupná.`)}</div>
      ) : (
        <div className="config-editor">
          <div className="config-meta">
            <div className="config-chip">{section.label || sectionKey}</div>
            <div className="config-chip">{section.restartRequired ? tr('May require web restart', 'Může vyžadovat restart webu') : tr('Live-safe section', 'Live-safe sekce')}</div>
            <div className="config-chip">{tr(`${section.fields.length} fields`, `${section.fields.length} polí`)}</div>
          </div>
          {(section.fields || []).map(field => (
            <ConfigFieldEditor
              key={field.key}
              field={field}
              value={sectionValues[field.key]}
              onChange={v => updateField(field.key, v)}
              onStatusChange={setStatus}
              activeSection={sectionKey}
            />
          ))}
        </div>
      )}

      <div className="status-log ai-output">{status}</div>
      {diffSummary && <div className="diff-summary">{diffSummary}</div>}
      {diffMeta.requiresWebRestart && <div className="diff-warning">{tr('Web restart required for port/bind changes.', 'Pro změny portu nebo bind adresy je potřeba restart webu.')}</div>}
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
      <div className="button-row">
        <button className="ghost" type="button" onClick={reload}>{tr('Run reload', 'Spustit reload')}</button>
        <button className="danger" type="button" onClick={rollback}>{tr('Rollback latest backup', 'Rollback poslední zálohy')}</button>
        {stagedAi && (
          <button type="button" onClick={applyAiSuggestion}>
            {tr('Apply staged AI suggestion', 'Použít staged AI návrh')}
          </button>
        )}
      </div>
    </article>
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

function formatColorInsertion(value: string, mode: 'voidium' | 'css'): string {
  return mode === 'css'
    ? String(value || '#A855F7').toUpperCase()
    : toVoidiumHex(value)
}

function ColorHelper({ fieldKey, rawValue, onInsert, mode = 'voidium', applyMode = 'insert' }: { fieldKey: string, rawValue: string, onInsert: (hex: string) => void, mode?: 'voidium' | 'css', applyMode?: 'insert' | 'replace' }) {
  const [color, setColor] = useState(extractLastHexColor(rawValue) || '#a855f7')
  const formatted = formatColorInsertion(color, mode)

  useEffect(() => {
    const next = extractLastHexColor(rawValue) || '#a855f7'
    setColor(current => current.toLowerCase() === next.toLowerCase() ? current : next)
  }, [rawValue])

  return (
    <div className="color-helper">
      <div className="color-helper-row">
        <input type="color" value={color} onChange={e => setColor(e.target.value)} />
        <button
          type="button"
          className="ghost small"
          onClick={() => {
            const input = document.querySelector(`[data-config-key="${fieldKey}"]`) as HTMLInputElement | HTMLTextAreaElement | null
            const hex = formatColorInsertion(color, mode)
            if (applyMode === 'replace') {
              onInsert(hex)
              return
            }
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
        <div className="color-helper-code">{formatted}</div>
      </div>
      <div className="color-helper-note">
        {mode === 'css'
          ? applyMode === 'replace'
            ? 'Replaces the field with CSS/Discord hex format like #RRGGBB.'
            : 'Inserts CSS/Discord hex format like #RRGGBB at the cursor.'
          : 'Inserts Voidium hex format like &#RRGGBB at the cursor.'}
      </div>
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
                  <input data-config-key={`rank-${i}-value`} type="text" value={rank.value} onChange={e => updateRankProp(i, 'value', e.target.value)} />
                  <ColorHelper fieldKey={`rank-${i}-value`} rawValue={rank.value} onInsert={v => updateRankProp(i, 'value', v)} />
                  <McTextPreview text={rank.value} />
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

type RoleRenderTarget = 'prefix' | 'suffix'

const ROLE_STYLE_PREVIEW_PLAYER = 'VOIDIVIDE'

function stripMcFormatting(raw: string): string {
  return String(raw || '')
    .replace(/§x(§[0-9a-fA-F]){6}/g, '')
    .replace(/[&§]#[0-9a-fA-F]{6}/g, '')
    .replace(/[&§][0-9a-fk-or]/gi, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function getRoleRenderTarget(style: RoleStyle): RoleRenderTarget {
  return style.suffix.trim() && !style.prefix.trim() ? 'suffix' : 'prefix'
}

function getRoleRenderValue(style: RoleStyle, target: RoleRenderTarget): string {
  return target === 'suffix' ? style.suffix : style.prefix
}

function setRoleRenderValue(style: RoleStyle, target: RoleRenderTarget, value: string): RoleStyle {
  return target === 'suffix'
    ? { ...style, prefix: '', suffix: value }
    : { ...style, prefix: value, suffix: '' }
}

function normalizeRoleRenderSpacing(value: string, target: RoleRenderTarget): string {
  const trimmed = String(value || '').trim()
  if (!trimmed) return ''
  return target === 'suffix' ? ` ${trimmed}` : `${trimmed} `
}

function buildAutoRoleRenderText(role: DiscordRole | undefined, target: RoleRenderTarget, fallback = ''): string {
  const fallbackName = stripMcFormatting(fallback)
    .replace(/^\s*\[?/, '')
    .replace(/\]?\s*$/, '')
  const roleName = role?.name?.trim() || fallbackName || 'Role'
  const sourceColor = role?.color || extractLastHexColor(fallback)
  const colorCode = sourceColor ? toVoidiumHex(sourceColor) : ''
  const badge = `${colorCode}[${roleName}]`
  return target === 'suffix' ? ` ${badge}` : `${badge} `
}

function buildRoleStylePreviewText(style: RoleStyle, playerName = ROLE_STYLE_PREVIEW_PLAYER): string {
  return `${style.prefix || ''}${playerName}${style.suffix || ''}`
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
  const tr = useTr()
  const styles = parseStyles(value)
  const [roles, setRoles] = useState<DiscordRole[]>([])

  useEffect(() => {
    api.discordRoles()
      .then(r => setRoles((r.roles || []).slice().sort((left, right) => {
        const positionDelta = Number(right.position || 0) - Number(left.position || 0)
        return positionDelta || String(left.name || '').localeCompare(String(right.name || ''))
      })))
      .catch(() => {})
  }, [])

  const updateStyles = (newStyles: RoleStyle[]) => {
    onChange(serializeStyles(newStyles))
    onStatusChange(`Unsaved changes in ${activeSection}.`)
  }

  const updateStyle = (index: number, updater: (current: RoleStyle) => RoleStyle) => {
    const updated = [...styles]
    updated[index] = updater(updated[index])
    updateStyles(updated)
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

  const roleById = (id: string) => roles.find(role => role.id === id)

  return (
    <div className="config-field">
      <span>{field.label || field.key}</span>
      <div className="rank-actions role-style-toolbar">
        <span className="rank-index">Structured Discord role style editor</span>
        <button type="button" className="ghost small" onClick={addStyle}>Add role style</button>
      </div>
      <div className="rank-list role-style-list">
        {!styles.length
          ? <div className="feed-item">{tr('No Discord role styles configured.', 'Zatim nejsou nastavene zadne Discord role styly.')}</div>
          : styles.map((style, i) => {
            const selectedRole = roleById(style.roleId)
            const renderTarget = getRoleRenderTarget(style)
            const renderValue = getRoleRenderValue(style, renderTarget)
            const previewText = buildRoleStylePreviewText(style)
            const roleLabel = selectedRole?.name || (style.roleId ? tr('Custom role', 'Vlastni role') : tr('No role selected', 'Role neni vybrana'))
            const roleOptions = style.roleId && !selectedRole
              ? [{ id: style.roleId, name: `${tr('Custom role', 'Vlastni role')} (${style.roleId})`, color: style.color }, ...roles]
              : roles
            return (
              <div className="rank-row role-style-card" key={`${style.roleId || 'role'}-${i}`}>
                <div className="rank-actions">
                  <span className="rank-index">{tr(`Role style ${i + 1}`, `Role style ${i + 1}`)}{selectedRole?.name ? ` (${selectedRole.name})` : ''}</span>
                  <button type="button" className="ghost small" onClick={() => removeStyle(i)}>Remove</button>
                </div>
                <div className="role-style-header-grid">
                  <label className="role-style-field">
                    <span>{tr('Discord role', 'Discord role')}</span>
                    {roles.length ? (
                      <select
                        value={style.roleId}
                        onChange={e => {
                          const nextRole = roleById(e.target.value)
                          updateStyle(i, current => {
                            const currentTarget = getRoleRenderTarget(current)
                            const currentValue = getRoleRenderValue(current, currentTarget)
                            const nextValue = e.target.value
                              ? buildAutoRoleRenderText(nextRole, currentTarget, currentValue)
                              : currentValue
                            return setRoleRenderValue({
                              ...current,
                              roleId: e.target.value,
                              color: nextRole?.color || current.color,
                              priority: Number(current.priority || 0) > 0 ? current.priority : Number(nextRole?.position || 0),
                            }, currentTarget, nextValue)
                          })
                        }}
                      >
                        <option value="">{tr('Select available role', 'Vyber dostupnou roli')}</option>
                        {roleOptions.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}
                      </select>
                    ) : (
                      <input
                        type="text"
                        value={style.roleId}
                        placeholder={tr('Discord role ID', 'Discord role ID')}
                        onChange={e => updateStyleProp(i, 'roleId', e.target.value)}
                      />
                    )}
                    {selectedRole && (
                      <div className="role-style-role-meta">
                        <span className="role-style-swatch" style={{ backgroundColor: selectedRole.color || '#99AAB5' }} />
                        <div className="role-style-role-copy">
                          <strong>{selectedRole.name}</strong>
                          <span>ID {selectedRole.id}{selectedRole.position ? ` · #${selectedRole.position}` : ''}</span>
                        </div>
                      </div>
                    )}
                    {roles.length > 0 && style.roleId && !selectedRole && (
                      <input
                        type="text"
                        value={style.roleId}
                        placeholder={tr('Manual role ID', 'Manualni role ID')}
                        onChange={e => updateStyleProp(i, 'roleId', e.target.value)}
                      />
                    )}
                    {!roles.length && <div className="field-description">{tr('Role list is unavailable, so the editor falls back to manual role ID input.', 'Seznam roli neni dostupny, editor proto pouziva manualni role ID.')}</div>}
                  </label>
                  <label className="role-style-field"><span>{tr('Render target', 'Cil renderu')}</span>
                    <select
                      value={renderTarget}
                      onChange={e => {
                        const nextTarget = e.target.value as RoleRenderTarget
                        updateStyle(i, current => {
                          const currentTarget = getRoleRenderTarget(current)
                          const currentValue = getRoleRenderValue(current, currentTarget)
                          const nextValue = currentValue
                            ? normalizeRoleRenderSpacing(currentValue, nextTarget)
                            : buildAutoRoleRenderText(selectedRole, nextTarget)
                          return setRoleRenderValue(current, nextTarget, nextValue)
                        })
                      }}
                    >
                      <option value="prefix">{tr('Prefix before player', 'Prefix pred hracem')}</option>
                      <option value="suffix">{tr('Suffix after player', 'Suffix za hracem')}</option>
                    </select>
                    <div className="field-description">{tr('Changing this selector rewrites the live prefix/suffix slot automatically.', 'Zmena tohoto selectoru automaticky prepise aktivni prefix nebo suffix slot.')}</div>
                  </label>
                  <label className="role-style-field role-style-priority"><span>{tr('Priority', 'Priorita')}</span>
                    <input type="number" value={style.priority} onChange={e => updateStyleProp(i, 'priority', Number(e.target.value))} />
                  </label>
                </div>
                <div className="role-style-body-grid">
                  <label className="role-style-field role-style-render-field"><span>{tr('Render text', 'Text renderu')}</span>
                    <input
                      data-config-key={`role-style-${i}-render`}
                      type="text"
                      value={renderValue}
                      placeholder={buildAutoRoleRenderText(selectedRole, renderTarget)}
                      onChange={e => updateStyle(i, current => setRoleRenderValue(current, getRoleRenderTarget(current), e.target.value))}
                    />
                    <ColorHelper
                      fieldKey={`role-style-${i}-render`}
                      rawValue={renderValue}
                      onInsert={v => updateStyle(i, current => setRoleRenderValue(current, getRoleRenderTarget(current), v))}
                    />
                    <McTextPreview text={previewText} />
                    <div className="field-description">{tr('Edit the visible rank badge text here. The selected target decides whether it is stored as a prefix or suffix.', 'Zde upravis viditelny text badge. Vybrany cil urci, zda se ulozi jako prefix nebo suffix.')}</div>
                  </label>
                  <label className="role-style-field role-style-color-field"><span>{tr('Discord role color', 'Barva Discord role')}</span>
                    <input data-config-key={`role-style-${i}-color`} type="text" value={style.color} onChange={e => updateStyleProp(i, 'color', e.target.value)} />
                    <ColorHelper fieldKey={`role-style-${i}-color`} rawValue={style.color} onInsert={v => updateStyleProp(i, 'color', v)} mode="css" applyMode="replace" />
                    <RoleStylePreview style={style} roleName={roleLabel} />
                  </label>
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

function RoleStylePreview({ style, roleName }: { style: RoleStyle, roleName: string }) {
  const cssColor = /^#[0-9a-fA-F]{6}$/.test(style.color) ? style.color : null
  const swatchColor = cssColor || '#99AAB5'
  const colorLabel = cssColor ? cssColor.toUpperCase() : 'No hex color set'

  if (!roleName && !cssColor) return null

  return (
    <div className="role-style-preview">
      <div className="role-style-chip">
        <span className="role-style-swatch" style={{ backgroundColor: swatchColor }} />
        <div className="role-style-chip-copy">
          <span className="role-style-chip-label">{roleName}</span>
          <strong className="role-style-chip-text" style={cssColor ? { color: cssColor } : undefined}>{colorLabel}</strong>
        </div>
      </div>
    </div>
  )
}
