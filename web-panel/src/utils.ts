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

const LEGACY_MC_COLORS: Record<string, string> = {
  '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
  '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
  '8': '#555555', '9': '#5555FF', a: '#55FF55', b: '#55FFFF',
  c: '#FF5555', d: '#FF55FF', e: '#FFFF55', f: '#FFFFFF',
}

export function extractLastHexColor(value: string): string | null {
  const source = String(value || '')
  const prefixedHexMatches = [...source.matchAll(/&#([0-9a-fA-F]{6})/g)]
  if (prefixedHexMatches.length) {
    return '#' + prefixedHexMatches[prefixedHexMatches.length - 1][1]
  }

  const plainHexMatches = [...source.matchAll(/(^|[^&])#([0-9a-fA-F]{6})/g)]
  if (plainHexMatches.length) {
    return '#' + plainHexMatches[plainHexMatches.length - 1][2]
  }

  const legacyMatches = [...source.matchAll(/[&§]([0-9a-fA-F])/g)]
  if (legacyMatches.length) {
    const code = legacyMatches[legacyMatches.length - 1][1].toLowerCase()
    return LEGACY_MC_COLORS[code] || null
  }

  return null
}

export function toVoidiumHex(value: string): string {
  return `&#${String(value || '#a855f7').replace('#', '').toUpperCase()}`
}

export function formatValue(value: unknown): string {
  return Array.isArray(value) ? value.join(' | ') : String(value)
}
