import http from './http'

export function sendWelcomeNotification(userId: number): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/user/notification/welcome/${userId}`)
}

export function sendStatusChangeNotification(
  userId: number,
  payload: { newStatus: number; reason?: string }
): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/user/notification/status-change/${userId}`, payload)
}

export function sendBatchNotification(payload: { userIds: number[]; title: string; content: string }): Promise<boolean> {
  return http.post<boolean, boolean>('/api/user/notification/batch', payload)
}

export function sendSystemAnnouncement(payload: { title: string; content: string }): Promise<boolean> {
  return http.post<boolean, boolean>('/api/user/notification/system', payload)
}
