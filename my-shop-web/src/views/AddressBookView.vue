<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sessionState } from '../auth/session'
import type { UserAddress } from '../types/domain'
import {
  addUserAddress,
  deleteUserAddress,
  listUserAddresses,
  updateUserAddress
} from '../api/address'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const rows = ref<UserAddress[]>([])

const userId = computed(() => sessionState.user?.id)

const form = reactive<UserAddress>({
  consignee: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  street: '',
  detailAddress: '',
  isDefault: 0
})

function resetForm(): void {
  form.consignee = ''
  form.phone = ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.street = ''
  form.detailAddress = ''
  form.isDefault = 0
}

async function loadAddresses(): Promise<void> {
  if (!userId.value) {
    ElMessage.warning('User session is missing. Please sign in again.')
    return
  }
  loading.value = true
  try {
    rows.value = await listUserAddresses(userId.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load addresses'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function openAdd(): void {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserAddress): void {
  editingId.value = row.id ?? null
  form.consignee = row.consignee
  form.phone = row.phone
  form.province = row.province
  form.city = row.city
  form.district = row.district
  form.street = row.street
  form.detailAddress = row.detailAddress
  form.isDefault = row.isDefault ?? 0
  dialogVisible.value = true
}

async function saveAddress(): Promise<void> {
  if (!userId.value) {
    ElMessage.warning('User session is missing. Please sign in again.')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateUserAddress(editingId.value, form)
      ElMessage.success('Address updated.')
    } else {
      await addUserAddress(userId.value, form)
      ElMessage.success('Address added.')
    }
    dialogVisible.value = false
    await loadAddresses()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to save address'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

async function confirmDelete(row: UserAddress): Promise<void> {
  if (!row.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`Remove address for ${row.consignee}?`, 'Confirm', { type: 'warning' })
    await deleteUserAddress(row.id)
    ElMessage.success('Address removed.')
    await loadAddresses()
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

async function setDefault(row: UserAddress): Promise<void> {
  if (!row.id) {
    return
  }
  try {
    await updateUserAddress(row.id, {
      ...row,
      isDefault: 1
    })
    ElMessage.success('Default address updated.')
    await loadAddresses()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to update default address'
    ElMessage.error(message)
  }
}

onMounted(() => {
  void loadAddresses()
})
</script>

<template>
  <section class="glass-card panel">
    <div class="header">
      <h3>
        <span class="title-wrap">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 4h12v16H6V4Zm2 3h4M8 10h8M8 14h6" /></svg>
          Address Book
        </span>
      </h3>
      <div class="header-actions">
        <el-button round type="primary" @click="openAdd">Add Address</el-button>
        <el-button :loading="loading" round @click="loadAddresses">Refresh</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="Consignee" min-width="140" prop="consignee" />
      <el-table-column label="Phone" min-width="140" prop="phone" />
      <el-table-column label="Address" min-width="320">
        <template #default="scope">
          {{ scope.row.province }} {{ scope.row.city }} {{ scope.row.district }} {{ scope.row.street }}
          {{ scope.row.detailAddress }}
        </template>
      </el-table-column>
      <el-table-column label="Default" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.isDefault === 1" type="success" round>Default</el-tag>
          <el-button v-else text size="small" @click="setDefault(scope.row)">Set Default</el-button>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="180">
        <template #default="scope">
          <el-button round size="small" @click="openEdit(scope.row)">Edit</el-button>
          <el-button round size="small" type="danger" plain @click="confirmDelete(scope.row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" width="520px" align-center>
      <template #header>
        <strong>{{ editingId ? 'Edit Address' : 'Add Address' }}</strong>
      </template>
      <el-form label-position="top" class="form">
        <el-form-item label="Consignee">
          <el-input v-model="form.consignee" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <div class="grid">
          <el-form-item label="Province">
            <el-input v-model="form.province" />
          </el-form-item>
          <el-form-item label="City">
            <el-input v-model="form.city" />
          </el-form-item>
          <el-form-item label="District">
            <el-input v-model="form.district" />
          </el-form-item>
        </div>
        <el-form-item label="Street">
          <el-input v-model="form.street" />
        </el-form-item>
        <el-form-item label="Detail Address">
          <el-input v-model="form.detailAddress" />
        </el-form-item>
        <el-form-item label="Default">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="dialogVisible = false">Cancel</el-button>
        <el-button :loading="saving" round type="primary" @click="saveAddress">Save</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
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
  font-size: clamp(1.02rem, 1.25vw, 1.28rem);
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.title-wrap svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.form {
  margin-top: 4px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

@media (max-width: 900px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
