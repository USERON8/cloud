<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import ChartView from '../../../components/ChartView.vue'
import type {
  AdminInfo,
  AdminUpsertPayload,
  CategoryItem,
  MerchantAuthInfo,
  MerchantInfo,
  MerchantUpsertPayload,
  UserStatisticsOverview,
  UserSummary,
  UserUpsertPayload
} from '../../../types/domain'
import {
  deleteUser,
  deleteUsersBatch,
  searchUsers,
  updateUser,
  updateUserStatusBatch
} from '../../../api/user-management'
import {
  approveMerchant,
  createMerchant,
  deleteMerchant,
  getMerchants,
  rejectMerchant,
  updateMerchant
} from '../../../api/merchant'
import { listMerchantAuthByStatus, reviewMerchantAuth } from '../../../api/merchant-auth'
import {
  createCategory,
  deleteCategory,
  getCategories,
  updateCategory,
  updateCategorySort,
  updateCategoryStatus
} from '../../../api/category'
import {
  sendBatchNotification,
  sendStatusChangeNotification,
  sendSystemAnnouncement,
  sendWelcomeNotification
} from '../../../api/notification'
import {
  getActivityRanking,
  getActiveUsers,
  getGrowthRate,
  getRegistrationTrend,
  getRoleDistribution,
  getStatisticsOverview,
  getStatusDistribution
} from '../../../api/statistics'
import { createAdmin, deleteAdmin, getAdmins, resetAdminPassword, updateAdmin, updateAdminStatus } from '../../../api/admin'
import { confirm, toast } from '../../../utils/ui'

const tabs = [
  { key: 'users', label: 'User management' },
  { key: 'merchants', label: 'Merchant management' },
  { key: 'auth', label: 'Auth review' },
  { key: 'categories', label: 'Category management' },
  { key: 'notifications', label: 'Notification center' },
  { key: 'statistics', label: 'Statistics' },
  { key: 'admins', label: 'Administrators' }
]

const activeTab = ref('users')

function toNumber(value: string, fallback?: number): number | undefined {
  const num = Number(value)
  if (Number.isFinite(num)) {
    return num
  }
  return fallback
}

function normalizeStatus(value: unknown, label: string): number | null {
  const num = Number(value)
  if (num === 0 || num === 1) {
    return num
  }
  toast(`${label} status must be 0 or 1`)
  return null
}

function normalizeOptionalNumber(value: unknown): number | undefined {
  if (value == null || value === '') {
    return undefined
  }
  const num = Number(value)
  if (!Number.isFinite(num)) {
    return undefined
  }
  return num
}

function requireText(value: string, label: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) {
    toast(`Please enter ${label}`)
    return null
  }
  return trimmed
}

// Users
const userLoading = ref(false)
const userRows = ref<UserSummary[]>([])
const userTotal = ref(0)
const userQuery = reactive({
  page: 1,
  size: 10,
  username: '',
  email: '',
  phone: '',
  nickname: '',
  status: '',
  roleCode: ''
})
const userSelection = ref<number[]>([])
const userDialogVisible = ref(false)
const userEditId = ref<number | null>(null)
const userForm = reactive<UserUpsertPayload>({
  username: '',
  phone: '',
  nickname: '',
  email: '',
  avatarUrl: '',
  status: 1,
  roles: []
})
const userRolesInput = ref('')

const allUsersSelected = computed(
  () => userRows.value.length > 0 && userSelection.value.length === userRows.value.length
)
const userTotalPages = computed(() => Math.max(1, Math.ceil(userTotal.value / userQuery.size)))

async function loadUsers(): Promise<void> {
  userLoading.value = true
  try {
    const result = await searchUsers({
      page: userQuery.page,
      size: userQuery.size,
      username: userQuery.username || undefined,
      email: userQuery.email || undefined,
      phone: userQuery.phone || undefined,
      nickname: userQuery.nickname || undefined,
      status: userQuery.status === '' ? undefined : toNumber(userQuery.status),
      roleCode: userQuery.roleCode || undefined
    })
    userRows.value = result.records
    userTotal.value = result.total
    userSelection.value = []
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load users')
  } finally {
    userLoading.value = false
  }
}

function changeUserPage(delta: number): void {
  const next = Math.min(userTotalPages.value, Math.max(1, userQuery.page + delta))
  if (next === userQuery.page) return
  userQuery.page = next
  void loadUsers()
}

function toggleUserSelection(id: number): void {
  if (userSelection.value.includes(id)) {
    userSelection.value = userSelection.value.filter((item) => item !== id)
    return
  }
  userSelection.value = [...userSelection.value, id]
}

function toggleAllUsers(): void {
  if (allUsersSelected.value) {
    userSelection.value = []
    return
  }
  userSelection.value = userRows.value.map((item) => item.id)
}

function openUserEdit(row: UserSummary): void {
  userEditId.value = row.id
  userForm.username = row.username
  userForm.phone = row.phone
  userForm.nickname = row.nickname
  userForm.email = row.email
  userForm.avatarUrl = row.avatarUrl
  userForm.status = row.status ?? 1
  userForm.roles = row.roles ? [...row.roles] : []
  userRolesInput.value = row.roles?.join(', ') || ''
  userDialogVisible.value = true
}

function closeUserEdit(): void {
  userDialogVisible.value = false
  userEditId.value = null
  userRolesInput.value = ''
}

async function saveUser(): Promise<void> {
  if (!userEditId.value) {
    toast('Please select a user')
    return
  }
  const username = requireText(userForm.username, 'username')
  if (!username) return
  const status = normalizeStatus(userForm.status, 'User')
  if (status === null) return
  const payload: UserUpsertPayload = {
    ...userForm,
    username,
    phone: userForm.phone.trim(),
    nickname: userForm.nickname.trim(),
    email: userForm.email.trim(),
    avatarUrl: userForm.avatarUrl.trim(),
    status,
    roles: userRolesInput.value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
  try {
    await updateUser(userEditId.value, payload)
    toast('User updated', 'success')
    closeUserEdit()
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Update failed')
  }
}

async function confirmDeleteUser(row: UserSummary): Promise<void> {
  const ok = await confirm(`Delete user ${row.username}?`)
  if (!ok) return
  try {
    await deleteUser(row.id)
    toast('User deleted', 'success')
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Delete failed')
  }
}

async function batchUpdateUserStatus(status: number): Promise<void> {
  const ids = userSelection.value
  if (ids.length === 0) {
    toast('Please select users first')
    return
  }
  try {
    await updateUserStatusBatch(ids, status)
    toast('User status updated', 'success')
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Update failed')
  }
}

async function batchDeleteUsers(): Promise<void> {
  const ids = userSelection.value
  if (ids.length === 0) {
    toast('Please select users first')
    return
  }
  const ok = await confirm(`Delete ${ids.length} users?`)
  if (!ok) return
  try {
    await deleteUsersBatch(ids)
    toast('Batch delete completed', 'success')
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Delete failed')
  }
}

function userStatusText(status?: number): string {
  return status === 1 ? 'Enabled' : 'Disabled'
}
// Merchants
const merchantLoading = ref(false)
const merchantRows = ref<MerchantInfo[]>([])
const merchantTotal = ref(0)
const merchantQuery = reactive({
  page: 1,
  size: 10,
  status: '',
  auditStatus: ''
})
const merchantTotalPages = computed(() => Math.max(1, Math.ceil(merchantTotal.value / merchantQuery.size)))
const merchantDialogVisible = ref(false)
const merchantEditId = ref<number | null>(null)
const merchantForm = reactive<MerchantUpsertPayload>({
  username: '',
  password: '',
  merchantName: '',
  email: '',
  phone: '',
  status: 1
})
const merchantApproveRemark = ref('')
const merchantRejectReason = ref('')

async function loadMerchants(): Promise<void> {
  merchantLoading.value = true
  try {
    const status = merchantQuery.status ? toNumber(merchantQuery.status) : undefined
    const auditStatus = merchantQuery.auditStatus ? toNumber(merchantQuery.auditStatus) : undefined
    const result = await getMerchants({
      page: merchantQuery.page,
      size: merchantQuery.size,
      status,
      auditStatus
    })
    merchantRows.value = result.records
    merchantTotal.value = result.total
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load merchants')
  } finally {
    merchantLoading.value = false
  }
}

function changeMerchantPage(delta: number): void {
  const next = Math.min(merchantTotalPages.value, Math.max(1, merchantQuery.page + delta))
  if (next === merchantQuery.page) return
  merchantQuery.page = next
  void loadMerchants()
}

function openMerchantEdit(row: MerchantInfo): void {
  merchantEditId.value = row.id
  merchantForm.username = row.username
  merchantForm.password = ''
  merchantForm.merchantName = row.merchantName
  merchantForm.email = row.email
  merchantForm.phone = row.phone
  merchantForm.status = row.status ?? 1
  merchantDialogVisible.value = true
}

function openMerchantCreate(): void {
  merchantEditId.value = null
  merchantForm.username = ''
  merchantForm.password = ''
  merchantForm.merchantName = ''
  merchantForm.email = ''
  merchantForm.phone = ''
  merchantForm.status = 1
  merchantDialogVisible.value = true
}

function closeMerchantDialog(): void {
  merchantDialogVisible.value = false
  merchantEditId.value = null
}

async function saveMerchant(): Promise<void> {
  const username = requireText(merchantForm.username, 'merchant username')
  if (!username) return
  const merchantName = requireText(merchantForm.merchantName, 'merchant name')
  if (!merchantName) return
  const password = merchantForm.password?.trim() || ''
  if (!merchantEditId.value && !password) {
    toast('Please enter a merchant password')
    return
  }
  const status = normalizeStatus(merchantForm.status, 'Merchant')
  if (status === null) return
  const payload: MerchantUpsertPayload = {
    ...merchantForm,
    username,
    password: password || undefined,
    merchantName,
    email: merchantForm.email.trim(),
    phone: merchantForm.phone.trim(),
    status
  }
  try {
    if (merchantEditId.value) {
      await updateMerchant(merchantEditId.value, payload)
      toast('Merchant updated', 'success')
    } else {
      await createMerchant(payload)
      toast('Merchant created', 'success')
    }
    closeMerchantDialog()
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Save failed')
  }
}

async function confirmDeleteMerchant(row: MerchantInfo): Promise<void> {
  const name = row.merchantName || row.username || 'Merchant'
  const ok = await confirm(`Delete ${name}?`)
  if (!ok) return
  try {
    await deleteMerchant(row.id)
    toast('Merchant deleted', 'success')
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Delete failed')
  }
}

async function approveMerchantRow(row: MerchantInfo): Promise<void> {
  try {
    await approveMerchant(row.id, merchantApproveRemark.value || undefined)
    toast('Merchant approved', 'success')
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Review failed')
  }
}

async function rejectMerchantRow(row: MerchantInfo): Promise<void> {
  if (!merchantRejectReason.value.trim()) {
    toast('Please enter a rejection reason')
    return
  }
  try {
    await rejectMerchant(row.id, merchantRejectReason.value.trim())
    toast('Merchant rejected', 'success')
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Reject failed')
  }
}

function merchantStatusText(status?: number): string {
  if (status === 1) return 'Enabled'
  if (status === 0) return 'Disabled'
  return 'Unknown'
}

// Merchant Auth
const authLoading = ref(false)
const authRows = ref<MerchantAuthInfo[]>([])
const authStatusFilter = ref('0')
const authRemark = ref('')

async function loadMerchantAuth(): Promise<void> {
  authLoading.value = true
  try {
    const status = toNumber(authStatusFilter.value, 0) || 0
    authRows.value = await listMerchantAuthByStatus(status)
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load auth reviews')
  } finally {
    authLoading.value = false
  }
}

async function reviewMerchantAuthRow(row: MerchantAuthInfo, status: number): Promise<void> {
  try {
    await reviewMerchantAuth(row.merchantId!, status, authRemark.value || undefined)
    toast('Auth review submitted', 'success')
    await loadMerchantAuth()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Review failed')
  }
}
// Categories
const categoryLoading = ref(false)
const categoryRows = ref<CategoryItem[]>([])
const categoryTotal = ref(0)
const categoryQuery = reactive({
  page: 1,
  size: 10
})
const categoryTotalPages = computed(() => Math.max(1, Math.ceil(categoryTotal.value / categoryQuery.size)))
const categoryDialogVisible = ref(false)
const categoryEditId = ref<number | null>(null)
const categoryForm = reactive<CategoryItem>({
  name: '',
  description: '',
  parentId: undefined,
  status: 1,
  sortOrder: undefined
})

async function loadCategories(): Promise<void> {
  categoryLoading.value = true
  try {
    const result = await getCategories({ page: categoryQuery.page, size: categoryQuery.size })
    categoryRows.value = result.records
    categoryTotal.value = result.total
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load categories')
  } finally {
    categoryLoading.value = false
  }
}

function changeCategoryPage(delta: number): void {
  const next = Math.min(categoryTotalPages.value, Math.max(1, categoryQuery.page + delta))
  if (next === categoryQuery.page) return
  categoryQuery.page = next
  void loadCategories()
}

function openCategoryEdit(row: CategoryItem): void {
  categoryEditId.value = row.id ?? null
  categoryForm.name = row.name
  categoryForm.description = row.description
  categoryForm.parentId = row.parentId
  categoryForm.status = row.status ?? 1
  categoryForm.sortOrder = row.sortOrder
  categoryDialogVisible.value = true
}

function openCategoryCreate(): void {
  categoryEditId.value = null
  categoryForm.name = ''
  categoryForm.description = ''
  categoryForm.parentId = undefined
  categoryForm.status = 1
  categoryForm.sortOrder = undefined
  categoryDialogVisible.value = true
}

function closeCategoryDialog(): void {
  categoryDialogVisible.value = false
  categoryEditId.value = null
}

async function saveCategory(): Promise<void> {
  const name = requireText(categoryForm.name, 'category name')
  if (!name) return
  const status = normalizeStatus(categoryForm.status, 'Category')
  if (status === null) return
  const payload: CategoryItem = {
    ...categoryForm,
    name,
    description: categoryForm.description.trim(),
    parentId: normalizeOptionalNumber(categoryForm.parentId),
    status,
    sortOrder: normalizeOptionalNumber(categoryForm.sortOrder)
  }
  try {
    if (categoryEditId.value) {
      await updateCategory(categoryEditId.value, payload)
      toast('Category updated', 'success')
    } else {
      await createCategory(payload)
      toast('Category created', 'success')
    }
    closeCategoryDialog()
    await loadCategories()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Save failed')
  }
}

async function deleteCategoryRow(row: CategoryItem): Promise<void> {
  if (!row.id) return
  const ok = await confirm(`Delete category ${row.name}?`)
  if (!ok) return
  try {
    await deleteCategory(row.id)
    toast('Category deleted', 'success')
    await loadCategories()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Delete failed')
  }
}

async function updateCategoryRowStatus(row: CategoryItem, status: number): Promise<void> {
  if (!row.id) return
  try {
    await updateCategoryStatus(row.id, status)
    toast('Status updated', 'success')
    await loadCategories()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Update failed')
  }
}

async function updateCategoryRowSort(row: CategoryItem): Promise<void> {
  if (!row.id) return
  const sort = Number(row.sortOrder)
  if (!Number.isFinite(sort)) {
    toast('Please enter a sort value')
    return
  }
  row.sortOrder = sort
  try {
    await updateCategorySort(row.id, row.sortOrder)
    toast('Sort order updated', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Update failed')
  }
}

// Notifications
const welcomeUserId = ref('')
const statusChangeUserId = ref('')
const statusChangeForm = reactive({ newStatus: 1, reason: '' })
const batchUserIds = ref('')
const batchForm = reactive({ title: '', content: '' })
const announcementForm = reactive({ title: '', content: '' })

function parseIdList(raw: string): number[] {
  return raw
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value) && value > 0)
}

async function sendWelcome(): Promise<void> {
  const id = Number(welcomeUserId.value)
  if (!id) {
    toast('Please enter a valid user ID')
    return
  }
  try {
    await sendWelcomeNotification(id)
    toast('Welcome notification sent', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Send failed')
  }
}

async function sendStatusChange(): Promise<void> {
  const id = Number(statusChangeUserId.value)
  if (!id) {
    toast('Please enter a valid user ID')
    return
  }
  try {
    await sendStatusChangeNotification(id, {
      newStatus: statusChangeForm.newStatus,
      reason: statusChangeForm.reason || undefined
    })
    toast('Status notification sent', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Send failed')
  }
}

async function sendBatch(): Promise<void> {
  const ids = parseIdList(batchUserIds.value)
  if (ids.length === 0) {
    toast('Please enter a list of user IDs')
    return
  }
  try {
    await sendBatchNotification({
      userIds: ids,
      title: batchForm.title,
      content: batchForm.content
    })
    toast('Batch notification sent', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Send failed')
  }
}

async function sendAnnouncement(): Promise<void> {
  try {
    await sendSystemAnnouncement({
      title: announcementForm.title,
      content: announcementForm.content
    })
    toast('Announcement sent', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Send failed')
  }
}
// Statistics
const statsLoading = ref(false)
const statsOverview = ref<UserStatisticsOverview | null>(null)
const roleDistribution = ref<Record<string, number>>({})
const statusDistribution = ref<Record<string, number>>({})
const registrationTrend = ref<Record<string, number>>({})
const activityRanking = ref<Record<string, number>>({})
const activeUsers = ref<number | null>(null)
const growthRate = ref<number | null>(null)

const trendList = computed(() =>
  Object.entries(registrationTrend.value).sort(([left], [right]) => left.localeCompare(right))
)
const rankingList = computed(() =>
  Object.entries(activityRanking.value).sort((left, right) => Number(right[1]) - Number(left[1]))
)

const roleChartData = computed(() => {
  const entries = Object.entries(roleDistribution.value)
  if (entries.length === 0) return null
  return {
    series: [
      {
        data: entries.map(([name, value]) => ({ name, value }))
      }
    ]
  }
})

const statusChartData = computed(() => {
  const entries = Object.entries(statusDistribution.value)
  if (entries.length === 0) return null
  return {
    series: [
      {
        data: entries.map(([name, value]) => ({ name, value }))
      }
    ]
  }
})

const trendChartData = computed(() => {
  if (trendList.value.length === 0) return null
  return {
    categories: trendList.value.map((entry) => entry[0]),
    series: [
      {
        name: 'Registrations',
        data: trendList.value.map((entry) => entry[1])
      }
    ]
  }
})

const rankingChartData = computed(() => {
  if (rankingList.value.length === 0) return null
  return {
    categories: rankingList.value.map((entry) => entry[0]),
    series: [
      {
        name: 'Activity',
        data: rankingList.value.map((entry) => entry[1])
      }
    ]
  }
})

const chartOpts = {
  legend: { position: 'bottom' },
  padding: [12, 12, 0, 12]
}

async function loadStatistics(): Promise<void> {
  statsLoading.value = true
  try {
    const [overview, roleDist, statusDist, trend, active, growth, ranking] = await Promise.all([
      getStatisticsOverview(),
      getRoleDistribution(),
      getStatusDistribution(),
      getRegistrationTrend(30),
      getActiveUsers(7),
      getGrowthRate(7),
      getActivityRanking(10, 30)
    ])
    statsOverview.value = overview
    roleDistribution.value = roleDist
    statusDistribution.value = statusDist
    registrationTrend.value = trend
    activeUsers.value = active
    growthRate.value = growth
    activityRanking.value = ranking
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load statistics')
  } finally {
    statsLoading.value = false
  }
}

// Admins
const adminLoading = ref(false)
const adminRows = ref<AdminInfo[]>([])
const adminTotal = ref(0)
const adminQuery = reactive({
  page: 1,
  size: 10
})
const adminTotalPages = computed(() => Math.max(1, Math.ceil(adminTotal.value / adminQuery.size)))
const adminDialogVisible = ref(false)
const adminEditId = ref<number | null>(null)
const adminForm = reactive<AdminUpsertPayload>({
  username: '',
  password: '',
  phone: '',
  realName: '',
  role: '',
  status: 1
})

async function loadAdmins(): Promise<void> {
  adminLoading.value = true
  try {
    const result = await getAdmins({ page: adminQuery.page, size: adminQuery.size })
    adminRows.value = result.records
    adminTotal.value = result.total
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load administrators')
  } finally {
    adminLoading.value = false
  }
}

function changeAdminPage(delta: number): void {
  const next = Math.min(adminTotalPages.value, Math.max(1, adminQuery.page + delta))
  if (next === adminQuery.page) return
  adminQuery.page = next
  void loadAdmins()
}

function openAdminEdit(row: AdminInfo): void {
  adminEditId.value = row.id
  adminForm.username = row.username
  adminForm.password = ''
  adminForm.phone = row.phone
  adminForm.realName = row.realName
  adminForm.role = row.role
  adminForm.status = row.status ?? 1
  adminDialogVisible.value = true
}

function openAdminCreate(): void {
  adminEditId.value = null
  adminForm.username = ''
  adminForm.password = ''
  adminForm.phone = ''
  adminForm.realName = ''
  adminForm.role = ''
  adminForm.status = 1
  adminDialogVisible.value = true
}

function closeAdminDialog(): void {
  adminDialogVisible.value = false
  adminEditId.value = null
}

async function saveAdmin(): Promise<void> {
  const username = requireText(adminForm.username, 'username')
  if (!username) return
  const realName = requireText(adminForm.realName, 'real name')
  if (!realName) return
  const password = adminForm.password?.trim() || ''
  if (!adminEditId.value && !password) {
    toast('Please enter an administrator password')
    return
  }
  const status = normalizeStatus(adminForm.status, 'Administrator')
  if (status === null) return
  const payload: AdminUpsertPayload = {
    ...adminForm,
    username,
    password: password || undefined,
    phone: adminForm.phone.trim(),
    realName,
    role: adminForm.role.trim(),
    status
  }
  try {
    if (adminEditId.value) {
      await updateAdmin(adminEditId.value, payload)
      toast('Administrator updated', 'success')
    } else {
      await createAdmin(payload)
      toast('Administrator created', 'success')
    }
    closeAdminDialog()
    await loadAdmins()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Save failed')
  }
}

async function confirmDeleteAdmin(row: AdminInfo): Promise<void> {
  const ok = await confirm(`Delete administrator ${row.username}?`)
  if (!ok) return
  try {
    await deleteAdmin(row.id)
    toast('Administrator deleted', 'success')
    await loadAdmins()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Delete failed')
  }
}

async function updateAdminRowStatus(row: AdminInfo, status: number): Promise<void> {
  try {
    await updateAdminStatus(row.id, status)
    toast('Status updated', 'success')
    await loadAdmins()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Update failed')
  }
}

async function resetPassword(row: AdminInfo): Promise<void> {
  const ok = await confirm(`Reset password for ${row.username}?`)
  if (!ok) return
  try {
    const temporaryPassword = await resetAdminPassword(row.id)
    toast(`Temporary password: ${temporaryPassword}`, 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Reset failed')
  }
}

function onTabChange(key: string): void {
  activeTab.value = key
  if (key === 'users' && userRows.value.length === 0) void loadUsers()
  if (key === 'merchants' && merchantRows.value.length === 0) void loadMerchants()
  if (key === 'auth' && authRows.value.length === 0) void loadMerchantAuth()
  if (key === 'categories' && categoryRows.value.length === 0) void loadCategories()
  if (key === 'statistics' && !statsOverview.value) void loadStatistics()
  if (key === 'admins' && adminRows.value.length === 0) void loadAdmins()
}

watch(
  () => [userQuery.username, userQuery.email, userQuery.phone, userQuery.nickname, userQuery.status, userQuery.roleCode],
  () => {
    userQuery.page = 1
  }
)

watch(
  () => [merchantQuery.status, merchantQuery.auditStatus],
  () => {
    merchantQuery.page = 1
  }
)

watch(
  () => authStatusFilter.value,
  () => {
    authRows.value = []
    void loadMerchantAuth()
  }
)

onMounted(() => {
  void loadUsers()
})
</script>
<template>
  <AppShell title="Admin Center">
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        @click="onTabChange(tab.key)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view v-if="activeTab === 'users'" class="panel glass-card">
      <view class="toolbar">
        <input v-model="userQuery.username" class="input" placeholder="Username" />
        <input v-model="userQuery.email" class="input" placeholder="Email" />
        <input v-model="userQuery.phone" class="input" placeholder="Phone" />
        <input v-model="userQuery.nickname" class="input" placeholder="Nickname" />
        <input v-model="userQuery.status" class="input" placeholder="Status (0/1)" />
        <input v-model="userQuery.roleCode" class="input" placeholder="Role code" />
        <button class="btn-primary" @click="loadUsers">Search</button>
      </view>

      <view class="toolbar">
        <button class="btn-outline" @click="toggleAllUsers">{{ allUsersSelected ? 'Clear selection' : 'Select all' }}</button>
        <button class="btn-outline" @click="batchUpdateUserStatus(1)">Batch enable</button>
        <button class="btn-outline" @click="batchUpdateUserStatus(0)">Batch disable</button>
        <button class="btn-outline" @click="batchDeleteUsers">Batch delete</button>
      </view>
      <text class="muted">Total {{ userTotal }} records</text>

      <view v-if="userLoading" class="muted">Loading...</view>
      <view v-else class="list">
        <view v-for="row in userRows" :key="row.id" class="row">
          <view class="row-head">
            <checkbox :checked="userSelection.includes(row.id)" @click="toggleUserSelection(row.id)" />
            <text class="name">{{ row.username }}</text>
            <text class="muted">#{{ row.id }}</text>
          </view>
          <text class="muted">Nickname: {{ row.nickname || '--' }}</text>
          <text class="muted">Email: {{ row.email || '--' }}</text>
          <text class="muted">Roles: {{ row.roles?.join(', ') || '--' }}</text>
          <text class="muted">Status: {{ userStatusText(row.status) }}</text>
          <view class="actions">
            <button class="btn-outline" @click="openUserEdit(row)">Edit</button>
            <button class="btn-outline" @click="confirmDeleteUser(row)">Delete</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="userQuery.page <= 1" @click="changeUserPage(-1)">Previous</button>
        <text class="muted">Page {{ userQuery.page }} / {{ userTotalPages }}</text>
        <button class="btn-outline" :disabled="userQuery.page >= userTotalPages" @click="changeUserPage(1)">Next</button>
      </view>

      <view v-if="userDialogVisible" class="dialog">
        <text class="section-title">Edit user</text>
        <view class="form">
          <input v-model="userForm.username" class="input" placeholder="Username" />
          <input v-model="userForm.nickname" class="input" placeholder="Nickname" />
          <input v-model="userForm.email" class="input" placeholder="Email" />
          <input v-model="userForm.phone" class="input" placeholder="Phone" />
          <input v-model="userForm.avatarUrl" class="input" placeholder="Avatar URL" />
          <input v-model="userRolesInput" class="input" placeholder="Roles (comma separated)" />
          <input v-model="userForm.status" class="input" placeholder="Status (0/1)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveUser">Save</button>
          <button class="btn-outline" @click="closeUserEdit">Cancel</button>
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'merchants'" class="panel glass-card">
      <view class="toolbar">
        <input v-model="merchantQuery.status" class="input" placeholder="Status (optional)" />
        <input v-model="merchantQuery.auditStatus" class="input" placeholder="Audit status (optional)" />
        <button class="btn-primary" @click="loadMerchants">Search</button>
        <button class="btn-outline" @click="openMerchantCreate">New merchant</button>
      </view>
      <view class="toolbar">
        <input v-model="merchantApproveRemark" class="input" placeholder="Approval remark (optional)" />
        <input v-model="merchantRejectReason" class="input" placeholder="Rejection reason" />
      </view>
      <text class="muted">Total {{ merchantTotal }} records</text>

      <view v-if="merchantLoading" class="muted">Loading...</view>
      <view v-else class="list">
        <view v-for="row in merchantRows" :key="row.id" class="row">
          <text class="name">{{ row.merchantName || row.username || 'Merchant' }}</text>
          <text class="muted">Email: {{ row.email || '--' }}</text>
          <text class="muted">Phone: {{ row.phone || '--' }}</text>
          <text class="muted">Status: {{ merchantStatusText(row.status) }}</text>
          <text class="muted">Auth: {{ merchantStatusText(row.authStatus) }}</text>
          <view class="actions">
            <button class="btn-outline" @click="openMerchantEdit(row)">Edit</button>
            <button class="btn-outline" @click="approveMerchantRow(row)">Approve</button>
            <button class="btn-outline" @click="rejectMerchantRow(row)">Reject</button>
            <button class="btn-outline" @click="confirmDeleteMerchant(row)">Delete</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="merchantQuery.page <= 1" @click="changeMerchantPage(-1)">Previous</button>
        <text class="muted">Page {{ merchantQuery.page }} / {{ merchantTotalPages }}</text>
        <button class="btn-outline" :disabled="merchantQuery.page >= merchantTotalPages" @click="changeMerchantPage(1)">Next</button>
      </view>

      <view v-if="merchantDialogVisible" class="dialog">
        <text class="section-title">{{ merchantEditId ? 'Edit merchant' : 'New merchant' }}</text>
        <view class="form">
          <input v-model="merchantForm.username" class="input" placeholder="Merchant username" />
          <input
            v-model="merchantForm.password"
            class="input"
            :placeholder="merchantEditId ? 'Password (leave blank to keep current)' : 'Password'"
            password
          />
          <input v-model="merchantForm.merchantName" class="input" placeholder="Merchant name" />
          <input v-model="merchantForm.email" class="input" placeholder="Email" />
          <input v-model="merchantForm.phone" class="input" placeholder="Phone" />
          <input v-model="merchantForm.status" class="input" placeholder="Status (0/1)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveMerchant">Save</button>
          <button class="btn-outline" @click="closeMerchantDialog">Cancel</button>
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'auth'" class="panel glass-card">
      <view class="toolbar">
        <input v-model="authStatusFilter" class="input" placeholder="Status (0 pending / 1 approved / 2 rejected)" />
        <input v-model="authRemark" class="input" placeholder="Review remark" />
        <button class="btn-primary" @click="loadMerchantAuth">Search</button>
      </view>

      <view v-if="authLoading" class="muted">Loading...</view>
      <view v-else class="list">
        <view v-for="row in authRows" :key="row.id" class="row">
          <text class="name">Merchant ID: {{ row.merchantId }}</text>
          <text class="muted">License: {{ row.businessLicenseNumber || '--' }}</text>
          <text class="muted">Status: {{ merchantStatusText(row.authStatus) }}</text>
          <view class="actions">
            <button class="btn-outline" @click="reviewMerchantAuthRow(row, 1)">Approve</button>
            <button class="btn-outline" @click="reviewMerchantAuthRow(row, 2)">Reject</button>
          </view>
        </view>
      </view>
    </view>
    <view v-if="activeTab === 'categories'" class="panel glass-card">
      <view class="toolbar">
        <button class="btn-outline" @click="openCategoryCreate">New category</button>
        <button class="btn-outline" @click="loadCategories">Refresh</button>
      </view>
      <text class="muted">Total {{ categoryTotal }} records</text>

      <view v-if="categoryLoading" class="muted">Loading...</view>
      <view v-else class="list">
        <view v-for="row in categoryRows" :key="row.id" class="row">
          <text class="name">{{ row.name }}</text>
          <text class="muted">Description: {{ row.description || '--' }}</text>
          <text class="muted">Status: {{ row.status === 1 ? 'Enabled' : 'Disabled' }}</text>
          <view class="row-inline">
            <input v-model="row.sortOrder" class="input small" placeholder="Sort order" />
            <button class="btn-outline" @click="updateCategoryRowSort(row)">Save sort</button>
          </view>
          <view class="actions">
            <button class="btn-outline" @click="openCategoryEdit(row)">Edit</button>
            <button class="btn-outline" @click="updateCategoryRowStatus(row, 1)">Enable</button>
            <button class="btn-outline" @click="updateCategoryRowStatus(row, 0)">Disable</button>
            <button class="btn-outline" @click="deleteCategoryRow(row)">Delete</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="categoryQuery.page <= 1" @click="changeCategoryPage(-1)">Previous</button>
        <text class="muted">Page {{ categoryQuery.page }} / {{ categoryTotalPages }}</text>
        <button class="btn-outline" :disabled="categoryQuery.page >= categoryTotalPages" @click="changeCategoryPage(1)">Next</button>
      </view>

      <view v-if="categoryDialogVisible" class="dialog">
        <text class="section-title">{{ categoryEditId ? 'Edit category' : 'New category' }}</text>
        <view class="form">
          <input v-model="categoryForm.name" class="input" placeholder="Category name" />
          <input v-model="categoryForm.description" class="input" placeholder="Category description" />
          <input v-model="categoryForm.parentId" class="input" placeholder="Parent ID (optional)" />
          <input v-model="categoryForm.status" class="input" placeholder="Status (0/1)" />
          <input v-model="categoryForm.sortOrder" class="input" placeholder="Sort order (optional)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveCategory">Save</button>
          <button class="btn-outline" @click="closeCategoryDialog">Cancel</button>
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'notifications'" class="panel glass-card">
      <view class="section">
        <text class="section-title">Welcome notification</text>
        <view class="row-inline">
          <input v-model="welcomeUserId" class="input" placeholder="User ID" />
          <button class="btn-primary" @click="sendWelcome">Send</button>
        </view>
      </view>

      <view class="section">
        <text class="section-title">Status change notification</text>
        <view class="row-inline">
          <input v-model="statusChangeUserId" class="input" placeholder="User ID" />
          <input v-model="statusChangeForm.newStatus" class="input" placeholder="New status (0/1)" />
        </view>
        <textarea v-model="statusChangeForm.reason" class="textarea" placeholder="Reason (optional)" />
        <button class="btn-primary" @click="sendStatusChange">Send</button>
      </view>

      <view class="section">
        <text class="section-title">Batch notification</text>
        <input v-model="batchUserIds" class="input" placeholder="User ID list (comma separated)" />
        <input v-model="batchForm.title" class="input" placeholder="Title" />
        <textarea v-model="batchForm.content" class="textarea" placeholder="Content" />
        <button class="btn-primary" @click="sendBatch">Send</button>
      </view>

      <view class="section">
        <text class="section-title">System announcement</text>
        <input v-model="announcementForm.title" class="input" placeholder="Title" />
        <textarea v-model="announcementForm.content" class="textarea" placeholder="Content" />
        <button class="btn-primary" @click="sendAnnouncement">Send</button>
      </view>
    </view>

    <view v-if="activeTab === 'statistics'" class="panel glass-card">
      <view class="toolbar">
        <button class="btn-outline" @click="loadStatistics">Refresh statistics</button>
      </view>
      <view v-if="statsLoading" class="muted">Loading...</view>
      <view v-else class="section">
        <text class="section-title">Overview</text>
        <view class="stats-grid">
          <view class="stat-card">
            <text class="stat-label">Total users</text>
            <text class="stat-value">{{ statsOverview?.totalUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">New today</text>
            <text class="stat-value">{{ statsOverview?.todayNewUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">New this month</text>
            <text class="stat-value">{{ statsOverview?.monthNewUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">Active users (7d)</text>
            <text class="stat-value">{{ activeUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">Growth rate (7d)</text>
            <text class="stat-value">{{ growthRate ?? '--' }}</text>
          </view>
        </view>
      </view>
      <view class="section">
        <text class="section-title">Role distribution</text>
        <view v-if="!roleChartData" class="muted">No data available</view>
        <ChartView v-else class="chart" type="pie" canvasId="roleChart" :chartData="roleChartData" :opts="chartOpts" />
      </view>
      <view class="section">
        <text class="section-title">Status distribution</text>
        <view v-if="!statusChartData" class="muted">No data available</view>
        <ChartView
          v-else
          class="chart"
          type="ring"
          canvasId="statusChart"
          :chartData="statusChartData"
          :opts="chartOpts"
        />
      </view>
      <view class="section">
        <text class="section-title">Registration trend (30d)</text>
        <view v-if="!trendChartData" class="muted">No data available</view>
        <ChartView v-else class="chart" type="line" canvasId="trendChart" :chartData="trendChartData" :opts="chartOpts" />
      </view>
      <view class="section">
        <text class="section-title">Activity ranking</text>
        <view v-if="!rankingChartData" class="muted">No data available</view>
        <ChartView
          v-else
          class="chart"
          type="column"
          canvasId="rankingChart"
          :chartData="rankingChartData"
          :opts="chartOpts"
        />
      </view>
    </view>

    <view v-if="activeTab === 'admins'" class="panel glass-card">
      <view class="toolbar">
        <button class="btn-outline" @click="openAdminCreate">New administrator</button>
        <button class="btn-outline" @click="loadAdmins">Refresh</button>
      </view>
      <text class="muted">Total {{ adminTotal }} records</text>

      <view v-if="adminLoading" class="muted">Loading...</view>
      <view v-else class="list">
        <view v-for="row in adminRows" :key="row.id" class="row">
          <text class="name">{{ row.username }}</text>
          <text class="muted">Name: {{ row.realName || '--' }}</text>
          <text class="muted">Role: {{ row.role || '--' }}</text>
          <text class="muted">Status: {{ row.status === 1 ? 'Enabled' : 'Disabled' }}</text>
          <view class="actions">
            <button class="btn-outline" @click="openAdminEdit(row)">Edit</button>
            <button class="btn-outline" @click="updateAdminRowStatus(row, 1)">Enable</button>
            <button class="btn-outline" @click="updateAdminRowStatus(row, 0)">Disable</button>
            <button class="btn-outline" @click="resetPassword(row)">Reset password</button>
            <button class="btn-outline" @click="confirmDeleteAdmin(row)">Delete</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="adminQuery.page <= 1" @click="changeAdminPage(-1)">Previous</button>
        <text class="muted">Page {{ adminQuery.page }} / {{ adminTotalPages }}</text>
        <button class="btn-outline" :disabled="adminQuery.page >= adminTotalPages" @click="changeAdminPage(1)">Next</button>
      </view>

      <view v-if="adminDialogVisible" class="dialog">
        <text class="section-title">{{ adminEditId ? 'Edit administrator' : 'New administrator' }}</text>
        <view class="form">
          <input v-model="adminForm.username" class="input" placeholder="Username" />
          <input
            v-model="adminForm.password"
            class="input"
            :placeholder="adminEditId ? 'Password (leave blank to keep current)' : 'Password'"
            password
          />
          <input v-model="adminForm.realName" class="input" placeholder="Real name" />
          <input v-model="adminForm.phone" class="input" placeholder="Phone" />
          <input v-model="adminForm.role" class="input" placeholder="Role" />
          <input v-model="adminForm.status" class="input" placeholder="Status (0/1)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveAdmin">Save</button>
          <button class="btn-outline" @click="closeAdminDialog">Cancel</button>
        </view>
      </view>
    </view>
  </AppShell>
</template>
<style scoped>
.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  margin-bottom: 12px;
}

.tab {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.tab.active {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 18px rgba(32, 40, 53, 0.08);
}

.panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.input {
  background: #fff;
  border-radius: 10px;
  padding: 6px 10px;
  font-size: 12px;
}

.input.small {
  width: 80px;
}

.textarea {
  background: #fff;
  border-radius: 10px;
  padding: 8px 10px;
  font-size: 12px;
  min-height: 72px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.row-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name {
  font-size: 14px;
  font-weight: 600;
}

.muted {
  font-size: 12px;
  color: var(--text-muted);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog {
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed rgba(36, 107, 255, 0.2);
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form {
  display: grid;
  gap: 8px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.stat-card {
  padding: 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chart {
  width: 100%;
  height: 260px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
}
</style>


