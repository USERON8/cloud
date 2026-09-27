import { ref } from 'vue'
import {
  createCategoriesBatch,
  deleteCategoriesBatch,
  getCategoryById,
  getCategoryChildren,
  getCategoryTree,
  moveCategory,
  updateCategorySort,
  updateCategoryStatusBatch
} from '../../../api/category'
import type { CategoryItem } from '../../../types/domain'
import { toast } from '../../../utils/ui'
import type { OpsInputTools } from '../model/input-tools'

type CategoryInputTools = Pick<OpsInputTools, 'parseJson' | 'parseIdList' | 'requirePositiveId'>

export function useCategoryGovernance(input: CategoryInputTools) {
  const categoryIdInput = ref('')
  const categorySortInput = ref('')
  const categoryMoveParent = ref('')
  const categoryBatchIds = ref('')
  const categoryBatchStatus = ref('')
  const categoryBatchPayload = ref('')
  const categoryTree = ref<CategoryItem[] | null>(null)
  const categoryChildren = ref<CategoryItem[] | null>(null)
  const categoryById = ref<CategoryItem | null>(null)
  const categoryBatchResult = ref<number | boolean | null>(null)

  async function loadCategoryTree(): Promise<void> {
    try {
      categoryTree.value = await getCategoryTree(false)
      toast('Category tree loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function loadCategoryChildren(): Promise<void> {
    const id = input.requirePositiveId(categoryIdInput.value, 'category ID')
    if (!id) {
      toast('Please enter a category ID')
      return
    }
    try {
      categoryChildren.value = await getCategoryChildren(id, false)
      toast('Child categories loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function loadCategoryById(): Promise<void> {
    const id = input.requirePositiveId(categoryIdInput.value, 'category ID')
    if (!id) {
      toast('Please enter a category ID')
      return
    }
    try {
      categoryById.value = await getCategoryById(id)
      toast('Category details loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function updateCategoryStatusBatchAction(): Promise<void> {
    const ids = input.parseIdList(categoryBatchIds.value)
    const status = Number(categoryBatchStatus.value)
    if (ids.length === 0 || !Number.isFinite(status)) {
      toast('Please enter batch IDs and a status value')
      return
    }
    try {
      categoryBatchResult.value = await updateCategoryStatusBatch(ids, status)
      toast('Batch status updated', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed')
    }
  }

  async function createCategoriesBatchAction(): Promise<void> {
    const payload = input.parseJson<CategoryItem[]>(categoryBatchPayload.value, 'Category batch')
    if (!payload) return
    try {
      categoryBatchResult.value = await createCategoriesBatch(payload)
      toast('Batch create submitted', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Create failed')
    }
  }

  async function deleteCategoriesBatchAction(): Promise<void> {
    const ids = input.parseIdList(categoryBatchIds.value)
    if (ids.length === 0) {
      toast('Please enter batch IDs')
      return
    }
    try {
      categoryBatchResult.value = await deleteCategoriesBatch(ids)
      toast('Batch delete submitted', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Delete failed')
    }
  }

  async function updateCategorySortAction(): Promise<void> {
    const id = input.requirePositiveId(categoryIdInput.value, 'category ID')
    const sort = Number(categorySortInput.value)
    if (!id || !Number.isFinite(sort)) {
      toast('Please enter a category ID and sort value')
      return
    }
    try {
      await updateCategorySort(id, sort)
      toast('Sort order updated', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed')
    }
  }

  async function moveCategoryAction(): Promise<void> {
    const id = input.requirePositiveId(categoryIdInput.value, 'category ID')
    const parentId = input.requirePositiveId(categoryMoveParent.value, 'new parent ID')
    if (!id || !parentId) {
      toast('Please enter a category ID and new parent ID')
      return
    }
    try {
      await moveCategory(id, parentId)
      toast('Category moved', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Move failed')
    }
  }

  return {
    categoryBatchIds,
    categoryBatchPayload,
    categoryBatchResult,
    categoryBatchStatus,
    categoryById,
    categoryChildren,
    categoryIdInput,
    categoryMoveParent,
    categorySortInput,
    categoryTree,
    createCategoriesBatchAction,
    deleteCategoriesBatchAction,
    loadCategoryById,
    loadCategoryChildren,
    loadCategoryTree,
    moveCategoryAction,
    updateCategorySortAction,
    updateCategoryStatusBatchAction
  }
}
