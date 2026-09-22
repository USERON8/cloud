import { ref } from 'vue'
import { getThreadPoolDetail, getThreadPools } from '../../../api/thread-pool'
import type { ThreadPoolInfo } from '../../../types/domain'
import { toast } from '../../../utils/ui'

export function useThreadPoolGovernance() {
  const threadPools = ref<ThreadPoolInfo[] | null>(null)
  const threadPoolName = ref('')
  const threadPoolDetail = ref<ThreadPoolInfo | null>(null)

  async function loadThreadPools(): Promise<void> {
    try {
      threadPools.value = await getThreadPools()
      toast('Thread pools loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function loadThreadPoolDetail(): Promise<void> {
    const name = threadPoolName.value.trim()
    if (!name) {
      toast('Please enter a thread pool name')
      return
    }
    try {
      threadPoolDetail.value = await getThreadPoolDetail(name)
      toast('Thread pool details loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  return {
    loadThreadPoolDetail,
    loadThreadPools,
    threadPoolDetail,
    threadPoolName,
    threadPools
  }
}
