<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { sessionState } from '../../../auth/session'
import type { MerchantAuthInfo, MerchantAuthPayload, MerchantInfo, MerchantUpsertPayload } from '../../../types/domain'
import { getMerchantById, getMerchantStatistics, updateMerchant } from '../../../api/merchant'
import { applyMerchantAuth, getMerchantAuth, revokeMerchantAuth, uploadMerchantBusinessLicense } from '../../../api/merchant-auth'
import { confirm, toast } from '../../../utils/ui'

const merchantInfo = ref<MerchantInfo | null>(null)
const authInfo = ref<MerchantAuthInfo | null>(null)
const stats = ref<Record<string, unknown> | null>(null)
const loading = ref(false)
const saving = ref(false)
const authSaving = ref(false)
const uploadState = reactive({
  businessLicenseUrl: false,
  idCardFrontUrl: false,
  idCardBackUrl: false
})

const merchantId = computed(() => sessionState.user?.id)
const uploadUrl =
  import.meta.env.VITE_MERCHANT_AUTH_UPLOAD_URL || import.meta.env.VITE_UPLOAD_URL || ''

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

type UploadField = 'businessLicenseUrl' | 'idCardFrontUrl' | 'idCardBackUrl'

function statusText(status?: number): string {
  if (status === 1) return 'Approved'
  if (status === 2) return 'Rejected'
  if (status === 0) return 'Pending review'
  return 'Unknown'
}

function resolveUploadUrl(): string | null {
  if (!uploadUrl) {
    toast('Upload endpoint is not configured. Set VITE_MERCHANT_AUTH_UPLOAD_URL or VITE_UPLOAD_URL')
    return null
  }
  return uploadUrl
}

function extractUploadUrl(raw: string): string | null {
  if (!raw) return null
  try {
    const payload = JSON.parse(raw) as unknown
    if (typeof payload === 'string') return payload
    if (payload && typeof payload === 'object') {
      const record = payload as Record<string, unknown>
      if (typeof record.url === 'string') return record.url
      if (typeof record.data === 'string') return record.data
      if (record.data && typeof (record.data as Record<string, unknown>).url === 'string') {
        return (record.data as Record<string, unknown>).url as string
      }
    }
  } catch {
    return null
  }
  return null
}

async function uploadAuthImage(field: UploadField): Promise<void> {
  uploadState[field] = true
  try {
    const chooseResult = await uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera']
    })
    const filePath = chooseResult.tempFilePaths?.[0]
    if (!filePath) {
      toast('No image selected')
      return
    }

    if (field === 'businessLicenseUrl') {
      if (!merchantId.value) {
        toast('Merchant session is missing. Please sign in again.')
        return
      }
      const result = await uploadMerchantBusinessLicense(merchantId.value, filePath)
      authForm.businessLicenseUrl = result.previewUrl
      toast('Upload completed', 'success')
      return
    }

    const target = resolveUploadUrl()
    if (!target) return
    const uploadResult = await new Promise<UniApp.UploadFileSuccessCallbackResult>((resolve, reject) => {
      uni.uploadFile({
        url: target,
        filePath,
        name: 'file',
        success: resolve,
        fail: reject
      })
    })
    if (uploadResult.statusCode && uploadResult.statusCode >= 400) {
      toast('Upload failed')
      return
    }
    const url = extractUploadUrl(uploadResult.data)
    if (!url) {
      toast('The upload response did not contain a file URL')
      return
    }
    authForm[field] = url
    toast('Upload completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Upload failed')
  } finally {
    uploadState[field] = false
  }
}

function previewImage(url?: string): void {
  if (!url) {
    toast('There is no image to preview')
    return
  }
  uni.previewImage({
    urls: [url]
  })
}

async function loadMerchant(): Promise<void> {
  if (!merchantId.value) {
    toast('Merchant session is missing. Please sign in again.')
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
    toast(error instanceof Error ? error.message : 'Failed to load merchant data')
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
    toast('Profile updated', 'success')
    await loadMerchant()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Update failed')
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
    toast('Merchant auth submitted', 'success')
    await loadMerchant()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Submit failed')
  } finally {
    authSaving.value = false
  }
}

async function revokeAuth(): Promise<void> {
  if (!merchantId.value) {
    return
  }
  const ok = await confirm('Revoke merchant authentication?')
  if (!ok) {
    return
  }
  try {
    await revokeMerchantAuth(merchantId.value)
    toast('Merchant auth revoked', 'success')
    await loadMerchant()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Revoke failed')
  }
}

onMounted(() => {
  void loadMerchant()
})
</script>

<template>
  <AppShell title="Merchant Center">
    <view class="grid">
      <view class="card glass-card">
        <text class="label">Merchant overview</text>
        <text class="title">{{ merchantInfo?.merchantName || merchantInfo?.username || 'Merchant' }}</text>
        <text class="muted">Merchant ID: {{ merchantInfo?.id ?? '-' }}</text>
        <text class="muted">Status: {{ statusText(merchantInfo?.status) }}</text>
        <text class="muted">Auth status: {{ statusText(merchantInfo?.authStatus) }}</text>
        <button class="btn-outline" :loading="loading" @click="loadMerchant">Refresh</button>
      </view>

      <view class="card glass-card wide">
        <text class="label">Merchant profile</text>
        <view class="form">
          <view class="field">
            <text class="field-label">Merchant name</text>
            <input v-model="profileForm.merchantName" class="input" placeholder="Enter the merchant name" />
          </view>
          <view class="field">
            <text class="field-label">Email</text>
            <input v-model="profileForm.email" class="input" placeholder="Enter the email address" />
          </view>
          <view class="field">
            <text class="field-label">Phone</text>
            <input v-model="profileForm.phone" class="input" placeholder="Enter the phone number" />
          </view>
        </view>
        <button class="btn-primary" :loading="saving" @click="saveProfile">Save profile</button>
      </view>

      <view class="card glass-card wide">
        <view class="section-head">
          <text class="label">Merchant authentication</text>
          <text v-if="authInfo" class="status-chip">{{ statusText(authInfo.authStatus) }}</text>
        </view>
        <view class="form">
          <view class="field">
            <text class="field-label">Business license number</text>
            <input v-model="authForm.businessLicenseNumber" class="input" placeholder="Enter the business license number" />
          </view>
          <view class="field">
            <text class="field-label">Business license URL</text>
            <view class="row-inline">
              <input v-model="authForm.businessLicenseUrl" class="input" placeholder="Enter the business license image URL" />
              <button class="btn-outline" :loading="uploadState.businessLicenseUrl" @click="uploadAuthImage('businessLicenseUrl')">
                Upload
              </button>
              <button class="btn-outline" @click="previewImage(authForm.businessLicenseUrl)">Preview</button>
            </view>
          </view>
          <view class="field">
            <text class="field-label">Front ID card URL</text>
            <view class="row-inline">
              <input v-model="authForm.idCardFrontUrl" class="input" placeholder="Enter the front ID card image URL" />
              <button class="btn-outline" :loading="uploadState.idCardFrontUrl" @click="uploadAuthImage('idCardFrontUrl')">
                Upload
              </button>
              <button class="btn-outline" @click="previewImage(authForm.idCardFrontUrl)">Preview</button>
            </view>
          </view>
          <view class="field">
            <text class="field-label">Back ID card URL</text>
            <view class="row-inline">
              <input v-model="authForm.idCardBackUrl" class="input" placeholder="Enter the back ID card image URL" />
              <button class="btn-outline" :loading="uploadState.idCardBackUrl" @click="uploadAuthImage('idCardBackUrl')">
                Upload
              </button>
              <button class="btn-outline" @click="previewImage(authForm.idCardBackUrl)">Preview</button>
            </view>
          </view>
          <view class="field">
            <text class="field-label">Contact phone</text>
            <input v-model="authForm.contactPhone" class="input" placeholder="Enter the contact phone number" />
          </view>
          <view class="field">
            <text class="field-label">Contact address</text>
            <textarea v-model="authForm.contactAddress" class="textarea" placeholder="Enter the contact address" />
          </view>
        </view>
        <view class="actions">
          <button class="btn-primary" :loading="authSaving" @click="submitAuth">Submit auth</button>
          <button v-if="authInfo" class="btn-outline" @click="revokeAuth">Revoke auth</button>
        </view>
      </view>

      <view class="card glass-card">
        <text class="label">Merchant statistics</text>
        <view v-if="statsEntries.length === 0" class="muted">No statistics available</view>
        <view v-else class="stats-list">
          <view v-for="entry in statsEntries" :key="entry.key" class="stats-row">
            <text class="stats-key">{{ entry.key }}</text>
            <text class="stats-value">{{ entry.value ?? '-' }}</text>
          </view>
        </view>
      </view>
    </view>
  </AppShell>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card.wide {
  grid-column: span 2;
}

.label {
  font-size: 12px;
  color: var(--text-muted);
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.muted {
  font-size: 12px;
  color: var(--text-muted);
}

.form {
  display: grid;
  gap: 10px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.row-inline .input {
  flex: 1;
  min-width: 200px;
}

.field-label {
  font-size: 12px;
  color: var(--text-muted);
}

.input,
.textarea {
  background: #fff;
  border-radius: 12px;
  padding: 8px 12px;
  font-size: 14px;
}

.textarea {
  min-height: 72px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-chip {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(36, 107, 255, 0.1);
  color: var(--accent);
}

.stats-list {
  display: grid;
  gap: 8px;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.stats-key {
  color: var(--text-muted);
}

.stats-value {
  font-weight: 600;
}

@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .card.wide {
    grid-column: span 1;
  }
}
</style>
