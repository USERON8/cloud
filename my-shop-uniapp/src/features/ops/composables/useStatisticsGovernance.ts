import { ref } from 'vue'
import {
  getRegistrationTrendRange,
  getStatisticsOverviewAsync,
  refreshStatisticsCache
} from '../../../api/statistics'
import type { UserStatisticsOverview } from '../../../types/domain'
import { isDateAfter } from '../../../utils/format'
import { toast } from '../../../utils/ui'
import type { OpsInputTools } from '../model/input-tools'

type StatisticsInputTools = Pick<OpsInputTools, 'normalizeDateInput'>

export function useStatisticsGovernance(input: StatisticsInputTools) {
  const statsStartDate = ref('')
  const statsEndDate = ref('')
  const statsOverviewAsync = ref<UserStatisticsOverview | null>(null)
  const statsTrendRange = ref<Record<string, number> | null>(null)
  const statsRefreshResult = ref<boolean | null>(null)

  async function loadStatsOverviewAsync(): Promise<void> {
    try {
      statsOverviewAsync.value = await getStatisticsOverviewAsync()
      toast('Async statistics loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function loadStatsTrendRange(): Promise<void> {
    const start = input.normalizeDateInput(statsStartDate.value, 'Start date')
    if (!start) return
    const end = input.normalizeDateInput(statsEndDate.value, 'End date')
    if (!end) return
    if (isDateAfter(start, end)) {
      toast('Start date cannot be later than end date')
      return
    }
    try {
      statsTrendRange.value = await getRegistrationTrendRange(start, end)
      toast('Trend data loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function refreshStatsCache(): Promise<void> {
    try {
      statsRefreshResult.value = await refreshStatisticsCache()
      toast('Statistics cache refresh triggered', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Refresh failed')
    }
  }

  return {
    loadStatsOverviewAsync,
    loadStatsTrendRange,
    refreshStatsCache,
    statsEndDate,
    statsOverviewAsync,
    statsRefreshResult,
    statsStartDate,
    statsTrendRange
  }
}
