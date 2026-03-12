<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
} from '../types/domain'
import {
  deleteUser,
  deleteUsersBatch,
  searchUsers,
  updateUser,
  updateUserStatusBatch
} from '../api/user-management'
import {
  approveMerchant,
  createMerchant,
  deleteMerchant,
  getMerchants,
  rejectMerchant,
  updateMerchant,
  updateMerchantStatus
} from '../api/merchant'
import { listMerchantAuthByStatus, reviewMerchantAuth } from '../api/merchant-auth'
import {
  createCategory,
  deleteCategory,
  getCategories,
  updateCategory,
  updateCategoryStatus
} from '../api/category'
import {
  sendBatchNotification,
  sendStatusChangeNotification,
  sendSystemAnnouncement,
  sendWelcomeNotification
} from '../api/notification'
import {
  getActivityRanking,
  getActiveUsers,
  getGrowthRate,
  getRegistrationTrend,
  getRoleDistribution,
  getStatisticsOverview,
  getStatusDistribution
} from '../api/statistics'
import {
  createAdmin,
  deleteAdmin,
  getAdmins,
  resetAdminPassword,
  updateAdmin,
  updateAdminStatus
} from '../api/admin'

const activeTab = ref('users')

// Users
const userLoading = ref(false)
const userRows = ref<UserSummary[]>([])
const userTotal = ref(0)
const userQuery = reactive({
  page: 1,
  size: 10,
  username: '',
  email: '',
  roleCode: ''
})
const userSelection = ref<UserSummary[]>([])
const userDialogVisible = ref(false)
const userEditId = ref<number | null>(null)
const userForm = reactive<UserUpsertPayload>({
  username: '',
  phone: '',
  nickname: '',
  email: '',
  avatarUrl: '',
  status: 1
})

async function loadUsers(): Promise<void> {
  userLoading.value = true
  try {
    const result = await searchUsers({
      page: userQuery.page,
      size: userQuery.size,
      username: userQuery.username || undefined,
      email: userQuery.email || undefined,
      roleCode: userQuery.roleCode || undefined
    })
    userRows.value = result.records
    userTotal.value = result.total
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load users'
    ElMessage.error(message)
  } finally {
    userLoading.value = false
  }
}

function openUserEdit(row: UserSummary): void {
  userEditId.value = row.id
  userForm.username = row.username
  userForm.phone = row.phone
  userForm.nickname = row.nickname
  userForm.email = row.email
  userForm.avatarUrl = row.avatarUrl
  userForm.status = row.status ?? 1
  userDialogVisible.value = true
}

function openSelectedUserEdit(): void {
  const [first] = userSelection.value
  if (!first) {
    ElMessage.warning('Select a user first.')
    return
  }
  openUserEdit(first)
}

async function saveUser(): Promise<void> {
  if (!userEditId.value) {
    ElMessage.warning('Select a user to update.')
    return
  }
  try {
    await updateUser(userEditId.value, userForm)
    ElMessage.success('User updated.')
    userDialogVisible.value = false
    await loadUsers()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to update user'
    ElMessage.error(message)
  }
}

async function confirmDeleteUser(row: UserSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(`Delete user ${row.username}?`, 'Confirm', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('User deleted.')
    await loadUsers()
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

async function batchUpdateUserStatus(status: number): Promise<void> {
  const ids = userSelection.value.map((item) => item.id)
  if (ids.length === 0) {
    ElMessage.warning('Select users first.')
    return
  }
  try {
    await updateUserStatusBatch(ids, status)
    ElMessage.success('User status updated.')
    await loadUsers()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to update user status'
    ElMessage.error(message)
  }
}

async function batchDeleteUsers(): Promise<void> {
  const ids = userSelection.value.map((item) => item.id)
  if (ids.length === 0) {
    ElMessage.warning('Select users first.')
    return
  }
  try {
    await ElMessageBox.confirm(`Delete ${ids.length} users?`, 'Confirm', { type: 'warning' })
    await deleteUsersBatch(ids)
    ElMessage.success('Users deleted.')
    await loadUsers()
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

// Merchants
const merchantLoading = ref(false)
const merchantRows = ref<MerchantInfo[]>([])
const merchantTotal = ref(0)
const merchantQuery = reactive({
  page: 1,
  size: 10,
  status: undefined as number | undefined
})
const merchantDialogVisible = ref(false)
const merchantEditId = ref<number | null>(null)
const merchantForm = reactive<MerchantUpsertPayload>({
  merchantName: '',
  email: '',
  phone: '',
  status: 1
})

async function loadMerchants(): Promise<void> {
  merchantLoading.value = true
  try {
    const result = await getMerchants({
      page: merchantQuery.page,
      size: merchantQuery.size,
      status: merchantQuery.status
    })
    merchantRows.value = result.records
    merchantTotal.value = result.total
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load merchants'
    ElMessage.error(message)
  } finally {
    merchantLoading.value = false
  }
}

function openMerchantEdit(row: MerchantInfo): void {
  merchantEditId.value = row.id
  merchantForm.merchantName = row.merchantName
  merchantForm.email = row.email
  merchantForm.phone = row.phone
  merchantForm.status = row.status ?? 1
  merchantDialogVisible.value = true
}

function openMerchantCreate(): void {
  merchantEditId.value = null
  merchantForm.merchantName = ''
  merchantForm.email = ''
  merchantForm.phone = ''
  merchantForm.status = 1
  merchantDialogVisible.value = true
}

async function saveMerchant(): Promise<void> {
  try {
    if (merchantEditId.value) {
      await updateMerchant(merchantEditId.value, merchantForm)
      ElMessage.success('Merchant updated.')
    } else {
      await createMerchant(merchantForm)
      ElMessage.success('Merchant created.')
    }
    merchantDialogVisible.value = false
    await loadMerchants()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to save merchant'
    ElMessage.error(message)
  }
}

async function confirmDeleteMerchant(row: MerchantInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(`Delete merchant ${row.merchantName || row.username}?`, 'Confirm', { type: 'warning' })
    await deleteMerchant(row.id)
    ElMessage.success('Merchant deleted.')
    await loadMerchants()
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

async function approveMerchantRow(row: MerchantInfo): Promise<void> {
  let remark = ''
  try {
    const result = await ElMessageBox.prompt('Approval remark (optional)', 'Approve Merchant', {
      confirmButtonText: 'Approve',
      cancelButtonText: 'Cancel'
    })
    remark = result.value || ''
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
  try {
    await approveMerchant(row.id, remark || undefined)
    ElMessage.success('Merchant approved.')
    await loadMerchants()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to approve merchant'
    ElMessage.error(message)
  }
}

async function rejectMerchantRow(row: MerchantInfo): Promise<void> {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('Reject reason', 'Reject Merchant', {
      confirmButtonText: 'Reject',
      cancelButtonText: 'Cancel'
    })
    reason = result.value || ''
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
  if (!reason) {
    return
  }
  try {
    await rejectMerchant(row.id, reason)
    ElMessage.success('Merchant rejected.')
    await loadMerchants()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to reject merchant'
    ElMessage.error(message)
  }
}

async function updateMerchantRowStatus(row: MerchantInfo, status: number): Promise<void> {
  try {
    await updateMerchantStatus(row.id, status)
    ElMessage.success('Merchant status updated.')
    await loadMerchants()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to update merchant status'
    ElMessage.error(message)
  }
}

// Merchant Auth
const authLoading = ref(false)
const authRows = ref<MerchantAuthInfo[]>([])
const authStatusFilter = ref(0)

async function loadMerchantAuth(): Promise<void> {
  authLoading.value = true
  try {
    authRows.value = await listMerchantAuthByStatus(authStatusFilter.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load merchant auth'
    ElMessage.error(message)
  } finally {
    authLoading.value = false
  }
}

async function reviewMerchantAuthRow(row: MerchantAuthInfo, status: number): Promise<void> {
  const title = status === 1 ? 'Approve Auth' : 'Reject Auth'
  let remark = ''
  try {
    const result = await ElMessageBox.prompt('Remark (optional)', title, {
      confirmButtonText: status === 1 ? 'Approve' : 'Reject',
      cancelButtonText: 'Cancel'
    })
    remark = result.value || ''
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
  try {
    await reviewMerchantAuth(row.merchantId!, status, remark || undefined)
    ElMessage.success('Auth review submitted.')
    await loadMerchantAuth()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to review auth'
    ElMessage.error(message)
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
const categoryDialogVisible = ref(false)
const categoryEditId = ref<number | null>(null)
const categoryForm = reactive<CategoryItem>({
  name: '',
  description: '',
  parentId: undefined,
  status: 1
})

async function loadCategories(): Promise<void> {
  categoryLoading.value = true
  try {
    const result = await getCategories({ page: categoryQuery.page, size: categoryQuery.size })
    categoryRows.value = result.records
    categoryTotal.value = result.total
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load categories'
    ElMessage.error(message)
  } finally {
    categoryLoading.value = false
  }
}

function openCategoryEdit(row: CategoryItem): void {
  categoryEditId.value = row.id ?? null
  categoryForm.name = row.name
  categoryForm.description = row.description
  categoryForm.parentId = row.parentId
  categoryForm.status = row.status ?? 1
  categoryDialogVisible.value = true
}

function openCategoryCreate(): void {
  categoryEditId.value = null
  categoryForm.name = ''
  categoryForm.description = ''
  categoryForm.parentId = undefined
  categoryForm.status = 1
  categoryDialogVisible.value = true
}

async function saveCategory(): Promise<void> {
  try {
    if (categoryEditId.value) {
      await updateCategory(categoryEditId.value, categoryForm)
      ElMessage.success('Category updated.')
    } else {
      await createCategory(categoryForm)
      ElMessage.success('Category created.')
    }
    categoryDialogVisible.value = false
    await loadCategories()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to save category'
    ElMessage.error(message)
  }
}

async function deleteCategoryRow(row: CategoryItem): Promise<void> {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`Delete category ${row.name}?`, 'Confirm', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('Category deleted.')
    await loadCategories()
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

async function updateCategoryRowStatus(row: CategoryItem, status: number): Promise<void> {
  if (!row.id) return
  try {
    await updateCategoryStatus(row.id, status)
    ElMessage.success('Category status updated.')
    await loadCategories()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to update category status'
    ElMessage.error(message)
  }
}

// Notifications
const welcomeUserId = ref('')
const statusChangeUserId = ref('')
const statusChangeForm = reactive({ newStatus: 1, reason: '' })
const batchUserIds = ref('')
const batchForm = reactive({ title: '', content: '' })
const announcementForm = reactive({ title: '', content: '' })

async function sendWelcome(): Promise<void> {
  const id = Number(welcomeUserId.value)
  if (!id) {
    ElMessage.warning('Provide a valid user id.')
    return
  }
  try {
    await sendWelcomeNotification(id)
    ElMessage.success('Welcome notification sent.')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to send welcome notification'
    ElMessage.error(message)
  }
}

async function sendStatusChange(): Promise<void> {
  const id = Number(statusChangeUserId.value)
  if (!id) {
    ElMessage.warning('Provide a valid user id.')
    return
  }
  try {
    await sendStatusChangeNotification(id, {
      newStatus: statusChangeForm.newStatus,
      reason: statusChangeForm.reason || undefined
    })
    ElMessage.success('Status change notification sent.')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to send status change'
    ElMessage.error(message)
  }
}

async function sendBatch(): Promise<void> {
  const ids = batchUserIds.value
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value) && value > 0)
  if (ids.length === 0) {
    ElMessage.warning('Provide user ids separated by comma.')
    return
  }
  try {
    await sendBatchNotification({
      userIds: ids,
      title: batchForm.title,
      content: batchForm.content
    })
    ElMessage.success('Batch notification sent.')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to send batch notification'
    ElMessage.error(message)
  }
}

async function sendAnnouncement(): Promise<void> {
  try {
    await sendSystemAnnouncement({
      title: announcementForm.title,
      content: announcementForm.content
    })
    ElMessage.success('System announcement sent.')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to send announcement'
    ElMessage.error(message)
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

const trendList = computed(() => Object.entries(registrationTrend.value))
const rankingList = computed(() => Object.entries(activityRanking.value))

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
    const message = error instanceof Error ? error.message : 'Failed to load statistics'
    ElMessage.error(message)
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
const adminDialogVisible = ref(false)
const adminEditId = ref<number | null>(null)
const adminForm = reactive<AdminUpsertPayload>({
  username: '',
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
    const message = error instanceof Error ? error.message : 'Failed to load admins'
    ElMessage.error(message)
  } finally {
    adminLoading.value = false
  }
}

function openAdminEdit(row: AdminInfo): void {
  adminEditId.value = row.id
  adminForm.username = row.username
  adminForm.phone = row.phone
  adminForm.realName = row.realName
  adminForm.role = row.role
  adminForm.status = row.status ?? 1
  adminDialogVisible.value = true
}

function openAdminCreate(): void {
  adminEditId.value = null
  adminForm.username = ''
  adminForm.phone = ''
  adminForm.realName = ''
  adminForm.role = ''
  adminForm.status = 1
  adminDialogVisible.value = true
}

async function saveAdmin(): Promise<void> {
  try {
    if (adminEditId.value) {
      await updateAdmin(adminEditId.value, adminForm)
      ElMessage.success('Admin updated.')
    } else {
      await createAdmin(adminForm)
      ElMessage.success('Admin created.')
    }
    adminDialogVisible.value = false
    await loadAdmins()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to save admin'
    ElMessage.error(message)
  }
}

async function confirmDeleteAdmin(row: AdminInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(`Delete admin ${row.username}?`, 'Confirm', { type: 'warning' })
    await deleteAdmin(row.id)
    ElMessage.success('Admin deleted.')
    await loadAdmins()
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

async function updateAdminRowStatus(row: AdminInfo, status: number): Promise<void> {
  try {
    await updateAdminStatus(row.id, status)
    ElMessage.success('Admin status updated.')
    await loadAdmins()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to update admin status'
    ElMessage.error(message)
  }
}

async function resetPassword(row: AdminInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(`Reset password for ${row.username}?`, 'Confirm', { type: 'warning' })
    await resetAdminPassword(row.id)
    ElMessage.success('Password reset triggered.')
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

watch(
  () => activeTab.value,
  (tab) => {
    if (tab === 'users' && userRows.value.length === 0) {
      void loadUsers()
    }
    if (tab === 'merchants' && merchantRows.value.length === 0) {
      void loadMerchants()
    }
    if (tab === 'auth' && authRows.value.length === 0) {
      void loadMerchantAuth()
    }
    if (tab === 'categories' && categoryRows.value.length === 0) {
      void loadCategories()
    }
    if (tab === 'statistics' && !statsOverview.value) {
      void loadStatistics()
    }
    if (tab === 'admins' && adminRows.value.length === 0) {
      void loadAdmins()
    }
  },
  { immediate: true }
)

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <section class="glass-card panel">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="Users" name="users">
        <div class="toolbar">
          <el-input v-model="userQuery.username" placeholder="Username" />
          <el-input v-model="userQuery.email" placeholder="Email" />
          <el-input v-model="userQuery.roleCode" placeholder="Role" />
          <el-button round type="primary" @click="loadUsers">Search</el-button>
          <el-button round @click="openSelectedUserEdit">Edit Selected</el-button>
          <el-button round @click="batchUpdateUserStatus(1)">Enable</el-button>
          <el-button round @click="batchUpdateUserStatus(0)">Disable</el-button>
          <el-button round type="danger" plain @click="batchDeleteUsers">Delete</el-button>
        </div>
        <el-table v-loading="userLoading" :data="userRows" stripe @selection-change="(rows) => (userSelection = rows)">
          <el-table-column type="selection" width="48" />
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="Username" prop="username" min-width="160" />
          <el-table-column label="Nickname" prop="nickname" min-width="140" />
          <el-table-column label="Email" prop="email" min-width="180" />
          <el-table-column label="Status" min-width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" round>
                {{ scope.row.status === 1 ? 'Active' : 'Disabled' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="180">
            <template #default="scope">
              <el-button round size="small" @click="openUserEdit(scope.row)">Edit</el-button>
              <el-button round size="small" type="danger" plain @click="confirmDeleteUser(scope.row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="userQuery.page"
            v-model:page-size="userQuery.size"
            :page-sizes="[10, 20, 50]"
            :total="userTotal"
            background
            layout="total, sizes, prev, pager, next"
            @current-change="loadUsers"
            @size-change="loadUsers"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="Merchants" name="merchants">
        <div class="toolbar">
          <el-select v-model="merchantQuery.status" clearable placeholder="Status">
            <el-option :value="0" label="Pending" />
            <el-option :value="1" label="Approved" />
            <el-option :value="2" label="Rejected" />
          </el-select>
          <el-button round type="primary" @click="loadMerchants">Search</el-button>
          <el-button round @click="openMerchantCreate">New Merchant</el-button>
        </div>
        <el-table v-loading="merchantLoading" :data="merchantRows" stripe>
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="Name" prop="merchantName" min-width="160" />
          <el-table-column label="Email" prop="email" min-width="180" />
          <el-table-column label="Status" min-width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : scope.row.status === 2 ? 'danger' : 'warning'" round>
                {{ scope.row.status === 1 ? 'Approved' : scope.row.status === 2 ? 'Rejected' : 'Pending' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="280">
            <template #default="scope">
              <el-button round size="small" @click="openMerchantEdit(scope.row)">Edit</el-button>
              <el-button round size="small" type="success" plain @click="approveMerchantRow(scope.row)">Approve</el-button>
              <el-button round size="small" type="warning" plain @click="rejectMerchantRow(scope.row)">Reject</el-button>
              <el-button round size="small" type="danger" plain @click="confirmDeleteMerchant(scope.row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="merchantQuery.page"
            v-model:page-size="merchantQuery.size"
            :page-sizes="[10, 20, 50]"
            :total="merchantTotal"
            background
            layout="total, sizes, prev, pager, next"
            @current-change="loadMerchants"
            @size-change="loadMerchants"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="Merchant Auth" name="auth">
        <div class="toolbar">
          <el-select v-model="authStatusFilter" placeholder="Auth Status" @change="loadMerchantAuth">
            <el-option :value="0" label="Pending" />
            <el-option :value="1" label="Approved" />
            <el-option :value="2" label="Rejected" />
          </el-select>
          <el-button round @click="loadMerchantAuth">Refresh</el-button>
        </div>
        <el-table v-loading="authLoading" :data="authRows" stripe>
          <el-table-column label="Merchant ID" prop="merchantId" width="120" />
          <el-table-column label="License" prop="businessLicenseNumber" min-width="160" />
          <el-table-column label="Contact" prop="contactPhone" min-width="140" />
          <el-table-column label="Status" min-width="120">
            <template #default="scope">
              <el-tag :type="scope.row.authStatus === 1 ? 'success' : scope.row.authStatus === 2 ? 'danger' : 'warning'" round>
                {{ scope.row.authStatus === 1 ? 'Approved' : scope.row.authStatus === 2 ? 'Rejected' : 'Pending' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="220">
            <template #default="scope">
              <el-button round size="small" type="success" plain @click="reviewMerchantAuthRow(scope.row, 1)">Approve</el-button>
              <el-button round size="small" type="danger" plain @click="reviewMerchantAuthRow(scope.row, 2)">Reject</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="Categories" name="categories">
        <div class="toolbar">
          <el-button round type="primary" @click="loadCategories">Refresh</el-button>
          <el-button round @click="openCategoryCreate">New Category</el-button>
        </div>
        <el-table v-loading="categoryLoading" :data="categoryRows" stripe>
          <el-table-column label="ID" prop="id" width="90" />
          <el-table-column label="Name" prop="name" min-width="160" />
          <el-table-column label="Parent" prop="parentId" width="120" />
          <el-table-column label="Status" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" round>
                {{ scope.row.status === 1 ? 'Enabled' : 'Disabled' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="200">
            <template #default="scope">
              <el-button round size="small" @click="openCategoryEdit(scope.row)">Edit</el-button>
              <el-button round size="small" type="success" plain @click="updateCategoryRowStatus(scope.row, 1)">Enable</el-button>
              <el-button round size="small" type="warning" plain @click="updateCategoryRowStatus(scope.row, 0)">Disable</el-button>
              <el-button round size="small" type="danger" plain @click="deleteCategoryRow(scope.row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="categoryQuery.page"
            v-model:page-size="categoryQuery.size"
            :page-sizes="[10, 20, 50]"
            :total="categoryTotal"
            background
            layout="total, sizes, prev, pager, next"
            @current-change="loadCategories"
            @size-change="loadCategories"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="Notifications" name="notifications">
        <div class="notification-grid">
          <section class="card-block">
            <h4>Welcome Notification</h4>
            <el-input v-model="welcomeUserId" placeholder="User ID" />
            <el-button round type="primary" @click="sendWelcome">Send</el-button>
          </section>
          <section class="card-block">
            <h4>Status Change Notification</h4>
            <el-input v-model="statusChangeUserId" placeholder="User ID" />
            <el-input-number v-model="statusChangeForm.newStatus" :min="0" :max="1" />
            <el-input v-model="statusChangeForm.reason" placeholder="Reason" />
            <el-button round type="primary" @click="sendStatusChange">Send</el-button>
          </section>
          <section class="card-block">
            <h4>Batch Notification</h4>
            <el-input v-model="batchUserIds" placeholder="User IDs, comma separated" />
            <el-input v-model="batchForm.title" placeholder="Title" />
            <el-input v-model="batchForm.content" placeholder="Content" type="textarea" rows="3" />
            <el-button round type="primary" @click="sendBatch">Send</el-button>
          </section>
          <section class="card-block">
            <h4>System Announcement</h4>
            <el-input v-model="announcementForm.title" placeholder="Title" />
            <el-input v-model="announcementForm.content" placeholder="Content" type="textarea" rows="3" />
            <el-button round type="primary" @click="sendAnnouncement">Send</el-button>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Statistics" name="statistics">
        <div class="toolbar">
          <el-button round type="primary" @click="loadStatistics">Refresh</el-button>
        </div>
        <div v-loading="statsLoading" class="stats-grid">
          <article class="stat-card">
            <p>Total Users</p>
            <strong>{{ statsOverview?.totalUsers ?? '--' }}</strong>
          </article>
          <article class="stat-card">
            <p>New Today</p>
            <strong>{{ statsOverview?.todayNewUsers ?? '--' }}</strong>
          </article>
          <article class="stat-card">
            <p>New This Month</p>
            <strong>{{ statsOverview?.monthNewUsers ?? '--' }}</strong>
          </article>
          <article class="stat-card">
            <p>Active Users (7d)</p>
            <strong>{{ activeUsers ?? '--' }}</strong>
          </article>
          <article class="stat-card">
            <p>Growth Rate</p>
            <strong>{{ growthRate != null ? `${growthRate.toFixed(2)}%` : '--' }}</strong>
          </article>
        </div>
        <div class="stats-split">
          <section class="card-block">
            <h4>Role Distribution</h4>
            <ul>
              <li v-for="(value, key) in roleDistribution" :key="key">{{ key }}: {{ value }}</li>
            </ul>
          </section>
          <section class="card-block">
            <h4>Status Distribution</h4>
            <ul>
              <li v-for="(value, key) in statusDistribution" :key="key">{{ key }}: {{ value }}</li>
            </ul>
          </section>
          <section class="card-block">
            <h4>Registration Trend (30d)</h4>
            <ul>
              <li v-for="item in trendList" :key="item[0]">{{ item[0] }}: {{ item[1] }}</li>
            </ul>
          </section>
          <section class="card-block">
            <h4>Activity Ranking</h4>
            <ul>
              <li v-for="item in rankingList" :key="item[0]">User {{ item[0] }}: {{ item[1] }}</li>
            </ul>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Admins" name="admins">
        <div class="toolbar">
          <el-button round type="primary" @click="loadAdmins">Refresh</el-button>
          <el-button round @click="openAdminCreate">New Admin</el-button>
        </div>
        <el-table v-loading="adminLoading" :data="adminRows" stripe>
          <el-table-column label="ID" prop="id" width="90" />
          <el-table-column label="Username" prop="username" min-width="160" />
          <el-table-column label="Role" prop="role" min-width="120" />
          <el-table-column label="Status" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" round>
                {{ scope.row.status === 1 ? 'Active' : 'Disabled' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="260">
            <template #default="scope">
              <el-button round size="small" @click="openAdminEdit(scope.row)">Edit</el-button>
              <el-button round size="small" type="success" plain @click="updateAdminRowStatus(scope.row, 1)">Enable</el-button>
              <el-button round size="small" type="warning" plain @click="updateAdminRowStatus(scope.row, 0)">Disable</el-button>
              <el-button round size="small" @click="resetPassword(scope.row)">Reset PW</el-button>
              <el-button round size="small" type="danger" plain @click="confirmDeleteAdmin(scope.row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="adminQuery.page"
            v-model:page-size="adminQuery.size"
            :page-sizes="[10, 20, 50]"
            :total="adminTotal"
            background
            layout="total, sizes, prev, pager, next"
            @current-change="loadAdmins"
            @size-change="loadAdmins"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userDialogVisible" width="520px" align-center>
      <template #header>
        <strong>User Profile</strong>
      </template>
      <el-form label-position="top" class="form">
        <el-form-item label="Username">
          <el-input v-model="userForm.username" disabled />
        </el-form-item>
        <el-form-item label="Nickname">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="Avatar URL">
          <el-input v-model="userForm.avatarUrl" />
        </el-form-item>
        <el-form-item label="Status">
          <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="userDialogVisible = false">Cancel</el-button>
        <el-button round type="primary" @click="saveUser">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="merchantDialogVisible" width="520px" align-center>
      <template #header>
        <strong>Merchant</strong>
      </template>
      <el-form label-position="top" class="form">
        <el-form-item label="Merchant Name">
          <el-input v-model="merchantForm.merchantName" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="merchantForm.email" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="merchantForm.phone" />
        </el-form-item>
        <el-form-item label="Status">
          <el-switch v-model="merchantForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="merchantDialogVisible = false">Cancel</el-button>
        <el-button round type="primary" @click="saveMerchant">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" width="520px" align-center>
      <template #header>
        <strong>Category</strong>
      </template>
      <el-form label-position="top" class="form">
        <el-form-item label="Name">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="categoryForm.description" />
        </el-form-item>
        <el-form-item label="Parent ID">
          <el-input-number v-model="categoryForm.parentId" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="Status">
          <el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="categoryDialogVisible = false">Cancel</el-button>
        <el-button round type="primary" @click="saveCategory">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminDialogVisible" width="520px" align-center>
      <template #header>
        <strong>Admin</strong>
      </template>
      <el-form label-position="top" class="form">
        <el-form-item label="Username">
          <el-input v-model="adminForm.username" :disabled="Boolean(adminEditId)" />
        </el-form-item>
        <el-form-item label="Real Name">
          <el-input v-model="adminForm.realName" />
        </el-form-item>
        <el-form-item label="Role">
          <el-input v-model="adminForm.role" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="adminForm.phone" />
        </el-form-item>
        <el-form-item label="Status">
          <el-switch v-model="adminForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="adminDialogVisible = false">Cancel</el-button>
        <el-button round type="primary" @click="saveAdmin">Save</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.form {
  margin-top: 6px;
}

.notification-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.card-block {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  gap: 8px;
}

.card-block h4 {
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.stat-card {
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.85);
}

.stat-card p {
  margin: 0;
  color: var(--text-muted);
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 1.3rem;
}

.stats-split {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}
</style>
