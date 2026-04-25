export const SUCCESS_CODE = 200

export interface ResultEnvelope<T> {
  code: number
  message: string
  data: T
  timestamp?: number
  traceId?: string
}

export interface PageResult<T> {
  current: number
  size: number
  total: number
  pages: number
  records: T[]
  hasPrevious: boolean
  hasNext: boolean
}

export class BusinessError extends Error {
  code: number
  httpStatus?: number
  traceId?: string
  category?: ApiErrorCategory

  constructor(message: string, code: number, options: ApiErrorOptions = {}) {
    super(message)
    this.name = 'BusinessError'
    this.code = code
    this.httpStatus = options.httpStatus
    this.traceId = options.traceId
    this.category = options.category
  }
}

export type ApiErrorCategory =
  | 'business'
  | 'validation'
  | 'auth'
  | 'permission'
  | 'notFound'
  | 'conflict'
  | 'rateLimit'
  | 'system'
  | 'remote'
  | 'network'

export interface ApiErrorOptions {
  httpStatus?: number
  traceId?: string
  category?: ApiErrorCategory
}
