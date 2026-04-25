import http from './http'
import type { UserStatisticsOverview } from '../types/domain'

export function getStatisticsOverview(): Promise<UserStatisticsOverview> {
  return http.get<UserStatisticsOverview, UserStatisticsOverview>('/api/admin/statistics/overview')
}

export function getStatisticsOverviewAsync(): Promise<UserStatisticsOverview> {
  return http.get<UserStatisticsOverview, UserStatisticsOverview>('/api/admin/statistics/overview/async')
}

export function getRegistrationTrendRange(startDate: string, endDate: string): Promise<Record<string, number>> {
  return http.get<Record<string, number>, Record<string, number>>('/api/admin/statistics/registration-trend', {
    params: { startDate, endDate }
  })
}

export function getRegistrationTrend(days = 30): Promise<Record<string, number>> {
  return http.get<Record<string, number>, Record<string, number>>('/api/admin/statistics/registration-trend/async', {
    params: { days }
  })
}

export function getRoleDistribution(): Promise<Record<string, number>> {
  return http.get<Record<string, number>, Record<string, number>>('/api/admin/statistics/role-distribution')
}

export function getStatusDistribution(): Promise<Record<string, number>> {
  return http.get<Record<string, number>, Record<string, number>>('/api/admin/statistics/status-distribution')
}

export function getActiveUsers(days = 7): Promise<number> {
  return http.get<number, number>('/api/admin/statistics/active-users', { params: { days } })
}

export function getGrowthRate(days = 7): Promise<number> {
  return http.get<number, number>('/api/admin/statistics/growth-rate', { params: { days } })
}

export function getActivityRanking(limit = 10, days = 30): Promise<Record<number, number>> {
  return http.get<Record<number, number>, Record<number, number>>('/api/admin/statistics/activity-ranking', {
    params: { limit, days }
  })
}

export function refreshStatisticsCache(): Promise<boolean> {
  return http.post<boolean, boolean>('/api/admin/statistics/cache-refreshes')
}
