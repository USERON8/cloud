function encodeSvg(value: string): string {
  return encodeURIComponent(value)
    .replace(/%20/g, ' ')
    .replace(/%3D/g, '=')
    .replace(/%3A/g, ':')
    .replace(/%2F/g, '/')
}

function buildLabel(value?: string): string {
  const trimmed = (value || '').trim()
  if (!trimmed) {
    return 'Product'
  }
  return trimmed.length > 18 ? `${trimmed.slice(0, 18)}...` : trimmed
}

function buildAccent(value?: string): string {
  const palette = ['#0b6b5f', '#0f766e', '#b45309', '#9f1239', '#0f766e', '#0369a1']
  const source = buildLabel(value)
  const seed = source.split('').reduce((sum, char) => sum + char.charCodeAt(0), 0)
  return palette[seed % palette.length]
}

function shouldUsePlaceholderImage(imageUrl: string): boolean {
  try {
    const parsed = new URL(imageUrl, 'https://placeholder.invalid')
    if (!/^https?:$/.test(parsed.protocol)) {
      return true
    }
    if (/(^|\.)example\.com$/i.test(parsed.hostname)) {
      return true
    }
    if (
      typeof window !== 'undefined' &&
      window.location.protocol === 'https:' &&
      parsed.protocol === 'http:'
    ) {
      return true
    }
    return false
  } catch {
    return true
  }
}

export function createProductPlaceholderSvg(productName?: string): string {
  const label = buildLabel(productName)
  const accent = buildAccent(productName)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 320" role="img" aria-label="${label}">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#fffdf8"/>
      <stop offset="100%" stop-color="#f1e7d8"/>
    </linearGradient>
  </defs>
  <rect width="320" height="320" rx="36" fill="url(#bg)"/>
  <circle cx="252" cy="70" r="34" fill="${accent}" opacity="0.14"/>
  <circle cx="72" cy="250" r="44" fill="${accent}" opacity="0.1"/>
  <rect x="64" y="74" width="192" height="140" rx="28" fill="#ffffff"/>
  <rect x="92" y="104" width="136" height="80" rx="18" fill="${accent}" opacity="0.18"/>
  <rect x="96" y="232" width="128" height="12" rx="6" fill="#7f8b92"/>
  <rect x="122" y="254" width="76" height="10" rx="5" fill="#c7cdd1"/>
  <text x="160" y="292" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="18" fill="#2b3135">${label}</text>
</svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeSvg(svg)}`
}

export function resolveProductImageUrl(imageUrl?: string, productName?: string, failed = false): string {
  const trimmed = (imageUrl || '').trim()
  if (!trimmed || failed || shouldUsePlaceholderImage(trimmed)) {
    return createProductPlaceholderSvg(productName)
  }
  return trimmed
}
