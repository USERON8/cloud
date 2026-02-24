export const SUCCESS_CODE = 200

export interface ResultEnvelope<T> {
  code: number
  message: string
  data: T
  timestamp?: number
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

  constructor(message: string, code: number) {
    super(message)
    this.name = 'BusinessError'
    this.code = code
  }
}
