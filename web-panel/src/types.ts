export interface DashboardData {
  serverName: string
  baseUrl: string
  publicAccessUrl: string
  version: string
  latestVersion?: string | null
  updateAvailable?: boolean
  updateUrl?: string | null
  serverIconUrl?: string | null
  onlinePlayers: number
  maxPlayers: number
  tps: number
  mspt: number
  uptime: string
  memoryUsedMb: number
  memoryMaxMb: number
  memoryUsagePercent: number
  nextRestart: string
  maintenanceMode: boolean
  timers: Timer[]
  tickets: TicketData
  voteQueue: VoteQueueData
  players: Player[]
  modules: Module[]
  alerts: string[]
  history: HistoryPoint[]
  ai: AiCapabilities
  chatFeed: ChatEntry[]
  consoleFeed: ConsoleEntry[]
  auditFeed: AuditEntry[]
  systemInfo?: SystemInfo
}

export interface SystemInfo {
  osName?: string
  osArch?: string
  availableProcessors?: number
  cpuLoad?: number
  processCpuLoad?: number
  systemRamTotalMb?: number
  systemRamUsedMb?: number
  systemRamPercent?: number
  diskTotalGb?: number
  diskUsedGb?: number
  diskPercent?: number
}

export interface Timer {
  id: string
  title: string
  subtitle: string
  remainingSeconds: number
  totalSeconds: number
  tone: 'danger' | 'accent' | 'mint'
}

export interface Player {
  name: string
  uuid: string
  ping: number
  linked: boolean
  discordId?: string | null
}

export interface Module {
  name: string
  enabled: boolean
  detail: string
}

export interface TicketData {
  open: number
  items: TicketItem[]
}

export interface TicketItem {
  channelId: string
  player: string
  cachedMessages: number
  previewLines: string[]
}

export interface VoteQueueData {
  total: number
  players: VotePlayer[]
}

export interface VotePlayer {
  player: string
  count: number
  latestService: string
  latestQueuedAt: string
  latestVoteAt: string
  online: boolean
}

export interface HistoryPoint {
  timestamp: number
  players: number
  tps: number
}

export interface AiCapabilities {
  enabled: boolean
  redactsSensitiveValues: boolean
  configFiles: string[]
  history: AiHistoryEntry[]
}

export interface AiHistoryEntry {
  role: string
  content: string
}

export interface ChatEntry {
  sender: string
  message: string
}

export interface ConsoleEntry {
  level: string
  logger: string
  message: string
}

export interface AuditEntry {
  action: string
  detail: string
  timestamp: string
  source: string
  success: boolean
}

export interface ConfigSchema {
  [section: string]: {
    label: string
    restartRequired?: boolean
    fields: ConfigField[]
  }
}

export interface ConfigField {
  key: string
  label: string
  description: string
  type: 'text' | 'number' | 'boolean' | 'secret' | 'select' | 'multiline-list' | 'json' | 'rank-list' | 'discord-role-style-list'
  options?: string[]
}

export type ConfigValues = Record<string, Record<string, unknown>>

export interface ConfigDiffChange {
  section: string
  field: string
  current: unknown
  proposed: unknown
  impact?: string
  impactLabel?: string
}

export interface ConfigDiffResult {
  summary: string
  changes: ConfigDiffChange[]
  requiresWebRestart?: boolean
  recommendedAction?: { label: string }
}

export interface ConfigApplyResult {
  message: string
  applied: boolean
  validationErrors?: string[]
  recommendedAction?: { label: string }
}

export interface ConfigPreviewResult {
  previews: Record<string, string>
  validationErrors?: string[]
}

export interface AiSuggestResult {
  summary: string
  diffPreview: string
  staged: boolean
  values?: ConfigValues
  stagedDiffPreview?: string
  stagedSummary?: string
  warnings?: string[]
}

export interface DiscordRole {
  id: string
  name: string
  color?: string
  position?: number
}

export interface DimensionHeatmap {
  [dimension: string]: {
    items: number
    mobs: number
    xpOrbs: number
    arrows: number
    total: number
  }
}

export interface PlayerAiConversation {
  uuid: string
  turns: number
  history: AiHistoryEntry[]
}
