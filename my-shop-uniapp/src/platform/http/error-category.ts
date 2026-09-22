import type { ApiErrorCategory } from '../../types/api'

export function resolveApiErrorCategory(code: number, httpStatus?: number): ApiErrorCategory {
  if (httpStatus === 401 || (code >= 17011 && code <= 17055)) {
    return 'auth'
  }
  if (httpStatus === 403 || code === 2001 || code === 2002) {
    return 'permission'
  }
  if (
    httpStatus === 404 ||
    code === 4001 ||
    code === 7004 ||
    code === 9001 ||
    code === 10001 ||
    code === 11001 ||
    code === 12001 ||
    code === 13001
  ) {
    return 'notFound'
  }
  if (
    httpStatus === 409 ||
    code === 4002 ||
    code === 5001 ||
    code === 5002 ||
    code === 7002 ||
    code === 9008
  ) {
    return 'conflict'
  }
  if (httpStatus === 429 || code === 429 || code === 18003) {
    return 'rateLimit'
  }
  if ((code >= 18001 && code <= 18004) || httpStatus === 503 || httpStatus === 504) {
    return 'remote'
  }
  if (
    (httpStatus && httpStatus >= 500) ||
    (code >= 1001 && code <= 1004) ||
    code === 500 ||
    code === 7001 ||
    code === 14002
  ) {
    return 'system'
  }
  if (
    httpStatus === 400 ||
    code === 400 ||
    code === 501 ||
    (code >= 3001 && code <= 3003) ||
    code === 8007
  ) {
    return 'validation'
  }
  return 'business'
}
