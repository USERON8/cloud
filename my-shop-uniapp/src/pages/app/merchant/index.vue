<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { sessionState } from '../../../auth/session'
import type { MerchantAuthInfo, MerchantAuthPayload, MerchantInfo, MerchantUpsertPayload } from '../../../types/domain'
import { getMerchantById, getMerchantStatistics, updateMerchant } from '../../../api/merchant'
import { applyMerchantAuth, getMerchantAuth, revokeMerchantAuth } from '../../../api/merchant-auth'
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
  if (status === 1) return '通过'
  if (status === 2) return '驳回'
  if (status === 0) return '待审核'
  return '未知'
}

function resolveUploadUrl(): string | null {
  if (!uploadUrl) {
    toast('未配置上传地址，请设置 VITE_MERCHANT_AUTH_UPLOAD_URL 或 VITE_UPLOAD_URL')
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
  const target = resolveUploadUrl()
  if (!target) return
  uploadState[field] = true
  try {
    const chooseResult = await uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera']
    })
    const filePath = chooseResult.tempFilePaths?.[0]
    if (!filePath) {
      toast('未选择图片')
      return
    }
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
      toast('上传失败')
      return
    }
    const url = extractUploadUrl(uploadResult.data)
    if (!url) {
      toast('未获取到上传地址')
      return
    }
    authForm[field] = url
    toast('上传成功', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploadState[field] = false
  }
}

function previewImage(url?: string): void {
  if (!url) {
    toast('暂无可预览图片')
    return
  }
  uni.previewImage({
    urls: [url]
  })
}

async function loadMerchant(): Promise<void> {
  if (!merchantId.value) {
    toast('商家会话缺失，请重新登录')
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
    toast(error instanceof Error ? error.message : '加载商家信息失败')
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
    toast('资料已更新', 'success')
    await loadMerchant()
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
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
    toast('认证已提交', 'success')
    await loadMerchant()
  } catch (error) {
    toast(error instanceof Error ? error.message : '提交失败')
  } finally {
    authSaving.value = false
  }
}

async function revokeAuth(): Promise<void> {
  if (!merchantId.value) {
    return
  }
  const ok = await confirm('确认撤销商家认证？')
  if (!ok) {
    return
  }
  try {
    await revokeMerchantAuth(merchantId.value)
    toast('已撤销', 'success')
    await loadMerchant()
  } catch (error) {
    toast(error instanceof Error ? error.message : '撤销失败')
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
        <text class="label">商家概览</text>
        <text class="title">{{ merchantInfo?.merchantName || merchantInfo?.username || '商家' }}</text>
        <text class="muted">商家 ID：{{ merchantInfo?.id ?? '-' }}</text>
        <text class="muted">状态：{{ statusText(merchantInfo?.status) }}</text>
        <text class="muted">认证状态：{{ statusText(merchantInfo?.authStatus) }}</text>
        <button class="btn-outline" :loading="loading" @click="loadMerchant">刷新</button>
      </view>

      <view class="card glass-card wide">
        <text class="label">商家资料</text>
        <view class="form">
          <view class="field">
            <text class="field-label">商家名称</text>
            <input v-model="profileForm.merchantName" class="input" placeholder="请输入商家名称" />
          </view>
          <view class="field">
            <text class="field-label">邮箱</text>
            <input v-model="profileForm.email" class="input" placeholder="请输入邮箱" />
          </view>
          <view class="field">
            <text class="field-label">联系电话</text>
            <input v-model="profileForm.phone" class="input" placeholder="请输入联系电话" />
          </view>
        </view>
        <button class="btn-primary" :loading="saving" @click="saveProfile">保存资料</button>
      </view>

      <view class="card glass-card wide">
        <view class="section-head">
          <text class="label">商家认证</text>
          <text v-if="authInfo" class="status-chip">{{ statusText(authInfo.authStatus) }}</text>
        </view>
        <view class="form">
          <view class="field">
            <text class="field-label">营业执照号</text>
            <input v-model="authForm.businessLicenseNumber" class="input" placeholder="请输入营业执照号" />
          </view>
          <view class="field">
            <text class="field-label">营业执照 URL</text>
            <view class="row-inline">
              <input v-model="authForm.businessLicenseUrl" class="input" placeholder="请输入营业执照图片地址" />
              <button class="btn-outline" :loading="uploadState.businessLicenseUrl" @click="uploadAuthImage('businessLicenseUrl')">
                上传
              </button>
              <button class="btn-outline" @click="previewImage(authForm.businessLicenseUrl)">预览</button>
            </view>
          </view>
          <view class="field">
            <text class="field-label">身份证正面 URL</text>
            <view class="row-inline">
              <input v-model="authForm.idCardFrontUrl" class="input" placeholder="请输入身份证正面地址" />
              <button class="btn-outline" :loading="uploadState.idCardFrontUrl" @click="uploadAuthImage('idCardFrontUrl')">
                上传
              </button>
              <button class="btn-outline" @click="previewImage(authForm.idCardFrontUrl)">预览</button>
            </view>
          </view>
          <view class="field">
            <text class="field-label">身份证反面 URL</text>
            <view class="row-inline">
              <input v-model="authForm.idCardBackUrl" class="input" placeholder="请输入身份证反面地址" />
              <button class="btn-outline" :loading="uploadState.idCardBackUrl" @click="uploadAuthImage('idCardBackUrl')">
                上传
              </button>
              <button class="btn-outline" @click="previewImage(authForm.idCardBackUrl)">预览</button>
            </view>
          </view>
          <view class="field">
            <text class="field-label">联系人电话</text>
            <input v-model="authForm.contactPhone" class="input" placeholder="请输入联系人电话" />
          </view>
          <view class="field">
            <text class="field-label">联系地址</text>
            <textarea v-model="authForm.contactAddress" class="textarea" placeholder="请输入联系地址" />
          </view>
        </view>
        <view class="actions">
          <button class="btn-primary" :loading="authSaving" @click="submitAuth">提交认证</button>
          <button v-if="authInfo" class="btn-outline" @click="revokeAuth">撤销认证</button>
        </view>
      </view>

      <view class="card glass-card">
        <text class="label">商家统计</text>
        <view v-if="statsEntries.length === 0" class="muted">暂无统计数据</view>
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


