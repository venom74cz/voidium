import type {
  DashboardData,
  ConfigSchema,
  ConfigValues,
  ConfigDiffResult,
  ConfigApplyResult,
  ConfigPreviewResult,
  AiSuggestResult,
  DiscordRole,
  DimensionHeatmap,
  PlayerAiConversation,
} from './types'
import {
  demoAction,
  demoActionJson,
  demoAiAdmin,
  demoAiPlayers,
  demoAiSuggest,
  demoConfigApply,
  demoConfigDefaults,
  demoConfigDiff,
  demoConfigLocale,
  demoConfigPreview,
  demoConfigReload,
  demoConfigRollback,
  demoConfigSchema,
  demoConfigSchemaExport,
  demoConfigValues,
  demoConsoleExecute,
  demoDashboard,
  demoDiscordRoleList,
  demoServerProperties,
  demoServerPropertiesSave,
} from './mockData'

class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function isLocalDemoMode(): boolean {
  if (typeof window === 'undefined') return false
  const host = window.location.hostname
  const port = window.location.port
  const query = new URLSearchParams(window.location.search)
  if (query.get('live') === '1' || query.get('demo') === '0') return false
  if (query.get('demo') === '1') return true
  const localHost = host === 'localhost' || host === '127.0.0.1'
  const previewPort = port === '4173' || port === '4174' || port === '5173' || port === '5174'
  return localHost && previewPort
}

function shouldUseDemoFallback(error: unknown): boolean {
  if (!isLocalDemoMode()) return false
  if (error instanceof ApiError) {
    return error.status === 404 || error.status >= 500
  }
  return true
}

async function withDemoFallback<T>(request: () => Promise<T>, fallback: () => T | Promise<T>): Promise<T> {
  if (isLocalDemoMode()) {
    return fallback()
  }
  try {
    return await request()
  } catch (error) {
    if (!shouldUseDemoFallback(error)) {
      throw error
    }
    return fallback()
  }
}

async function checkedJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const data = await res.json().catch(() => null)
    throw new ApiError(data?.message || `HTTP ${res.status}`, res.status)
  }
  return res.json() as Promise<T>
}

async function safeFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  return checkedJson<T>(res)
}

async function post<T>(url: string, body?: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
    headers: body !== undefined ? { 'Content-Type': 'application/json;charset=UTF-8' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  return checkedJson<T>(res)
}

async function postForm<T>(url: string, params: Record<string, string>): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
    body: new URLSearchParams(params).toString(),
  })
  return checkedJson<T>(res)
}

export const api = {
  isDemoMode: isLocalDemoMode,

  dashboard: (): Promise<DashboardData> =>
    withDemoFallback(
      () => safeFetch('/api/dashboard', { credentials: 'same-origin' }),
      () => demoDashboard(),
    ),

  configSchema: (): Promise<ConfigSchema> =>
    withDemoFallback(
      () => safeFetch('/api/config/schema', { credentials: 'same-origin' }),
      () => demoConfigSchema(),
    ),

  configSchemaExport: (): Promise<Record<string, unknown>> =>
    withDemoFallback(
      () => safeFetch('/api/config/schema/export', { credentials: 'same-origin' }),
      () => demoConfigSchemaExport(),
    ),

  configValues: (): Promise<ConfigValues> =>
    withDemoFallback(
      () => safeFetch('/api/config/values', { credentials: 'same-origin' }),
      () => demoConfigValues(),
    ),

  configDefaults: (section: string, values: ConfigValues) =>
    withDemoFallback(
      () => post<{ message: string; values?: ConfigValues }>('/api/config/defaults', { section, values }),
      () => demoConfigDefaults(section, values),
    ),

  configLocale: (section: string, locale: string, values: ConfigValues) =>
    withDemoFallback(
      () => post<{ message: string; values?: ConfigValues }>('/api/config/locale', { section, locale, values }),
      () => demoConfigLocale(section, locale, values),
    ),

  configPreview: (values: ConfigValues) =>
    withDemoFallback(
      () => post<ConfigPreviewResult>('/api/config/preview', { values }),
      () => demoConfigPreview(values),
    ),

  configDiff: (values: ConfigValues) =>
    withDemoFallback(
      () => post<ConfigDiffResult>('/api/config/diff', { values }),
      () => demoConfigDiff(values),
    ),

  configApply: (values: ConfigValues, source = 'manual-web') =>
    withDemoFallback(
      () => post<ConfigApplyResult>('/api/config/apply', { values, source }),
      () => demoConfigApply(values, source),
    ),

  configRollback: () =>
    withDemoFallback(
      () => post<{ message: string; rolledBack?: boolean }>('/api/config/rollback'),
      () => demoConfigRollback(),
    ),

  configReload: () =>
    withDemoFallback(
      () => post<{ message: string }>('/api/config/reload'),
      () => demoConfigReload(),
    ),

  action: (action: string, extra: Record<string, string> = {}) =>
    withDemoFallback(
      () => postForm<{ message: string }>('/api/action', { action, ...extra }),
      () => demoAction(action, extra),
    ),

  actionJson: (action: string, extra: Record<string, string> = {}) =>
    withDemoFallback(
      () => postForm<{ message: string; dimensions?: DimensionHeatmap; result?: Record<string, number>; lines?: string[]; player?: string }>('/api/action', { action, ...extra }),
      () => demoActionJson(action, extra),
    ),

  aiAdmin: (payload: {
    message: string
    includeServer: boolean
    includePlayers: boolean
    includeModules: boolean
    includeConfigs: boolean
    configFiles: string[]
  }) =>
    withDemoFallback(
      () => post<{ answer?: string; message?: string }>('/api/ai/admin', payload),
      () => demoAiAdmin(payload),
    ),

  aiSuggest: (payload: {
    message: string
    includeServer: boolean
    includePlayers: boolean
    includeModules: boolean
    includeConfigs: boolean
    configFiles: string[]
  }) =>
    withDemoFallback(
      () => post<AiSuggestResult>('/api/ai/admin/suggest', payload),
      () => demoAiSuggest(),
    ),

  aiPlayers: (): Promise<{ conversations: PlayerAiConversation[] }> =>
    withDemoFallback(
      () => safeFetch('/api/ai/players', { credentials: 'same-origin' }),
      () => demoAiPlayers(),
    ),

  discordRoles: (): Promise<{ roles: DiscordRole[] }> =>
    withDemoFallback(
      () => safeFetch('/api/discord/roles', { credentials: 'same-origin' }),
      () => demoDiscordRoleList(),
    ),

  consoleExecute: (command: string) =>
    withDemoFallback(
      () => post<{ message: string }>('/api/console/execute', { command }),
      () => demoConsoleExecute(command),
    ),

  logout: () =>
    withDemoFallback(
      () => safeFetch('/api/logout', { method: 'POST', credentials: 'same-origin' }),
      () => ({ message: 'No authenticated session exists in demo mode.' }),
    ),

  serverProperties: (): Promise<{ properties: Record<string, string> }> =>
    withDemoFallback(
      () => safeFetch('/api/server-properties', { credentials: 'same-origin' }),
      () => demoServerProperties(),
    ),

  serverPropertiesSave: (properties: Record<string, string>) =>
    withDemoFallback(
      () => post<{ message: string; changedKeys?: string[] }>('/api/server-properties', { properties }),
      () => demoServerPropertiesSave(properties),
    ),
}
