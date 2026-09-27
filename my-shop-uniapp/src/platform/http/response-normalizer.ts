const LONG_INTEGER_PATTERN = /([:\[,]\s*)(-?\d{16,})(?=\s*[,}\]])/g
const ENTITY_ID_KEY_PATTERN = /(^id$|Id$)/
const ENTITY_IDS_KEY_PATTERN = /Ids$/

function normalizeIdentifierValue(key: string, value: unknown): unknown {
  if (ENTITY_ID_KEY_PATTERN.test(key) && (typeof value === 'number' || typeof value === 'string')) {
    return String(value)
  }
  if (ENTITY_IDS_KEY_PATTERN.test(key) && Array.isArray(value)) {
    return value.map((item) =>
      typeof item === 'number' || typeof item === 'string' ? String(item) : item
    )
  }
  return value
}

export function parseJsonTextWithLongIntegers(payload: string): unknown {
  const normalized = payload.replace(LONG_INTEGER_PATTERN, '$1"$2"')
  return JSON.parse(normalized, normalizeIdentifierValue)
}

export function normalizeResponseData(data: unknown): unknown {
  if (typeof data !== 'string') {
    return data
  }
  const trimmed = data.trim()
  if (!trimmed) {
    return data
  }
  if (
    (trimmed.startsWith('{') && trimmed.endsWith('}')) ||
    (trimmed.startsWith('[') && trimmed.endsWith(']'))
  ) {
    try {
      return parseJsonTextWithLongIntegers(trimmed)
    } catch {
      return data
    }
  }
  return data
}
