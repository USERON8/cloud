import { getSpu } from '../api/product-catalog'
import type { ProductItem, SearchProductDocument } from '../types/domain'
import { toast } from './ui'

/**
 * Convert a SearchProductDocument (from ES / smart search) to a ProductItem
 * used throughout the catalog and market views.
 */
export function mapSearchDocumentToProduct(item: SearchProductDocument): ProductItem {
  return {
    id: typeof item.productId === 'number' ? item.productId : 0,
    shopId: item.shopId,
    name: item.productName || 'Unnamed product',
    price: item.price,
    stockQuantity: item.stockQuantity,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status,
    description: item.description,
    imageUrl: item.imageUrl,
  }
}

/**
 * Resolve the SKU ID for a product, with per-page caches supplied by the caller
 * so each page scope maintains its own cache lifetime.
 */
export async function resolveCartSkuId(
  item: ProductItem,
  skuIdCache: Map<number | string, number | null>,
  skuLookupCache: Map<number | string, Promise<number | null>>,
): Promise<number | null> {
  const cachedSkuId = skuIdCache.get(item.id)
  if (cachedSkuId !== undefined) {
    return cachedSkuId
  }
  const inflightLookup = skuLookupCache.get(item.id)
  if (inflightLookup) {
    return inflightLookup
  }

  if (typeof item.skuId === 'number' && item.skuId > 0) {
    skuIdCache.set(item.id, item.skuId)
    return item.skuId
  }

  const lookupPromise = (async () => {
    const spu = await getSpu(item.id)
    const availableSkus = (spu?.skus || []).filter(
      (sku) => typeof sku.skuId === 'number' && sku.skuId > 0,
    )
    if (availableSkus.length === 1 && typeof availableSkus[0]?.skuId === 'number') {
      const resolvedSkuId = availableSkus[0].skuId
      skuIdCache.set(item.id, resolvedSkuId)
      return resolvedSkuId
    }
    if (availableSkus.length === 0) {
      skuIdCache.set(item.id, null)
      toast('SKU information is unavailable')
      return null
    }
    skuIdCache.set(item.id, null)
    toast('This product has multiple variants and cannot be added from the list view')
    return null
  })().finally(() => {
    skuLookupCache.delete(item.id)
  })

  skuLookupCache.set(item.id, lookupPromise)
  return lookupPromise
}
