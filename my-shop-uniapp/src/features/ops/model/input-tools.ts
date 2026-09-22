export type InputErrorReporter = (message: string) => void

export function createOpsInputTools(reportError: InputErrorReporter) {
  function formatJson(value: unknown): string {
    if (value == null) {
      return '--'
    }
    try {
      return JSON.stringify(value, null, 2)
    } catch {
      return String(value)
    }
  }

  function parseJson<T>(raw: string, label: string): T | null {
    if (!raw.trim()) {
      reportError(`${label} JSON cannot be empty`)
      return null
    }
    try {
      return JSON.parse(raw) as T
    } catch (error) {
      reportError(error instanceof Error ? error.message : `Failed to parse ${label} JSON`)
      return null
    }
  }

  function parseNumberList(raw: string): number[] {
    return raw
      .split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => Number.isFinite(value) && value > 0)
  }

  function requirePositiveId(raw: string, label: string): number | null {
    const value = Number(raw)
    if (!Number.isFinite(value) || value <= 0) {
      reportError(`Please enter ${label}`)
      return null
    }
    return value
  }

  function parseOptionalNumber(raw: string, label: string): number | undefined | null {
    if (!raw.trim()) {
      return undefined
    }
    const value = Number(raw)
    if (!Number.isFinite(value)) {
      reportError(`${label} must be numeric`)
      return null
    }
    return value
  }

  function parseOptionalBoolean(raw: string, label: string): boolean | undefined | null {
    const value = raw.trim().toLowerCase()
    if (!value) {
      return undefined
    }
    if (['true', '1', 'yes'].includes(value)) {
      return true
    }
    if (['false', '0', 'no'].includes(value)) {
      return false
    }
    reportError(`${label} must be true or false`)
    return null
  }

  function parseStringList(raw: string): string[] | undefined {
    const values = raw
      .split(',')
      .map((value) => value.trim())
      .filter((value) => value.length > 0)
    return values.length > 0 ? values : undefined
  }

  function compactPayload<T extends Record<string, unknown>>(payload: T): T {
    return Object.fromEntries(
      Object.entries(payload).filter(([, value]) => {
        if (value === undefined || value === '') {
          return false
        }
        return !Array.isArray(value) || value.length > 0
      })
    ) as T
  }

  function normalizeDateInput(value: string, label: string): string | null {
    const trimmed = value.trim()
    if (!trimmed) {
      reportError(`Please enter ${label}`)
      return null
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
      reportError(`${label} must use the YYYY-MM-DD format`)
      return null
    }
    return trimmed
  }

  return {
    compactPayload,
    formatJson,
    normalizeDateInput,
    parseJson,
    parseNumberList,
    parseOptionalBoolean,
    parseOptionalNumber,
    parseStringList,
    requirePositiveId,
    toPrettyJson: (value: unknown): string => JSON.stringify(value, null, 2)
  }
}

export type OpsInputTools = ReturnType<typeof createOpsInputTools>
