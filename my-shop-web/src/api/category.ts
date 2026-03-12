import http from './http'
import type { PageResult } from '../types/api'
import type { CategoryItem } from '../types/domain'

export interface CategoryQuery {
  page?: number
  size?: number
  parentId?: number
  level?: number
}

export function getCategories(params: CategoryQuery = {}): Promise<PageResult<CategoryItem>> {
  return http.get<PageResult<CategoryItem>, PageResult<CategoryItem>>('/api/category', { params })
}

export function getCategoryTree(enabledOnly = false): Promise<CategoryItem[]> {
  return http.get<CategoryItem[], CategoryItem[]>('/api/category/tree', { params: { enabledOnly } })
}

export function createCategory(payload: CategoryItem): Promise<CategoryItem> {
  return http.post<CategoryItem, CategoryItem>('/api/category', payload)
}

export function updateCategory(id: number, payload: CategoryItem): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/category/${id}`, payload)
}

export function deleteCategory(id: number, cascade = false): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/category/${id}`, { params: { cascade } })
}

export function updateCategoryStatus(id: number, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/category/${id}/status`, null, { params: { status } })
}

export function updateCategorySort(id: number, sort: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/category/${id}/sort`, null, { params: { sort } })
}

export function moveCategory(id: number, newParentId: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/category/${id}/move`, null, { params: { newParentId } })
}

export function deleteCategoriesBatch(ids: number[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/category/batch', { data: ids })
}

export function updateCategoryStatusBatch(ids: number[], status: number): Promise<number> {
  return http.patch<number, number>('/api/category/batch/status', null, {
    params: {
      ids: ids.join(','),
      status
    }
  })
}

export function createCategoriesBatch(payload: CategoryItem[]): Promise<number> {
  return http.post<number, number>('/api/category/batch', payload)
}
