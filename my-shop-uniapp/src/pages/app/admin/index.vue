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
  { key: 'users', label: '用户管理' },
  { key: 'merchants', label: '商家管理' },
  { key: 'auth', label: '资质审核' },
  { key: 'categories', label: '分类管理' },
  { key: 'notifications', label: '通知中心' },
  { key: 'statistics', label: '数据统计' },
  { key: 'admins', label: '管理员' }
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
  toast(`${label}状态需为 0 或 1`)
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
    toast(`请输入${label}`)
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
    toast(error instanceof Error ? error.message : '加载用户失败')
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
    toast('请选择用户')
    return
  }
  const username = requireText(userForm.username, '用户名')
  if (!username) return
  const status = normalizeStatus(userForm.status, '用户')
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
    toast('用户已更新', 'success')
    closeUserEdit()
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function confirmDeleteUser(row: UserSummary): Promise<void> {
  const ok = await confirm(`确认删除用户 ${row.username}？`)
  if (!ok) return
  try {
    await deleteUser(row.id)
    toast('用户已删除', 'success')
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

async function batchUpdateUserStatus(status: number): Promise<void> {
  const ids = userSelection.value
  if (ids.length === 0) {
    toast('请先选择用户')
    return
  }
  try {
    await updateUserStatusBatch(ids, status)
    toast('用户状态已更新', 'success')
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function batchDeleteUsers(): Promise<void> {
  const ids = userSelection.value
  if (ids.length === 0) {
    toast('请先选择用户')
    return
  }
  const ok = await confirm(`确认删除 ${ids.length} 个用户？`)
  if (!ok) return
  try {
    await deleteUsersBatch(ids)
    toast('批量删除完成', 'success')
    await loadUsers()
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

function userStatusText(status?: number): string {
  return status === 1 ? '启用' : '停用'
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
    toast(error instanceof Error ? error.message : '加载商家失败')
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
  const username = requireText(merchantForm.username, '商家用户名')
  if (!username) return
  const merchantName = requireText(merchantForm.merchantName, '商家名称')
  if (!merchantName) return
  const password = merchantForm.password?.trim() || ''
  if (!merchantEditId.value && !password) {
    toast('请输入商家密码')
    return
  }
  const status = normalizeStatus(merchantForm.status, '商家')
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
      toast('商家已更新', 'success')
    } else {
      await createMerchant(payload)
      toast('商家已创建', 'success')
    }
    closeMerchantDialog()
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : '保存失败')
  }
}

async function confirmDeleteMerchant(row: MerchantInfo): Promise<void> {
  const name = row.merchantName || row.username || '商家'
  const ok = await confirm(`确认删除 ${name}？`)
  if (!ok) return
  try {
    await deleteMerchant(row.id)
    toast('商家已删除', 'success')
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

async function approveMerchantRow(row: MerchantInfo): Promise<void> {
  try {
    await approveMerchant(row.id, merchantApproveRemark.value || undefined)
    toast('已通过', 'success')
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : '审核失败')
  }
}

async function rejectMerchantRow(row: MerchantInfo): Promise<void> {
  if (!merchantRejectReason.value.trim()) {
    toast('请输入驳回原因')
    return
  }
  try {
    await rejectMerchant(row.id, merchantRejectReason.value.trim())
    toast('已驳回', 'success')
    await loadMerchants()
  } catch (error) {
    toast(error instanceof Error ? error.message : '驳回失败')
  }
}

function merchantStatusText(status?: number): string {
  if (status === 1) return '启用'
  if (status === 0) return '停用'
  return '未知'
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
    toast(error instanceof Error ? error.message : '加载审核失败')
  } finally {
    authLoading.value = false
  }
}

async function reviewMerchantAuthRow(row: MerchantAuthInfo, status: number): Promise<void> {
  try {
    await reviewMerchantAuth(row.merchantId!, status, authRemark.value || undefined)
    toast('审核已提交', 'success')
    await loadMerchantAuth()
  } catch (error) {
    toast(error instanceof Error ? error.message : '审核失败')
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
    toast(error instanceof Error ? error.message : '加载分类失败')
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
  const name = requireText(categoryForm.name, '分类名称')
  if (!name) return
  const status = normalizeStatus(categoryForm.status, '分类')
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
      toast('分类已更新', 'success')
    } else {
      await createCategory(payload)
      toast('分类已创建', 'success')
    }
    closeCategoryDialog()
    await loadCategories()
  } catch (error) {
    toast(error instanceof Error ? error.message : '保存失败')
  }
}

async function deleteCategoryRow(row: CategoryItem): Promise<void> {
  if (!row.id) return
  const ok = await confirm(`确认删除分类 ${row.name}？`)
  if (!ok) return
  try {
    await deleteCategory(row.id)
    toast('分类已删除', 'success')
    await loadCategories()
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

async function updateCategoryRowStatus(row: CategoryItem, status: number): Promise<void> {
  if (!row.id) return
  try {
    await updateCategoryStatus(row.id, status)
    toast('状态已更新', 'success')
    await loadCategories()
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function updateCategoryRowSort(row: CategoryItem): Promise<void> {
  if (!row.id) return
  const sort = Number(row.sortOrder)
  if (!Number.isFinite(sort)) {
    toast('请输入排序值')
    return
  }
  row.sortOrder = sort
  try {
    await updateCategorySort(row.id, row.sortOrder)
    toast('排序已更新', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
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
    toast('请输入有效用户 ID')
    return
  }
  try {
    await sendWelcomeNotification(id)
    toast('欢迎通知已发送', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '发送失败')
  }
}

async function sendStatusChange(): Promise<void> {
  const id = Number(statusChangeUserId.value)
  if (!id) {
    toast('请输入有效用户 ID')
    return
  }
  try {
    await sendStatusChangeNotification(id, {
      newStatus: statusChangeForm.newStatus,
      reason: statusChangeForm.reason || undefined
    })
    toast('状态通知已发送', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '发送失败')
  }
}

async function sendBatch(): Promise<void> {
  const ids = parseIdList(batchUserIds.value)
  if (ids.length === 0) {
    toast('请输入用户 ID 列表')
    return
  }
  try {
    await sendBatchNotification({
      userIds: ids,
      title: batchForm.title,
      content: batchForm.content
    })
    toast('批量通知已发送', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '发送失败')
  }
}

async function sendAnnouncement(): Promise<void> {
  try {
    await sendSystemAnnouncement({
      title: announcementForm.title,
      content: announcementForm.content
    })
    toast('公告已发送', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '发送失败')
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
        name: '注册量',
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
        name: '活跃度',
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
    toast(error instanceof Error ? error.message : '加载统计失败')
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
    toast(error instanceof Error ? error.message : '加载管理员失败')
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
  const username = requireText(adminForm.username, '用户名')
  if (!username) return
  const realName = requireText(adminForm.realName, '姓名')
  if (!realName) return
  const password = adminForm.password?.trim() || ''
  if (!adminEditId.value && !password) {
    toast('请输入管理员密码')
    return
  }
  const status = normalizeStatus(adminForm.status, '管理员')
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
      toast('管理员已更新', 'success')
    } else {
      await createAdmin(payload)
      toast('管理员已创建', 'success')
    }
    closeAdminDialog()
    await loadAdmins()
  } catch (error) {
    toast(error instanceof Error ? error.message : '保存失败')
  }
}

async function confirmDeleteAdmin(row: AdminInfo): Promise<void> {
  const ok = await confirm(`确认删除管理员 ${row.username}？`)
  if (!ok) return
  try {
    await deleteAdmin(row.id)
    toast('管理员已删除', 'success')
    await loadAdmins()
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

async function updateAdminRowStatus(row: AdminInfo, status: number): Promise<void> {
  try {
    await updateAdminStatus(row.id, status)
    toast('状态已更新', 'success')
    await loadAdmins()
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function resetPassword(row: AdminInfo): Promise<void> {
  const ok = await confirm(`确认重置 ${row.username} 密码？`)
  if (!ok) return
  try {
    await resetAdminPassword(row.id)
    toast('重置请求已提交', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '重置失败')
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
        <input v-model="userQuery.username" class="input" placeholder="用户名" />
        <input v-model="userQuery.email" class="input" placeholder="邮箱" />
        <input v-model="userQuery.phone" class="input" placeholder="手机号" />
        <input v-model="userQuery.nickname" class="input" placeholder="昵称" />
        <input v-model="userQuery.status" class="input" placeholder="状态(0/1)" />
        <input v-model="userQuery.roleCode" class="input" placeholder="角色" />
        <button class="btn-primary" @click="loadUsers">查询</button>
      </view>

      <view class="toolbar">
        <button class="btn-outline" @click="toggleAllUsers">{{ allUsersSelected ? '取消全选' : '全选' }}</button>
        <button class="btn-outline" @click="batchUpdateUserStatus(1)">批量启用</button>
        <button class="btn-outline" @click="batchUpdateUserStatus(0)">批量停用</button>
        <button class="btn-outline" @click="batchDeleteUsers">批量删除</button>
      </view>
      <text class="muted">共 {{ userTotal }} 条</text>

      <view v-if="userLoading" class="muted">加载中...</view>
      <view v-else class="list">
        <view v-for="row in userRows" :key="row.id" class="row">
          <view class="row-head">
            <checkbox :checked="userSelection.includes(row.id)" @click="toggleUserSelection(row.id)" />
            <text class="name">{{ row.username }}</text>
            <text class="muted">#{{ row.id }}</text>
          </view>
          <text class="muted">昵称：{{ row.nickname || '--' }}</text>
          <text class="muted">邮箱：{{ row.email || '--' }}</text>
          <text class="muted">角色：{{ row.roles?.join(', ') || '--' }}</text>
          <text class="muted">状态：{{ userStatusText(row.status) }}</text>
          <view class="actions">
            <button class="btn-outline" @click="openUserEdit(row)">编辑</button>
            <button class="btn-outline" @click="confirmDeleteUser(row)">删除</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="userQuery.page <= 1" @click="changeUserPage(-1)">上一页</button>
        <text class="muted">第 {{ userQuery.page }} / {{ userTotalPages }} 页</text>
        <button class="btn-outline" :disabled="userQuery.page >= userTotalPages" @click="changeUserPage(1)">下一页</button>
      </view>

      <view v-if="userDialogVisible" class="dialog">
        <text class="section-title">编辑用户</text>
        <view class="form">
          <input v-model="userForm.username" class="input" placeholder="用户名" />
          <input v-model="userForm.nickname" class="input" placeholder="昵称" />
          <input v-model="userForm.email" class="input" placeholder="邮箱" />
          <input v-model="userForm.phone" class="input" placeholder="手机号" />
          <input v-model="userForm.avatarUrl" class="input" placeholder="头像 URL" />
          <input v-model="userRolesInput" class="input" placeholder="角色(逗号分隔)" />
          <input v-model="userForm.status" class="input" placeholder="状态(0/1)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveUser">保存</button>
          <button class="btn-outline" @click="closeUserEdit">取消</button>
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'merchants'" class="panel glass-card">
      <view class="toolbar">
        <input v-model="merchantQuery.status" class="input" placeholder="状态(可选)" />
        <input v-model="merchantQuery.auditStatus" class="input" placeholder="审核状态(可选)" />
        <button class="btn-primary" @click="loadMerchants">查询</button>
        <button class="btn-outline" @click="openMerchantCreate">新增商家</button>
      </view>
      <view class="toolbar">
        <input v-model="merchantApproveRemark" class="input" placeholder="通过备注(可选)" />
        <input v-model="merchantRejectReason" class="input" placeholder="驳回原因" />
      </view>
      <text class="muted">共 {{ merchantTotal }} 条</text>

      <view v-if="merchantLoading" class="muted">加载中...</view>
      <view v-else class="list">
        <view v-for="row in merchantRows" :key="row.id" class="row">
          <text class="name">{{ row.merchantName || row.username || '商家' }}</text>
          <text class="muted">邮箱：{{ row.email || '--' }}</text>
          <text class="muted">电话：{{ row.phone || '--' }}</text>
          <text class="muted">状态：{{ merchantStatusText(row.status) }}</text>
          <text class="muted">认证：{{ merchantStatusText(row.authStatus) }}</text>
          <view class="actions">
            <button class="btn-outline" @click="openMerchantEdit(row)">编辑</button>
            <button class="btn-outline" @click="approveMerchantRow(row)">通过</button>
            <button class="btn-outline" @click="rejectMerchantRow(row)">驳回</button>
            <button class="btn-outline" @click="confirmDeleteMerchant(row)">删除</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="merchantQuery.page <= 1" @click="changeMerchantPage(-1)">上一页</button>
        <text class="muted">第 {{ merchantQuery.page }} / {{ merchantTotalPages }} 页</text>
        <button class="btn-outline" :disabled="merchantQuery.page >= merchantTotalPages" @click="changeMerchantPage(1)">下一页</button>
      </view>

      <view v-if="merchantDialogVisible" class="dialog">
        <text class="section-title">{{ merchantEditId ? '编辑商家' : '新增商家' }}</text>
        <view class="form">
          <input v-model="merchantForm.username" class="input" placeholder="商家用户名" />
          <input
            v-model="merchantForm.password"
            class="input"
            :placeholder="merchantEditId ? '密码(留空则不修改)' : '密码'"
            password
          />
          <input v-model="merchantForm.merchantName" class="input" placeholder="商家名称" />
          <input v-model="merchantForm.email" class="input" placeholder="邮箱" />
          <input v-model="merchantForm.phone" class="input" placeholder="电话" />
          <input v-model="merchantForm.status" class="input" placeholder="状态(0/1)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveMerchant">保存</button>
          <button class="btn-outline" @click="closeMerchantDialog">取消</button>
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'auth'" class="panel glass-card">
      <view class="toolbar">
        <input v-model="authStatusFilter" class="input" placeholder="状态(0待审/1通过/2驳回)" />
        <input v-model="authRemark" class="input" placeholder="审核备注" />
        <button class="btn-primary" @click="loadMerchantAuth">查询</button>
      </view>

      <view v-if="authLoading" class="muted">加载中...</view>
      <view v-else class="list">
        <view v-for="row in authRows" :key="row.id" class="row">
          <text class="name">商家 ID：{{ row.merchantId }}</text>
          <text class="muted">资质：{{ row.businessLicenseNumber || '--' }}</text>
          <text class="muted">状态：{{ merchantStatusText(row.authStatus) }}</text>
          <view class="actions">
            <button class="btn-outline" @click="reviewMerchantAuthRow(row, 1)">通过</button>
            <button class="btn-outline" @click="reviewMerchantAuthRow(row, 2)">驳回</button>
          </view>
        </view>
      </view>
    </view>
    <view v-if="activeTab === 'categories'" class="panel glass-card">
      <view class="toolbar">
        <button class="btn-outline" @click="openCategoryCreate">新增分类</button>
        <button class="btn-outline" @click="loadCategories">刷新</button>
      </view>
      <text class="muted">共 {{ categoryTotal }} 条</text>

      <view v-if="categoryLoading" class="muted">加载中...</view>
      <view v-else class="list">
        <view v-for="row in categoryRows" :key="row.id" class="row">
          <text class="name">{{ row.name }}</text>
          <text class="muted">描述：{{ row.description || '--' }}</text>
          <text class="muted">状态：{{ row.status === 1 ? '启用' : '停用' }}</text>
          <view class="row-inline">
            <input v-model="row.sortOrder" class="input small" placeholder="排序" />
            <button class="btn-outline" @click="updateCategoryRowSort(row)">保存排序</button>
          </view>
          <view class="actions">
            <button class="btn-outline" @click="openCategoryEdit(row)">编辑</button>
            <button class="btn-outline" @click="updateCategoryRowStatus(row, 1)">启用</button>
            <button class="btn-outline" @click="updateCategoryRowStatus(row, 0)">停用</button>
            <button class="btn-outline" @click="deleteCategoryRow(row)">删除</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="categoryQuery.page <= 1" @click="changeCategoryPage(-1)">上一页</button>
        <text class="muted">第 {{ categoryQuery.page }} / {{ categoryTotalPages }} 页</text>
        <button class="btn-outline" :disabled="categoryQuery.page >= categoryTotalPages" @click="changeCategoryPage(1)">下一页</button>
      </view>

      <view v-if="categoryDialogVisible" class="dialog">
        <text class="section-title">{{ categoryEditId ? '编辑分类' : '新增分类' }}</text>
        <view class="form">
          <input v-model="categoryForm.name" class="input" placeholder="分类名称" />
          <input v-model="categoryForm.description" class="input" placeholder="分类描述" />
          <input v-model="categoryForm.parentId" class="input" placeholder="父级 ID(可选)" />
          <input v-model="categoryForm.status" class="input" placeholder="状态(0/1)" />
          <input v-model="categoryForm.sortOrder" class="input" placeholder="排序(可选)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveCategory">保存</button>
          <button class="btn-outline" @click="closeCategoryDialog">取消</button>
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'notifications'" class="panel glass-card">
      <view class="section">
        <text class="section-title">欢迎通知</text>
        <view class="row-inline">
          <input v-model="welcomeUserId" class="input" placeholder="用户 ID" />
          <button class="btn-primary" @click="sendWelcome">发送</button>
        </view>
      </view>

      <view class="section">
        <text class="section-title">状态变更通知</text>
        <view class="row-inline">
          <input v-model="statusChangeUserId" class="input" placeholder="用户 ID" />
          <input v-model="statusChangeForm.newStatus" class="input" placeholder="新状态(0/1)" />
        </view>
        <textarea v-model="statusChangeForm.reason" class="textarea" placeholder="原因(可选)" />
        <button class="btn-primary" @click="sendStatusChange">发送</button>
      </view>

      <view class="section">
        <text class="section-title">批量通知</text>
        <input v-model="batchUserIds" class="input" placeholder="用户 ID 列表(逗号分隔)" />
        <input v-model="batchForm.title" class="input" placeholder="标题" />
        <textarea v-model="batchForm.content" class="textarea" placeholder="内容" />
        <button class="btn-primary" @click="sendBatch">发送</button>
      </view>

      <view class="section">
        <text class="section-title">系统公告</text>
        <input v-model="announcementForm.title" class="input" placeholder="标题" />
        <textarea v-model="announcementForm.content" class="textarea" placeholder="内容" />
        <button class="btn-primary" @click="sendAnnouncement">发送</button>
      </view>
    </view>

    <view v-if="activeTab === 'statistics'" class="panel glass-card">
      <view class="toolbar">
        <button class="btn-outline" @click="loadStatistics">刷新统计</button>
      </view>
      <view v-if="statsLoading" class="muted">加载中...</view>
      <view v-else class="section">
        <text class="section-title">概览</text>
        <view class="stats-grid">
          <view class="stat-card">
            <text class="stat-label">用户总数</text>
            <text class="stat-value">{{ statsOverview?.totalUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">今日新增</text>
            <text class="stat-value">{{ statsOverview?.todayNewUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">本月新增</text>
            <text class="stat-value">{{ statsOverview?.monthNewUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">活跃用户(7天)</text>
            <text class="stat-value">{{ activeUsers ?? '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">增长率(7天)</text>
            <text class="stat-value">{{ growthRate ?? '--' }}</text>
          </view>
        </view>
      </view>
      <view class="section">
        <text class="section-title">角色分布</text>
        <view v-if="!roleChartData" class="muted">暂无数据</view>
        <ChartView v-else class="chart" type="pie" canvasId="roleChart" :chartData="roleChartData" :opts="chartOpts" />
      </view>
      <view class="section">
        <text class="section-title">状态分布</text>
        <view v-if="!statusChartData" class="muted">暂无数据</view>
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
        <text class="section-title">注册趋势(30天)</text>
        <view v-if="!trendChartData" class="muted">暂无数据</view>
        <ChartView v-else class="chart" type="line" canvasId="trendChart" :chartData="trendChartData" :opts="chartOpts" />
      </view>
      <view class="section">
        <text class="section-title">活跃排行</text>
        <view v-if="!rankingChartData" class="muted">暂无数据</view>
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
        <button class="btn-outline" @click="openAdminCreate">新增管理员</button>
        <button class="btn-outline" @click="loadAdmins">刷新</button>
      </view>
      <text class="muted">共 {{ adminTotal }} 条</text>

      <view v-if="adminLoading" class="muted">加载中...</view>
      <view v-else class="list">
        <view v-for="row in adminRows" :key="row.id" class="row">
          <text class="name">{{ row.username }}</text>
          <text class="muted">姓名：{{ row.realName || '--' }}</text>
          <text class="muted">角色：{{ row.role || '--' }}</text>
          <text class="muted">状态：{{ row.status === 1 ? '启用' : '停用' }}</text>
          <view class="actions">
            <button class="btn-outline" @click="openAdminEdit(row)">编辑</button>
            <button class="btn-outline" @click="updateAdminRowStatus(row, 1)">启用</button>
            <button class="btn-outline" @click="updateAdminRowStatus(row, 0)">停用</button>
            <button class="btn-outline" @click="resetPassword(row)">重置密码</button>
            <button class="btn-outline" @click="confirmDeleteAdmin(row)">删除</button>
          </view>
        </view>
      </view>

      <view class="pagination">
        <button class="btn-outline" :disabled="adminQuery.page <= 1" @click="changeAdminPage(-1)">上一页</button>
        <text class="muted">第 {{ adminQuery.page }} / {{ adminTotalPages }} 页</text>
        <button class="btn-outline" :disabled="adminQuery.page >= adminTotalPages" @click="changeAdminPage(1)">下一页</button>
      </view>

      <view v-if="adminDialogVisible" class="dialog">
        <text class="section-title">{{ adminEditId ? '编辑管理员' : '新增管理员' }}</text>
        <view class="form">
          <input v-model="adminForm.username" class="input" placeholder="用户名" />
          <input
            v-model="adminForm.password"
            class="input"
            :placeholder="adminEditId ? '密码(留空则不修改)' : '密码'"
            password
          />
          <input v-model="adminForm.realName" class="input" placeholder="姓名" />
          <input v-model="adminForm.phone" class="input" placeholder="电话" />
          <input v-model="adminForm.role" class="input" placeholder="角色" />
          <input v-model="adminForm.status" class="input" placeholder="状态(0/1)" />
        </view>
        <view class="actions">
          <button class="btn-primary" @click="saveAdmin">保存</button>
          <button class="btn-outline" @click="closeAdminDialog">取消</button>
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


