export function formatBytes(bytes?: number | null): string {
  if (bytes == null) {
    return ''
  }
  let value = bytes
  let unit = 'B'
  for (const next of ['KB', 'MB', 'GB']) {
    if (value < 1024) {
      break
    }
    value /= 1024
    unit = next
  }
  return unit === 'B' ? `${value} B` : `${value.toFixed(value >= 100 ? 0 : 1)} ${unit}`
}

export function formatDateTime(iso?: string | null): string {
  if (!iso) {
    return ''
  }
  return new Date(iso).toLocaleString()
}
