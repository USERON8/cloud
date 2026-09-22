import { ref } from 'vue'
import {
  createSpu,
  getSpu,
  listSkuByIds,
  listSpuByCategory,
  updateSpu,
  updateSpuStatus
} from '../../../api/product-catalog'
import type { SkuDetail, SpuCreateRequest, SpuDetail } from '../../../types/domain'
import { toast } from '../../../utils/ui'
import type { OpsInputTools } from '../model/input-tools'

type CatalogInputTools = Pick<OpsInputTools, 'parseJson' | 'parseNumberList'>

export function useCatalogGovernance(input: CatalogInputTools) {
  const spuId = ref('')
  const spuStatus = ref('')
  const spuCategoryId = ref('')
  const skuIds = ref('')
  const spuPayloadJson = ref('')
  const spuDetail = ref<SpuDetail | null>(null)
  const spuList = ref<SpuDetail[] | null>(null)
  const skuList = ref<SkuDetail[] | null>(null)
  const spuActionResult = ref<number | boolean | null>(null)

  async function createSpuAction(): Promise<void> {
    const payload = input.parseJson<SpuCreateRequest>(spuPayloadJson.value, 'SPU')
    if (!payload) return
    try {
      spuActionResult.value = await createSpu(payload)
      toast('SPU created', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Create failed')
    }
  }

  async function updateSpuAction(): Promise<void> {
    const id = Number(spuId.value)
    const payload = input.parseJson<SpuCreateRequest>(spuPayloadJson.value, 'SPU')
    if (!Number.isFinite(id) || !payload) {
      toast('Please enter an SPU ID and provide JSON')
      return
    }
    try {
      spuActionResult.value = await updateSpu(id, payload)
      toast('SPU updated', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed')
    }
  }

  async function loadSpuDetail(): Promise<void> {
    const id = Number(spuId.value)
    if (!Number.isFinite(id)) {
      toast('Please enter an SPU ID')
      return
    }
    try {
      spuDetail.value = await getSpu(id)
      toast('SPU details loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function loadSpuByCategory(): Promise<void> {
    const id = Number(spuCategoryId.value)
    const status = spuStatus.value ? Number(spuStatus.value) : undefined
    if (!Number.isFinite(id)) {
      toast('Please enter a category ID')
      return
    }
    try {
      spuList.value = await listSpuByCategory(id, Number.isFinite(status) ? status : undefined)
      toast('Category SPUs loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function loadSkuList(): Promise<void> {
    const ids = input.parseNumberList(skuIds.value)
    if (ids.length === 0) {
      toast('Please enter a list of SKU IDs')
      return
    }
    try {
      skuList.value = await listSkuByIds(ids)
      toast('SKU list loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function updateSpuStatusAction(): Promise<void> {
    const id = Number(spuId.value)
    const status = Number(spuStatus.value)
    if (!Number.isFinite(id) || !Number.isFinite(status)) {
      toast('Please enter an SPU ID and status')
      return
    }
    try {
      spuActionResult.value = await updateSpuStatus(id, status)
      toast('Status updated', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Update failed')
    }
  }

  return {
    createSpuAction,
    loadSkuList,
    loadSpuByCategory,
    loadSpuDetail,
    skuIds,
    skuList,
    spuActionResult,
    spuCategoryId,
    spuDetail,
    spuId,
    spuList,
    spuPayloadJson,
    spuStatus,
    updateSpuAction,
    updateSpuStatusAction
  }
}
