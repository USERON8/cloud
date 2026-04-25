import http from './http'
import type { ThreadPoolInfo } from '../types/domain'

export function getThreadPools(): Promise<ThreadPoolInfo[]> {
  return http.get<ThreadPoolInfo[], ThreadPoolInfo[]>('/api/admin/thread-pools')
}

export function getThreadPoolDetail(name: string): Promise<ThreadPoolInfo> {
  return http.get<ThreadPoolInfo, ThreadPoolInfo>(`/api/admin/thread-pools/${name}`)
}
