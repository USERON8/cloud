<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sessionState } from '../auth/session'
import type { MerchantAuthInfo, MerchantAuthPayload, MerchantInfo, MerchantUpsertPayload } from '../types/domain'
import {
  getMerchantById,
  getMerchantStatistics,
  updateMerchant
} from '../api/merchant'
import {
  applyMerchantAuth,
  getMerchantAuth,
  revokeMerchantAuth
} from '../api/merchant-auth'

const merchantInfo = ref<MerchantInfo | null>(null)
const authInfo = ref<MerchantAuthInfo | null>(null)
const stats = ref<Record<string, unknown> | null>(null)
const loading = ref(false)
const saving = ref(false)
const authSaving = ref(false)

const merchantId = computed(() => sessionState.user?.id)

const profileForm = reactive<MerchantUpsertPayload>({
  merchantName: '',
  email: '',
  phone: ''
})

const authForm = reactive<MerchantAuthPayload>({
  businessLicenseNumber: '',
  businessLicenseUrl: '',
  idCardFrontUrl: '',
  idCardBackUrl: '',
  contactPhone: '',
  contactAddress: ''
})

const statsEntries = computed(() =>
  Object.entries(stats.value || {}).map(([key, value]) => ({ key, value }))
)

function statusText(status?: number): string {
  if (status === 1) return 'Approved'
  if (status === 2) return 'Rejected'
  if (status === 0) return 'Pending'
  return 'Unknown'
}

async function loadMerchant(): Promise<void> {
  if (!merchantId.value) {
    ElMessage.warning('Merchant session is missing. Please sign in again.')
    return
  }
  loading.value = true
  try {
    const [info, auth, statResult] = await Promise.all([
      getMerchantById(merchantId.value),
      getMerchantAuth(merchantId.value),
      getMerchantStatistics(merchantId.value)
    ])
    merchantInfo.value = info
    authInfo.value = auth
    stats.value = statResult

    profileForm.merchantName = info.merchantName || ''
    profileForm.email = info.email || ''
    profileForm.phone = info.phone || ''

    if (auth) {
      authForm.businessLicenseNumber = auth.businessLicenseNumber || ''
      authForm.businessLicenseUrl = auth.businessLicenseUrl || ''
      authForm.idCardFrontUrl = auth.idCardFrontUrl || ''
      authForm.idCardBackUrl = auth.idCardBackUrl || ''
      authForm.contactPhone = auth.contactPhone || ''
      authForm.contactAddress = auth.contactAddress || ''
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load merchant data'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function saveProfile(): Promise<void> {
  if (!merchantId.value) {
    return
  }
  saving.value = true
  try {
    await updateMerchant(merchantId.value, profileForm)
    ElMessage.success('Merchant profile updated.')
    await loadMerchant()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to update profile'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

async function submitAuth(): Promise<void> {
  if (!merchantId.value) {
    return
  }
  authSaving.value = true
  try {
    await applyMerchantAuth(merchantId.value, authForm)
    ElMessage.success('Merchant verification submitted.')
    await loadMerchant()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to submit verification'
    ElMessage.error(message)
  } finally {
    authSaving.value = false
  }
}

async function revokeAuth(): Promise<void> {
  if (!merchantId.value) {
    return
  }
  try {
    await ElMessageBox.confirm('Revoke current merchant verification?', 'Confirm', { type: 'warning' })
    await revokeMerchantAuth(merchantId.value)
    ElMessage.success('Verification revoked.')
    await loadMerchant()
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
  }
}

onMounted(() => {
  void loadMerchant()
})
</script>

<template>
  <div class="merchant-grid">
    <section class="glass-card card" v-loading="loading">
      <p class="label">Merchant Snapshot</p>
      <h3>{{ merchantInfo?.merchantName || merchantInfo?.username || 'Merchant' }}</h3>
      <p class="muted">Merchant ID: {{ merchantInfo?.id ?? '-' }}</p>
      <p class="muted">Status: {{ statusText(merchantInfo?.status) }}</p>
      <p class="muted">Auth Status: {{ statusText(merchantInfo?.authStatus) }}</p>
      <el-button round @click="loadMerchant">Refresh</el-button>
    </section>

    <section class="glass-card card wide">
      <p class="label">Merchant Profile</p>
      <el-form label-position="top" class="form">
        <el-form-item label="Merchant Name">
          <el-input v-model="profileForm.merchantName" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="profileForm.email" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="profileForm.phone" />
        </el-form-item>
      </el-form>
      <el-button :loading="saving" round type="primary" @click="saveProfile">Save Profile</el-button>
    </section>

    <section class="glass-card card wide">
      <div class="section-head">
        <p class="label">Merchant Verification</p>
        <el-tag v-if="authInfo" :type="authInfo.authStatus === 1 ? 'success' : authInfo.authStatus === 2 ? 'danger' : 'warning'" round>
          {{ statusText(authInfo.authStatus) }}
        </el-tag>
      </div>
      <el-form label-position="top" class="form">
        <el-form-item label="Business License Number">
          <el-input v-model="authForm.businessLicenseNumber" />
        </el-form-item>
        <el-form-item label="Business License URL">
          <el-input v-model="authForm.businessLicenseUrl" />
        </el-form-item>
        <el-form-item label="ID Card Front URL">
          <el-input v-model="authForm.idCardFrontUrl" />
        </el-form-item>
        <el-form-item label="ID Card Back URL">
          <el-input v-model="authForm.idCardBackUrl" />
        </el-form-item>
        <el-form-item label="Contact Phone">
          <el-input v-model="authForm.contactPhone" />
        </el-form-item>
        <el-form-item label="Contact Address">
          <el-input v-model="authForm.contactAddress" />
        </el-form-item>
      </el-form>
      <div class="actions">
        <el-button :loading="authSaving" round type="primary" @click="submitAuth">Submit Verification</el-button>
        <el-button v-if="authInfo" round type="danger" plain @click="revokeAuth">Revoke</el-button>
      </div>
    </section>

    <section class="glass-card card">
      <p class="label">Merchant Stats</p>
      <div v-if="statsEntries.length === 0" class="muted">No statistics available.</div>
      <ul v-else class="stats-list">
        <li v-for="entry in statsEntries" :key="entry.key">
          <span class="stat-key">{{ entry.key }}</span>
          <span class="stat-value">{{ entry.value ?? '-' }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.merchant-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.card {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.wide {
  grid-column: span 2;
}

.label {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-muted);
}

.muted {
  color: var(--text-muted);
}

.form {
  margin-top: 10px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stats-list {
  list-style: none;
  padding: 0;
  margin: 8px 0 0;
  display: grid;
  gap: 6px;
}

.stats-list li {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.stat-key {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.stat-value {
  font-weight: 600;
}

@media (max-width: 900px) {
  .merchant-grid {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }
}
</style>
