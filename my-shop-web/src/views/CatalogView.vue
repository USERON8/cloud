<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createProduct,
  listProducts,
  listSearchHotKeywords,
  listSearchKeywordRecommendations,
  listSearchSuggestions,
  smartSearchProducts,
  updateProduct,
  updateProductStatus
} from '../api/product'
import type { ProductItem, ProductUpsertPayload, SearchProductDocument } from '../types/domain'
import { useRole } from '../auth/permission'
import { sessionState } from '../auth/session'

interface ProductFormModel {
  shopId: number
  name: string
  price: number
  stockQuantity: number
  categoryId: number
  brandId?: number
  status: number
  description: string
  imageUrl: string
}

interface SearchSuggestionItem {
  value: string
}

const route = useRoute()
const loading = ref(false)
const rows = ref<ProductItem[]>([])
const total = ref(0)
const params = reactive({
  page: 1,
  size: 10,
  name: ''
})

const { isAdmin, isMerchant } = useRole()
const isManagementMode = computed(() => Boolean(route.meta.manageProduct))
const canManageProducts = computed(() => isManagementMode.value && (isAdmin.value || isMerchant.value))
const canEnterManagePage = computed(() => isAdmin.value || isMerchant.value)
const isMerchantOnly = computed(() => isMerchant.value && !isAdmin.value)
const currentMerchantId = computed(() =>
  typeof sessionState.user?.id === 'number' ? sessionState.user.id : null
)

const hotKeywords = ref<string[]>([])
const recommendedKeywords = ref<string[]>([])
const blurTimer = ref<number | null>(null)
const suggestTimer = ref<number | null>(null)

const dialogVisible = ref(false)
const formSubmitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<ProductFormModel>({
  shopId: 0,
  name: '',
  price: 0,
  stockQuantity: 0,
  categoryId: 0,
  brandId: undefined,
  status: 1,
  description: '',
  imageUrl: ''
})

const rules: FormRules<ProductFormModel> = {
  shopId: [
    { required: true, message: 'Shop ID is required.', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Shop ID must be greater than 0.', trigger: 'blur' }
  ],
  name: [
    { required: true, message: 'Product name is required.', trigger: 'blur' },
    { min: 1, max: 255, message: 'Name length must be 1-255.', trigger: 'blur' }
  ],
  price: [
    { required: true, message: 'Price is required.', trigger: 'blur' },
    { type: 'number', min: 0.01, message: 'Price must be greater than 0.', trigger: 'blur' }
  ],
  stockQuantity: [
    { required: true, message: 'Stock quantity is required.', trigger: 'blur' },
    { type: 'number', min: 0, message: 'Stock quantity cannot be negative.', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: 'Category ID is required.', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Category ID must be greater than 0.', trigger: 'blur' }
  ],
  status: [{ required: true, message: 'Status is required.', trigger: 'change' }]
}

const dialogTitle = computed(() => (editingId.value ? 'Edit Product' : 'Create Product'))

function mapSearchDocumentToProduct(item: SearchProductDocument): ProductItem {
  return {
    id: typeof item.productId === 'number' ? item.productId : 0,
    shopId: item.shopId,
    name: item.productName || 'Unnamed Product',
    price: item.price,
    stockQuantity: item.stockQuantity,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status,
    description: item.description,
    imageUrl: item.imageUrl
  }
}

function resetForm(): void {
  editingId.value = null
  form.shopId = isMerchantOnly.value ? (currentMerchantId.value ?? 0) : 0
  form.name = ''
  form.price = 0
  form.stockQuantity = 0
  form.categoryId = 0
  form.brandId = undefined
  form.status = 1
  form.description = ''
  form.imageUrl = ''
}

function canManageRow(row: ProductItem): boolean {
  if (!canManageProducts.value) {
    return false
  }
  if (isAdmin.value) {
    return true
  }
  if (!isMerchantOnly.value || currentMerchantId.value == null) {
    return false
  }
  return row.shopId === currentMerchantId.value
}

function formatPrice(price?: number): string {
  if (typeof price !== 'number') {
    return '--'
  }
  return `CNY ${price.toFixed(2)}`
}

function statusText(status?: number): string {
  if (status === 1) {
    return 'Active'
  }
  if (status === 0) {
    return 'Inactive'
  }
  return 'Unknown'
}

function statusType(status?: number): 'success' | 'info' | 'warning' {
  if (status === 1) {
    return 'success'
  }
  if (status === 0) {
    return 'info'
  }
  return 'warning'
}

async function refreshKeywordRecommendations(seedKeyword = ''): Promise<void> {
  if (isManagementMode.value) {
    return
  }
  const [hotResult, recommendationResult] = await Promise.allSettled([
    listSearchHotKeywords(8),
    listSearchKeywordRecommendations(seedKeyword, 10)
  ])

  hotKeywords.value = hotResult.status === 'fulfilled' ? hotResult.value : []
  recommendedKeywords.value = recommendationResult.status === 'fulfilled' ? recommendationResult.value : []
}

async function loadProducts(): Promise<void> {
  loading.value = true
  try {
    if (isManagementMode.value) {
      const result = await listProducts({
        page: params.page,
        size: params.size,
        name: params.name || undefined
      })
      rows.value = result.records
      total.value = result.total
    } else {
      const searchResult = await smartSearchProducts({
        keyword: params.name || undefined,
        page: params.page,
        size: params.size,
        sortField: 'score',
        sortOrder: 'desc'
      })

      rows.value = searchResult.documents.map(mapSearchDocumentToProduct)
      total.value = searchResult.total
      await refreshKeywordRecommendations(params.name)
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load products'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  params.page = 1
  void loadProducts()
}

function onSearchFocus(): void {
  if (blurTimer.value != null) {
    window.clearTimeout(blurTimer.value)
    blurTimer.value = null
  }
  void refreshKeywordRecommendations(params.name)
}

function onSearchBlur(): void {
  blurTimer.value = window.setTimeout(() => {
    blurTimer.value = null
  }, 180)
}

function onKeywordClick(keyword: string): void {
  params.name = keyword
  onSearch()
}

function onSuggestionSelect(item: SearchSuggestionItem): void {
  params.name = item.value
  onSearch()
}

function fetchSuggestions(queryString: string, callback: (items: SearchSuggestionItem[]) => void): void {
  const normalizedQuery = queryString.trim()
  if (!normalizedQuery) {
    callback(recommendedKeywords.value.map((item) => ({ value: item })))
    return
  }

  if (suggestTimer.value != null) {
    window.clearTimeout(suggestTimer.value)
  }

  suggestTimer.value = window.setTimeout(async () => {
    try {
      const suggestions = await listSearchSuggestions(normalizedQuery, 10)
      callback(suggestions.map((item) => ({ value: item })))
    } catch {
      callback([])
    }
  }, 180)
}

function onCreate(): void {
  if (isMerchantOnly.value && currentMerchantId.value == null) {
    ElMessage.error('Current merchant ID is not available in session.')
    return
  }
  resetForm()
  dialogVisible.value = true
  void nextTick(() => {
    formRef.value?.clearValidate()
  })
}

function onEdit(row: ProductItem): void {
  if (!canManageRow(row)) {
    ElMessage.warning('You can only manage products under your own shop.')
    return
  }
  editingId.value = row.id
  form.shopId = isMerchantOnly.value ? (currentMerchantId.value ?? 0) : (row.shopId ?? 0)
  form.name = row.name ?? ''
  form.price = typeof row.price === 'number' ? row.price : 0
  form.stockQuantity = row.stockQuantity ?? 0
  form.categoryId = row.categoryId ?? 0
  form.brandId = row.brandId
  form.status = row.status ?? 1
  form.description = row.description ?? ''
  form.imageUrl = row.imageUrl ?? ''
  dialogVisible.value = true
  void nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function toggleStatus(row: ProductItem): Promise<void> {
  if (typeof row.id !== 'number') {
    return
  }
  if (!canManageRow(row)) {
    ElMessage.warning('You can only manage products under your own shop.')
    return
  }
  const nextStatus: 0 | 1 = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `Set product "${row.name}" to ${nextStatus === 1 ? 'Active' : 'Inactive'}?`,
      'Confirm',
      { type: 'warning' }
    )
    await updateProductStatus(row.id, nextStatus)
    ElMessage.success(`Product ${row.name} status updated.`)
    await loadProducts()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

function toPayload(): ProductUpsertPayload {
  const shopId = isMerchantOnly.value ? (currentMerchantId.value ?? 0) : form.shopId
  return {
    shopId,
    name: form.name.trim(),
    price: form.price,
    stockQuantity: form.stockQuantity,
    categoryId: form.categoryId,
    brandId: form.brandId,
    status: form.status ?? 1,
    description: form.description.trim() || undefined,
    imageUrl: form.imageUrl.trim() || undefined
  }
}

async function submitForm(): Promise<void> {
  if (formSubmitting.value) {
    return
  }

  if (isMerchantOnly.value && currentMerchantId.value == null) {
    ElMessage.error('Current merchant ID is not available in session.')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  formSubmitting.value = true
  try {
    const payload = toPayload()

    if (editingId.value) {
      await updateProduct(editingId.value, payload)
      ElMessage.success('Product updated.')
    } else {
      await createProduct(payload)
      ElMessage.success('Product created.')
    }

    dialogVisible.value = false
    await loadProducts()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to save product'
    ElMessage.error(message)
  } finally {
    formSubmitting.value = false
  }
}

function onDialogClosed(): void {
  resetForm()
  formRef.value?.clearValidate()
}

onMounted(() => {
  void loadProducts()
})

onBeforeUnmount(() => {
  if (blurTimer.value != null) {
    window.clearTimeout(blurTimer.value)
  }
  if (suggestTimer.value != null) {
    window.clearTimeout(suggestTimer.value)
  }
})
</script>

<template>
  <section class="glass-card panel">
    <div class="header">
      <h3>{{ isManagementMode ? 'Product Management' : 'Product Catalog' }}</h3>
      <div class="search-row">
        <el-autocomplete
          v-model="params.name"
          clearable
          placeholder="Search products, categories, or brands"
          :fetch-suggestions="fetchSuggestions"
          @focus="onSearchFocus"
          @blur="onSearchBlur"
          @select="onSuggestionSelect"
          @keyup.enter="onSearch"
        />
        <el-button round type="primary" @click="onSearch">Search</el-button>
        <router-link v-if="canEnterManagePage && !isManagementMode" class="inline-link" to="/catalog/manage">
          <el-button round type="success">Open Management</el-button>
        </router-link>
        <router-link v-if="isManagementMode" class="inline-link" to="/catalog">
          <el-button round>Back to Catalog</el-button>
        </router-link>
        <el-button v-if="canManageProducts" round type="success" @click="onCreate">New Product</el-button>
      </div>
    </div>

    <div v-if="!isManagementMode" class="keyword-board">
      <div class="keyword-block">
        <span class="keyword-title">Trending</span>
        <div class="keyword-list">
          <el-tag
            v-for="keyword in hotKeywords"
            :key="`hot-${keyword}`"
            class="keyword-chip"
            effect="light"
            @click="onKeywordClick(keyword)"
          >
            {{ keyword }}
          </el-tag>
        </div>
      </div>
      <div class="keyword-block">
        <span class="keyword-title">Recommended</span>
        <div class="keyword-list">
          <el-tag
            v-for="keyword in recommendedKeywords"
            :key="`rec-${keyword}`"
            class="keyword-chip"
            type="success"
            effect="plain"
            @click="onKeywordClick(keyword)"
          >
            {{ keyword }}
          </el-tag>
        </div>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="Name" prop="name" min-width="220" />
      <el-table-column label="Price" min-width="120">
        <template #default="scope">
          {{ formatPrice(scope.row.price) }}
        </template>
      </el-table-column>
      <el-table-column label="Stock" min-width="100" prop="stockQuantity" />
      <el-table-column label="Status" min-width="120">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" round>
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column v-if="canManageProducts" label="Actions" min-width="220">
        <template #default="scope">
          <el-button v-if="canManageRow(scope.row)" round size="small" @click="onEdit(scope.row)">Edit</el-button>
          <el-button v-if="canManageRow(scope.row)" round size="small" @click="toggleStatus(scope.row)">
            {{ scope.row.status === 1 ? 'Disable' : 'Enable' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="params.page"
        v-model:page-size="params.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        background
        layout="total, sizes, prev, pager, next"
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </div>
  </section>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" @closed="onDialogClosed">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="Shop ID" prop="shopId">
          <el-input-number v-model="form.shopId" :disabled="isMerchantOnly" :min="1" :step="1" controls-position="right" />
          <div v-if="isMerchantOnly" class="field-hint">Bound to current merchant account.</div>
        </el-form-item>

        <el-form-item label="Category ID" prop="categoryId">
          <el-input-number v-model="form.categoryId" :min="1" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="Brand ID (Optional)">
          <el-input-number v-model="form.brandId" :min="1" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="Status" prop="status">
          <el-select v-model="form.status">
            <el-option :value="1" label="Active" />
            <el-option :value="0" label="Inactive" />
          </el-select>
        </el-form-item>

        <el-form-item class="span-2" label="Product Name" prop="name">
          <el-input v-model="form.name" maxlength="255" />
        </el-form-item>

        <el-form-item label="Price" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="Stock Quantity" prop="stockQuantity">
          <el-input-number v-model="form.stockQuantity" :min="0" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item class="span-2" label="Image URL (Optional)">
          <el-input v-model="form.imageUrl" />
        </el-form-item>

        <el-form-item class="span-2" label="Description (Optional)">
          <el-input v-model="form.description" :rows="3" type="textarea" />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button round @click="dialogVisible = false">Cancel</el-button>
      <el-button :loading="formSubmitting" round type="primary" @click="submitForm">Save</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.panel {
  padding: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.header h3 {
  margin: 0;
}

.search-row {
  display: flex;
  gap: 8px;
  width: min(880px, 100%);
}

.search-row :deep(.el-autocomplete) {
  flex: 1;
}

.keyword-board {
  margin-bottom: 12px;
  display: grid;
  gap: 10px;
}

.keyword-block {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.keyword-title {
  color: var(--text-muted);
  min-width: 88px;
  font-size: 0.82rem;
}

.keyword-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-chip {
  cursor: pointer;
}

.inline-link {
  text-decoration: none;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.span-2 {
  grid-column: span 2;
}

.field-hint {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 0.75rem;
}

@media (max-width: 900px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-row {
    width: 100%;
    flex-wrap: wrap;
  }

  .keyword-title {
    min-width: auto;
  }

  .pager {
    justify-content: center;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}
</style>
