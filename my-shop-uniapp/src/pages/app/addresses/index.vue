<script setup lang="ts">
import { onMounted, computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '../../../components/AppShell.vue'
import { addUserAddress, deleteUserAddress, listUserAddresses, updateUserAddress } from '../../../api/address'
import type { UserAddress } from '../../../types/domain'
import { sessionState } from '../../../auth/session'
import { confirm, toast } from '../../../utils/ui'

const rows = ref<UserAddress[]>([])
const loading = ref(false)
const saving = ref(false)
const editingAddressId = ref<number | null>(null)
const userId = computed(() => sessionState.user?.id)

const form = reactive<UserAddress>({
  consignee: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  street: '',
  detailAddress: '',
  isDefault: 1
})

function resetForm(): void {
  editingAddressId.value = null
  form.consignee = ''
  form.phone = ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.street = ''
  form.detailAddress = ''
  form.isDefault = rows.value.length === 0 ? 1 : 0
}

function toAddressPayload(address: UserAddress): UserAddress {
  return {
    consignee: address.consignee.trim(),
    phone: address.phone.trim(),
    province: address.province.trim(),
    city: address.city.trim(),
    district: address.district.trim(),
    street: address.street.trim(),
    detailAddress: address.detailAddress.trim(),
    isDefault: address.isDefault === 1 ? 1 : 0
  }
}

function validateForm(): boolean {
  if (!form.consignee.trim()) {
    toast('Consignee is required')
    return false
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    toast('Phone must use a mainland China mobile format')
    return false
  }
  if (!form.province.trim() || !form.city.trim() || !form.district.trim()) {
    toast('Province, city, and district are required')
    return false
  }
  if (!form.street.trim() || !form.detailAddress.trim()) {
    toast('Street and detail address are required')
    return false
  }
  return true
}

async function loadAddresses(): Promise<void> {
  if (loading.value) return
  if (typeof userId.value !== 'number') {
    toast('Missing user session')
    return
  }
  loading.value = true
  try {
    rows.value = await listUserAddresses(userId.value)
    if (editingAddressId.value == null && rows.value.length > 0 && form.consignee.trim().length === 0) {
      form.isDefault = 0
    }
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load addresses')
  } finally {
    loading.value = false
  }
}

async function saveAddress(): Promise<void> {
  if (saving.value) {
    return
  }
  if (typeof userId.value !== 'number') {
    toast('Missing user session')
    return
  }
  if (!validateForm()) {
    return
  }

  saving.value = true
  try {
    const payload = toAddressPayload(form)
    if (editingAddressId.value != null) {
      await updateUserAddress(editingAddressId.value, payload)
      toast('Address updated', 'success')
    } else {
      await addUserAddress(userId.value, payload)
      toast('Address created', 'success')
    }
    resetForm()
    await loadAddresses()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to save the address')
  } finally {
    saving.value = false
  }
}

function startEdit(item: UserAddress): void {
  editingAddressId.value = typeof item.id === 'number' ? item.id : null
  form.consignee = item.consignee
  form.phone = item.phone
  form.province = item.province
  form.city = item.city
  form.district = item.district
  form.street = item.street
  form.detailAddress = item.detailAddress
  form.isDefault = item.isDefault === 1 ? 1 : 0
}

async function markAsDefault(item: UserAddress): Promise<void> {
  if (typeof item.id !== 'number' || item.isDefault === 1) {
    return
  }
  try {
    await updateUserAddress(item.id, {
      ...toAddressPayload(item),
      isDefault: 1
    })
    toast('Default address updated', 'success')
    await loadAddresses()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to update the default address')
  }
}

async function removeAddress(item: UserAddress): Promise<void> {
  if (typeof item.id !== 'number') {
    return
  }
  const ok = await confirm(`Delete the address for ${item.consignee}?`)
  if (!ok) {
    return
  }
  try {
    await deleteUserAddress(item.id)
    toast('Address deleted', 'success')
    if (editingAddressId.value === item.id) {
      resetForm()
    }
    await loadAddresses()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to delete the address')
  }
}

onMounted(() => {
  resetForm()
  void loadAddresses()
})

onShow(() => {
  void loadAddresses()
})
</script>

<template>
  <AppShell title="Addresses">
    <view class="page">
      <view class="panel glass-card">
        <view class="header">
          <text class="section-title">{{ editingAddressId ? 'Edit address' : 'New address' }}</text>
          <button class="btn-outline" @click="resetForm">Reset</button>
        </view>

        <view class="form-grid">
          <input v-model="form.consignee" class="input" placeholder="Consignee" />
          <input v-model="form.phone" class="input" placeholder="Phone" />
          <input v-model="form.province" class="input" placeholder="Province" />
          <input v-model="form.city" class="input" placeholder="City" />
          <input v-model="form.district" class="input" placeholder="District" />
          <input v-model="form.street" class="input" placeholder="Street" />
        </view>

        <textarea v-model="form.detailAddress" class="textarea" placeholder="Detail address" />

        <label class="default-row">
          <switch :checked="form.isDefault === 1" @change="form.isDefault = $event.detail.value ? 1 : 0" />
          <text class="default-text">Set as default address</text>
        </label>

        <button class="btn-primary" :loading="saving" @click="saveAddress">
          {{ editingAddressId ? 'Update address' : 'Create address' }}
        </button>
      </view>

      <view class="panel glass-card">
        <view class="header">
          <text class="section-title">Address book</text>
          <button class="btn-outline" @click="loadAddresses">Refresh</button>
        </view>

        <view v-if="rows.length === 0" class="empty">
          <text class="text-muted">No address is available</text>
        </view>

        <view v-else class="list">
          <view v-for="item in rows" :key="item.id" class="row">
            <view class="row-copy">
              <text class="name">{{ item.consignee }} · {{ item.phone }}</text>
              <text class="meta">
                {{ item.province }} {{ item.city }} {{ item.district }} {{ item.street }} {{ item.detailAddress }}
              </text>
              <text class="meta" v-if="item.isDefault === 1">Default address</text>
            </view>
            <view class="row-actions">
              <button class="btn-outline" @click="startEdit(item)">Edit</button>
              <button v-if="item.isDefault !== 1" class="btn-outline" @click="markAsDefault(item)">Set default</button>
              <button class="btn-outline" @click="removeAddress(item)">Delete</button>
            </view>
          </view>
        </view>
      </view>
    </view>
  </AppShell>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.form-grid {
  display: grid;
  gap: 10px;
}

.input {
  width: 100%;
  background: #fff;
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
}

.textarea {
  width: 100%;
  min-height: 88px;
  background: #fff;
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
}

.default-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.default-text {
  font-size: 13px;
  color: var(--text-muted);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.row-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.name {
  font-size: 14px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  color: var(--text-muted);
}

.empty {
  padding: 16px 0;
  text-align: center;
}
</style>
