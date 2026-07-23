export function esc(str) {
  if (str === null || str === undefined) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

export function cap(s) {
  if (!s) return ''
  return s.charAt(0).toUpperCase() + s.slice(1)
}

export function usd(n) {
  if (n == null) return '$0'
  return '$' + Math.round(n).toLocaleString()
}

export function money(n) {
  if (n == null) return '$0'
  return '$' + Math.round(n).toLocaleString()
}
