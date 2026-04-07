const MC_COLORS: Record<string, string> = {
  '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
  '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
  '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
  'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF',
}

interface Segment {
  text: string
  color: string
  bold: boolean
  italic: boolean
  underline: boolean
  strikethrough: boolean
  obfuscated: boolean
}

function normalizeMcInput(raw: string): string {
  let normalized = String(raw || '')

  normalized = normalized.replace(/§x(§[0-9a-fA-F]){6}/g, match => {
    const hex = match.replaceAll('§x', '').replaceAll('§', '')
    return `&#${hex}`
  })

  return normalized.replaceAll('§', '&')
}

function parseMcText(raw: string): Segment[] {
  const source = normalizeMcInput(raw)
  const segments: Segment[] = []
  let color = '#AAAAAA'
  let bold = false
  let italic = false
  let underline = false
  let strikethrough = false
  let obfuscated = false
  let buffer = ''
  let i = 0

  const flush = () => {
    if (buffer) {
      segments.push({ text: buffer, color, bold, italic, underline, strikethrough, obfuscated })
      buffer = ''
    }
  }

  while (i < source.length) {
    if (source[i] === '&' && i + 1 < source.length) {
      const next = source[i + 1]
      if (next === '#' && i + 7 < source.length) {
        // &#RRGGBB hex
        const hex = source.substring(i + 2, i + 8)
        if (/^[0-9a-fA-F]{6}$/.test(hex)) {
          flush()
          color = '#' + hex
          bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false
          i += 8
          continue
        }
      }
      const code = next.toLowerCase()
      if (MC_COLORS[code]) {
        flush()
        color = MC_COLORS[code]
        bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false
        i += 2
        continue
      }
      if (code === 'l') { flush(); bold = true; i += 2; continue }
      if (code === 'o') { flush(); italic = true; i += 2; continue }
      if (code === 'n') { flush(); underline = true; i += 2; continue }
      if (code === 'm') { flush(); strikethrough = true; i += 2; continue }
      if (code === 'k') { flush(); obfuscated = true; i += 2; continue }
      if (code === 'r') {
        flush()
        color = '#AAAAAA'; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false
        i += 2
        continue
      }
    }
    buffer += source[i]
    i++
  }
  flush()
  return segments
}

interface Props {
  text: string
}

export function McTextPreview({ text }: Props) {
  if (!text?.trim()) return null
  const segments = parseMcText(text)
  return (
    <div className="mc-preview">
      <span className="mc-preview-label">Preview:</span>
      <div className="mc-preview-text">
        {segments.map((seg, i) => (
          <span
            key={i}
            style={{
              color: seg.color,
              fontWeight: seg.bold ? 700 : 400,
              fontStyle: seg.italic ? 'italic' : 'normal',
              textDecoration: [seg.underline && 'underline', seg.strikethrough && 'line-through'].filter(Boolean).join(' ') || 'none',
              ...(seg.obfuscated ? { filter: 'blur(3px)' } : {}),
            }}
          >{seg.text}</span>
        ))}
      </div>
    </div>
  )
}
