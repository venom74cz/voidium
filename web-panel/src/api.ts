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

async function checkedJson<T>(res: Response): Promise<T> {
  const data = await res.json()
  if (!res.ok) {
    throw new Error(data?.message || `HTTP ${res.status}`)
  }
  return data as T
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
  dashboard: (): Promise<DashboardData> =>
    fetch('/api/dashboard', { credentials: 'same-origin' }).then(r => checkedJson(r)),

  configSchema: (): Promise<ConfigSchema> =>
    fetch('/api/config/schema', { credentials: 'same-origin' }).then(r => checkedJson(r)),

  configSchemaExport: (): Promise<Record<string, unknown>> =>
    fetch('/api/config/schema/export', { credentials: 'same-origin' }).then(r => checkedJson(r)),

  configValues: (): Promise<ConfigValues> =>
    fetch('/api/config/values', { credentials: 'same-origin' }).then(r => checkedJson(r)),

  configDefaults: (section: string, values: ConfigValues) =>
    post<{ message: string; values?: ConfigValues }>('/api/config/defaults', { section, values }),

  configLocale: (section: string, locale: string, values: ConfigValues) =>
    post<{ message: string; values?: ConfigValues }>('/api/config/locale', { section, locale, values }),

  configPreview: (values: ConfigValues) =>
    post<ConfigPreviewResult>('/api/config/preview', { values }),

  configDiff: (values: ConfigValues) =>
    post<ConfigDiffResult>('/api/config/diff', { values }),

  configApply: (values: ConfigValues, source = 'manual-web') =>
    post<ConfigApplyResult>('/api/config/apply', { values, source }),

  configRollback: () =>
    post<{ message: string; rolledBack?: boolean }>('/api/config/rollback'),

  configReload: () =>
    post<{ message: string }>('/api/config/reload'),

  action: (action: string, extra: Record<string, string> = {}) =>
    postForm<{ message: string }>('/api/action', { action, ...extra }),

  actionJson: (action: string, extra: Record<string, string> = {}) =>
    postForm<{ message: string; dimensions?: DimensionHeatmap; result?: Record<string, number>; lines?: string[]; player?: string }>('/api/action', { action, ...extra }),

  aiAdmin: (payload: {
    message: string
    includeServer: boolean
    includePlayers: boolean
    includeModules: boolean
    includeConfigs: boolean
    configFiles: string[]
  }) =>
    post<{ answer?: string; message?: string }>('/api/ai/admin', payload),

  aiSuggest: (payload: {
    message: string
    includeServer: boolean
    includePlayers: boolean
    includeModules: boolean
    includeConfigs: boolean
    configFiles: string[]
  }) =>
    post<AiSuggestResult>('/api/ai/admin/suggest', payload),

  aiPlayers: (): Promise<{ conversations: PlayerAiConversation[] }> =>
    fetch('/api/ai/players', { credentials: 'same-origin' }).then(r => checkedJson(r)),

  discordRoles: (): Promise<{ roles: DiscordRole[] }> =>
    fetch('/api/discord/roles', { credentials: 'same-origin' }).then(r => checkedJson(r)),

  consoleExecute: (command: string) =>
    post<{ message: string }>('/api/console/execute', { command }),

  logout: () =>
    fetch('/api/logout', { method: 'POST', credentials: 'same-origin' }),

  serverProperties: (): Promise<{ properties: Record<string, string> }> =>
    fetch('/api/server-properties', { credentials: 'same-origin' }).then(r => checkedJson(r)),

  serverPropertiesSave: (properties: Record<string, string>) =>
    post<{ message: string; changedKeys?: string[] }>('/api/server-properties', { properties }),
}
