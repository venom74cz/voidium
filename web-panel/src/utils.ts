export function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

export function formatDurationCompact(totalSeconds: number): string {
  const seconds = Math.max(0, Number(totalSeconds || 0))
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (days > 0) return `${days}d ${hours}h ${minutes}m`
  if (hours > 0) return `${hours}h ${minutes}m ${secs}s`
  return `${minutes}m ${secs}s`
}

export function formatTimerVerbose(totalSeconds: number): string {
  const seconds = Math.max(0, Number(totalSeconds || 0))
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (hours > 0) return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  return `${minutes}:${String(secs).padStart(2, '0')}`
}

export function formatNumber(value: number, digits: number): string {
  return Number(value || 0).toFixed(digits)
}

export function formatChartTime(timestamp: number): string {
  if (!timestamp) return '--:--'
  return new Date(Number(timestamp)).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export function average(values: number[]): number {
  if (!values.length) return 0
  return values.reduce((sum, v) => sum + Number(v || 0), 0) / values.length
}

export function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

export function extractLastHexColor(value: string): string | null {
  const matches = String(value || '').match(/&#([0-9a-fA-F]{6})/g)
  if (!matches || !matches.length) return null
  return '#' + matches[matches.length - 1].slice(2)
}

export function toVoidiumHex(value: string): string {
  return `&#${String(value || '#6fe8ff').replace('#', '').toUpperCase()}`
}

export function formatValue(value: unknown): string {
  return Array.isArray(value) ? value.join(' | ') : String(value)
}
