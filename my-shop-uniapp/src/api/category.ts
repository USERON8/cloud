import http from './http'
import type { PageResult } from '../types/api'
import type { CategoryItem, EntityId } from '../types/domain'

export interface CategoryQuery {
  page?: number
  size?: number
  parentId?: EntityId
  level?: number
}

export function getCategories(params: CategoryQuery = {}): Promise<PageResult<CategoryItem>> {
  return http.get<PageResult<CategoryItem>, PageResult<CategoryItem>>('/api/categories', { params })
}

export function getCategoryById(id: EntityId): Promise<CategoryItem> {
  return http.get<CategoryItem, CategoryItem>(`/api/categories/${id}`)
}

export function getCategoryTree(enabledOnly = false): Promise<CategoryItem[]> {
  return http.get<CategoryItem[], CategoryItem[]>('/api/categories/tree', { params: { enabledOnly } })
}

export function getCategoryChildren(id: EntityId, enabledOnly = false): Promise<CategoryItem[]> {
  return http.get<CategoryItem[], CategoryItem[]>(`/api/categories/${id}/children`, { params: { enabledOnly } })
}

export function createCategory(payload: CategoryItem): Promise<CategoryItem> {
  return http.post<CategoryItem, CategoryItem>('/api/categories', payload)
}

export function updateCategory(id: EntityId, payload: CategoryItem): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/categories/${id}`, payload)
}

export function deleteCategory(id: EntityId, cascade = false): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/categories/${id}`, { params: { cascade } })
}

export function updateCategoryStatus(id: EntityId, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/categories/${id}/status`, null, { params: { status } })
}

export function updateCategorySort(id: EntityId, sort: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/categories/${id}/sort`, null, { params: { sort } })
}

export function moveCategory(id: EntityId, newParentId: EntityId): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/categories/${id}/move`, null, { params: { newParentId } })
}

export function deleteCategoriesBatch(ids: EntityId[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/categories/batch', { data: ids })
}

export function updateCategoryStatusBatch(ids: EntityId[], status: number): Promise<number> {
  return http.patch<number, number>('/api/categories/batch/status', null, {
    params: { ids, status }
  })
}

export function createCategoriesBatch(payload: CategoryItem[]): Promise<number> {
  return http.post<number, number>('/api/categories/batch', payload)
}
