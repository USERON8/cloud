import { ref } from 'vue'
import { reviewMerchantAuthBatch } from '../../../api/merchant-auth'
import {
  approveMerchantsBatch,
  deleteMerchantsBatch,
  updateMerchantStatusBatch
} from '../../../api/merchant'
import { findUserByUsername, updateUsersBatch } from '../../../api/user-management'
import type { UserSummary, UserUpsertPayload } from '../../../types/domains/user-management'
import { confirm, toast } from '../../../utils/ui'
import type { OpsInputTools } from '../model/input-tools'

type BatchInputTools = Pick<OpsInputTools, 'parseJson' | 'parseNumberList'>

export function useBatchGovernance(input: BatchInputTools) {
  const userFindUsername = ref('')
  const userBatchJson = ref('')
  const userBatchResult = ref<boolean | null>(null)
  const userFindResult = ref<UserSummary | null>(null)
  const merchantBatchIds = ref('')
  const merchantBatchStatus = ref('')
  const merchantBatchRemark = ref('')
  const merchantAuthBatchIds = ref('')
  const merchantAuthBatchStatus = ref('')
  const merchantAuthBatchRemark = ref('')
  const batchResult = ref<boolean | null>(null)

  async function findUser(): Promise<void> {
    const username = userFindUsername.value.trim()
    if (!username) {
      toast('Please enter a username')
      return
    }
    try {
      userFindResult.value = await findUserByUsername(username)
      toast('User loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Query failed')
    }
  }

  async function updateUsersBatchAction(): Promise<void> {
    const payload = input.parseJson<UserUpsertPayload[]>(userBatchJson.value, 'User batch')
    if (!payload) return
    try {
      userBatchResult.value = await updateUsersBatch(payload)
      toast('Batch update submitted', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed')
    }
  }

  async function approveMerchantsBatchAction(): Promise<void> {
    const ids = input.parseNumberList(merchantBatchIds.value)
    if (ids.length === 0) {
      toast('Please enter a list of merchant IDs')
      return
    }
    try {
      batchResult.value = await approveMerchantsBatch(
        ids,
        merchantBatchRemark.value || undefined
      )
      toast('Batch approval completed', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Operation failed')
    }
  }

  async function deleteMerchantsBatchAction(): Promise<void> {
    const ids = input.parseNumberList(merchantBatchIds.value)
    if (ids.length === 0) {
      toast('Please enter a list of merchant IDs')
      return
    }
    if (!(await confirm('Delete the selected merchants in batch?'))) return
    try {
      batchResult.value = await deleteMerchantsBatch(ids)
      toast('Batch delete submitted', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Delete failed')
    }
  }

  async function updateMerchantStatusBatchAction(): Promise<void> {
    const ids = input.parseNumberList(merchantBatchIds.value)
    const status = Number(merchantBatchStatus.value)
    if (ids.length === 0 || !Number.isFinite(status)) {
      toast('Please enter merchant IDs and a status value')
      return
    }
    try {
      batchResult.value = await updateMerchantStatusBatch(ids, status)
      toast('Batch status updated', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed')
    }
  }

  async function reviewMerchantAuthBatchAction(): Promise<void> {
    const ids = input.parseNumberList(merchantAuthBatchIds.value)
    const status = Number(merchantAuthBatchStatus.value)
    if (ids.length === 0 || !Number.isFinite(status)) {
      toast('Please enter merchant IDs and an auth status')
      return
    }
    try {
      batchResult.value = await reviewMerchantAuthBatch(
        ids,
        status,
        merchantAuthBatchRemark.value || undefined
      )
      toast('Batch auth review submitted', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Operation failed')
    }
  }

  return {
    approveMerchantsBatchAction,
    batchResult,
    deleteMerchantsBatchAction,
    findUser,
    merchantAuthBatchIds,
    merchantAuthBatchRemark,
    merchantAuthBatchStatus,
    merchantBatchIds,
    merchantBatchRemark,
    merchantBatchStatus,
    reviewMerchantAuthBatchAction,
    updateMerchantStatusBatchAction,
    updateUsersBatchAction,
    userBatchJson,
    userBatchResult,
    userFindResult,
    userFindUsername
  }
}
