<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import AppShell from "../../../components/AppShell.vue";
import { getCurrentUserId } from "../../../auth/session";
import {
    applyMerchantAuth,
    getMerchantAuth,
    revokeMerchantAuth,
    uploadMerchantAuthFile,
} from "../../../api/merchant-auth";
import {
    getMerchants,
    getMerchantById,
    getMerchantStatistics,
    updateMerchant,
} from "../../../api/merchant";
import { Routes } from "../../../router/routes";
import { ensurePageAccess, navigateTo } from "../../../router/navigation";
import { confirm, toast } from "../../../utils/ui";
import type { MerchantInfo, MerchantAuthInfo } from "../../../types/domain";

const merchantInfo = ref<MerchantInfo | null>(null);
const authInfo = ref<MerchantAuthInfo | null>(null);
const stats = ref<Record<string, unknown> | null>(null);
const loading = ref(false);
const saving = ref(false);
const authSaving = ref(false);
const merchantId = ref<number | null>(null);

let merchantIdPromise: Promise<number | null> | null = null;

const uploadState = reactive({
    businessLicenseUrl: false,
    idCardFrontUrl: false,
    idCardBackUrl: false,
});

const profileForm = reactive({
    merchantName: "",
    email: "",
    phone: "",
});

const authForm = reactive({
    businessLicenseNumber: "",
    businessLicenseUrl: "",
    idCardFrontUrl: "",
    idCardBackUrl: "",
    contactPhone: "",
    contactAddress: "",
});

const statsEntries = computed(() =>
    Object.entries(stats.value || {}).map(([key, value]) => ({
        key: prettyKey(key),
        rawKey: key,
        value,
    })),
);

const quickActions = computed(() => [
    {
        title: "Products",
        desc: "Manage product catalog and pricing",
        action: () => navigateTo(Routes.appCatalogManage),
    },
    {
        title: "Orders",
        desc: "Handle shipping and after-sale requests",
        action: () => navigateTo(Routes.appOrdersManage),
    },
]);

const heroStatusCards = computed(() => [
    {
        label: "Merchant status",
        value: merchantStatusText(merchantInfo.value?.status),
    },
    {
        label: "Auth status",
        value: authStatusText(
            authInfo.value?.authStatus ?? merchantInfo.value?.authStatus,
        ),
    },
]);

const readinessCards = computed(() => [
    {
        label: "Catalog entry",
        value: "Ready",
    },
    {
        label: "Order operations",
        value: "Available",
    },
]);

type UploadField = "businessLicenseUrl" | "idCardFrontUrl" | "idCardBackUrl";

function prettyKey(value: string): string {
    return value
        .replace(/([A-Z])/g, " $1")
        .replace(/[_-]/g, " ")
        .replace(/\s+/g, " ")
        .trim()
        .replace(/^./, (char) => char.toUpperCase());
}

function merchantStatusText(status?: number): string {
    if (status === 1) return "Enabled";
    if (status === 0) return "Disabled";
    return "Unknown";
}

function authStatusText(status?: number): string {
    if (status === 1) return "Approved";
    if (status === 2) return "Rejected";
    if (status === 0) return "Pending review";
    return "Not submitted";
}

function authTone(status?: number): string {
    if (status === 1) return "success";
    if (status === 2) return "danger";
    if (status === 0) return "warning";
    return "neutral";
}

async function resolveMerchantId(): Promise<number | null> {
    if (typeof merchantId.value === "number") {
        return merchantId.value;
    }
    if (!getCurrentUserId()) {
        return null;
    }
    if (merchantIdPromise) {
        return merchantIdPromise;
    }

    merchantIdPromise = getMerchants({ page: 1, size: 1 })
        .then((result) => {
            const currentMerchantId = result.records[0]?.id;
            merchantId.value =
                typeof currentMerchantId === "number" ? currentMerchantId : null;
            return merchantId.value;
        })
        .finally(() => {
            merchantIdPromise = null;
        });

    return merchantIdPromise;
}

async function requireMerchantId(): Promise<number | null> {
    try {
        const currentMerchantId = await resolveMerchantId();
        if (!currentMerchantId) {
            toast("Merchant profile is unavailable. Please contact support.");
        }
        return currentMerchantId;
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to resolve merchant profile",
        );
        return null;
    }
}

async function uploadAuthImage(field: UploadField): Promise<void> {
    uploadState[field] = true;
    try {
        const chooseResult = await uni.chooseImage({
            count: 1,
            sizeType: ["compressed"],
            sourceType: ["album", "camera"],
        });
        const filePath = chooseResult.tempFilePaths?.[0];
        if (!filePath) {
            toast("No image selected");
            return;
        }

        const currentMerchantId = await requireMerchantId();
        if (!currentMerchantId) return;
        const result = await uploadMerchantAuthFile(
            currentMerchantId,
            field,
            filePath,
        );
        authForm[field] = result.previewUrl;
        toast("Upload completed", "success");
    } catch (error) {
        toast(error instanceof Error ? error.message : "Upload failed");
    } finally {
        uploadState[field] = false;
    }
}

function previewImage(url?: string): void {
    if (!url) {
        toast("There is no image to preview");
        return;
    }
    uni.previewImage({ urls: [url] });
}

async function loadMerchant(): Promise<void> {
    const currentMerchantId = await requireMerchantId();
    if (!currentMerchantId) return;
    loading.value = true;
    try {
        const [info, auth, statResult] = await Promise.all([
            getMerchantById(currentMerchantId),
            getMerchantAuth(currentMerchantId),
            getMerchantStatistics(currentMerchantId),
        ]);

        merchantInfo.value = info;
        authInfo.value = auth;
        stats.value = statResult;
        merchantId.value = typeof info?.id === "number" ? info.id : currentMerchantId;

        profileForm.merchantName = info?.merchantName || "";
        profileForm.email = info?.email || "";
        profileForm.phone = info?.phone || "";

        authForm.businessLicenseNumber = auth?.businessLicenseNumber || "";
        authForm.businessLicenseUrl = auth?.businessLicenseUrl || "";
        authForm.idCardFrontUrl = auth?.idCardFrontUrl || "";
        authForm.idCardBackUrl = auth?.idCardBackUrl || "";
        authForm.contactPhone = auth?.contactPhone || "";
        authForm.contactAddress = auth?.contactAddress || "";
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to load merchant data",
        );
    } finally {
        loading.value = false;
    }
}

async function saveProfile(): Promise<void> {
    const currentMerchantId = await requireMerchantId();
    if (!currentMerchantId) return;
    saving.value = true;
    try {
        await updateMerchant(currentMerchantId, {
            merchantName: profileForm.merchantName,
            email: profileForm.email,
            phone: profileForm.phone,
        } as never);
        toast("Profile updated", "success");
        await loadMerchant();
    } catch (error) {
        toast(error instanceof Error ? error.message : "Update failed");
    } finally {
        saving.value = false;
    }
}

async function submitAuth(): Promise<void> {
    const currentMerchantId = await requireMerchantId();
    if (!currentMerchantId) return;
    authSaving.value = true;
    try {
        await applyMerchantAuth(currentMerchantId, { ...authForm } as never);
        toast("Merchant auth submitted", "success");
        await loadMerchant();
    } catch (error) {
        toast(error instanceof Error ? error.message : "Submit failed");
    } finally {
        authSaving.value = false;
    }
}

async function revokeAuth(): Promise<void> {
    const currentMerchantId = await requireMerchantId();
    if (!currentMerchantId) return;
    const ok = await confirm("Revoke merchant authentication?");
    if (!ok) return;
    try {
        await revokeMerchantAuth(currentMerchantId);
        toast("Merchant auth revoked", "success");
        await loadMerchant();
    } catch (error) {
        toast(error instanceof Error ? error.message : "Revoke failed");
    }
}

onMounted(() => {
    if (!ensurePageAccess(Routes.appMerchant)) {
        return;
    }
    void loadMerchant();
});
</script>

<template>
    <AppShell title="Merchant Center">
        <view class="merchant-page">
            <view class="display-panel dashboard-hero fade-in-up">
                <view class="dashboard-hero-copy">
                    <text class="hero-eyebrow">Merchant workspace</text>
                    <text class="hero-title">
                        {{
                            merchantInfo?.merchantName ||
                            merchantInfo?.username ||
                            "Merchant"
                        }}
                    </text>
                    <text class="hero-subtitle">
                        Operate your storefront from one place: update profile
                        data, submit business verification, review core metrics,
                        and jump directly into catalog or order handling.
                    </text>

                    <view class="hero-status-row">
                        <view
                            class="status-badge"
                            :class="authTone(merchantInfo?.status)"
                        >
                            Merchant:
                            {{ merchantStatusText(merchantInfo?.status) }}
                        </view>
                        <view
                            class="status-badge"
                            :class="
                                authTone(
                                    authInfo?.authStatus ??
                                        merchantInfo?.authStatus,
                                )
                            "
                        >
                            Auth:
                            {{
                                authStatusText(
                                    authInfo?.authStatus ??
                                        merchantInfo?.authStatus,
                                )
                            }}
                        </view>
                    </view>

                    <view class="action-wrap">
                        <button
                            class="btn-primary"
                            @click="navigateTo(Routes.appCatalogManage)"
                        >
                            Manage products
                        </button>
                        <button
                            class="btn-outline"
                            @click="navigateTo(Routes.appOrdersManage)"
                        >
                            Manage orders
                        </button>
                        <button
                            class="btn-outline"
                            :loading="loading"
                            @click="loadMerchant"
                        >
                            Refresh
                        </button>
                    </view>
                </view>

                <view class="dashboard-hero-stats">
                    <view
                        v-for="item in heroStatusCards"
                        :key="item.label"
                        class="metric-card"
                    >
                        <text class="metric-label">{{ item.label }}</text>
                        <text class="metric-value">{{ item.value }}</text>
                    </view>
                </view>
            </view>

            <view class="dashboard-grid-main fade-in-up">
                <view class="surface-card panel-block">
                    <view class="panel-head">
                        <view>
                            <text class="panel-title">Quick actions</text>
                            <text class="section-subtitle"
                                >Jump into the day-to-day merchant
                                operations.</text
                            >
                        </view>
                    </view>

                    <view class="quick-action-list">
                        <view
                            v-for="item in quickActions"
                            :key="item.title"
                            class="surface-muted panel-block quick-action-card"
                        >
                            <view class="quick-action-main">
                                <text class="quick-action-title">{{
                                    item.title
                                }}</text>
                                <text class="quick-action-desc">{{
                                    item.desc
                                }}</text>
                            </view>
                            <button class="btn-outline" @click="item.action()">
                                Open
                            </button>
                        </view>
                    </view>
                </view>

                <view class="surface-card panel-block sticky-side">
                    <view class="panel-head">
                        <view>
                            <text class="panel-title">Store snapshot</text>
                            <text class="section-subtitle"
                                >Core merchant identity and readiness.</text
                            >
                        </view>
                    </view>
                    <view class="kv-list">
                        <view class="kv-row">
                            <text class="kv-key">Merchant ID</text>
                            <text class="kv-value">{{
                                merchantInfo?.id ?? "-"
                            }}</text>
                        </view>
                        <view class="kv-row">
                            <text class="kv-key">Merchant name</text>
                            <text class="kv-value">{{
                                merchantInfo?.merchantName || "-"
                            }}</text>
                        </view>
                        <view class="kv-row">
                            <text class="kv-key">Username</text>
                            <text class="kv-value">{{
                                merchantInfo?.username || "-"
                            }}</text>
                        </view>
                        <view class="kv-row">
                            <text class="kv-key">Email</text>
                            <text class="kv-value">{{
                                merchantInfo?.email || "-"
                            }}</text>
                        </view>
                        <view class="kv-row">
                            <text class="kv-key">Phone</text>
                            <text class="kv-value">{{
                                merchantInfo?.phone || "-"
                            }}</text>
                        </view>
                    </view>

                    <view class="readiness-list">
                        <view
                            v-for="item in readinessCards"
                            :key="item.label"
                            class="surface-muted readiness-card"
                        >
                            <text class="field-label">{{ item.label }}</text>
                            <text class="readiness-value">{{ item.value }}</text>
                        </view>
                    </view>
                </view>
            </view>

            <view class="surface-card panel-block fade-in-up">
                <view class="panel-head">
                    <view>
                        <text class="panel-title">Merchant profile</text>
                        <text class="section-subtitle"
                            >Keep storefront identity and contact information up
                            to date.</text
                        >
                    </view>
                </view>

                <view class="form-grid">
                    <view class="field">
                        <text class="field-label">Merchant name</text>
                        <input
                            v-model="profileForm.merchantName"
                            class="field-control"
                            placeholder="Enter the merchant name"
                        />
                    </view>
                    <view class="field">
                        <text class="field-label">Email</text>
                        <input
                            v-model="profileForm.email"
                            class="field-control"
                            placeholder="Enter the email address"
                        />
                    </view>
                    <view class="field">
                        <text class="field-label">Phone</text>
                        <input
                            v-model="profileForm.phone"
                            class="field-control"
                            placeholder="Enter the phone number"
                        />
                    </view>
                </view>

                <view class="action-wrap">
                    <button
                        class="btn-primary"
                        :loading="saving"
                        @click="saveProfile"
                    >
                        Save profile
                    </button>
                </view>
            </view>

            <view class="surface-card panel-block fade-in-up">
                <view class="panel-head">
                    <view>
                        <text class="panel-title">Merchant authentication</text>
                        <text class="section-subtitle">
                            Submit or update your business verification
                            materials for administrator review.
                        </text>
                    </view>
                    <view
                        class="status-badge"
                        :class="authTone(authInfo?.authStatus)"
                    >
                        {{ authStatusText(authInfo?.authStatus) }}
                    </view>
                </view>

                <view class="form-grid">
                    <view class="field">
                        <text class="field-label">Business license number</text>
                        <input
                            v-model="authForm.businessLicenseNumber"
                            class="field-control"
                            placeholder="Enter the business license number"
                        />
                    </view>

                    <view class="field field-span">
                        <text class="field-label">Business license URL</text>
                        <view class="attachment-field">
                            <input
                                v-model="authForm.businessLicenseUrl"
                                class="field-control flex-input"
                                placeholder="Enter the business license image URL"
                            />
                            <view class="action-wrap attachment-actions">
                                <button
                                    class="btn-outline"
                                    :loading="uploadState.businessLicenseUrl"
                                    @click="
                                        uploadAuthImage('businessLicenseUrl')
                                    "
                                >
                                    Upload
                                </button>
                                <button
                                    v-if="authForm.businessLicenseUrl"
                                    class="btn-secondary"
                                    @click="
                                        previewImage(
                                            authForm.businessLicenseUrl,
                                        )
                                    "
                                >
                                    Preview
                                </button>
                            </view>
                        </view>
                    </view>

                    <view class="field field-span">
                        <text class="field-label">Front ID card URL</text>
                        <view class="attachment-field">
                            <input
                                v-model="authForm.idCardFrontUrl"
                                class="field-control flex-input"
                                placeholder="Enter the front ID card image URL"
                            />
                            <view class="action-wrap attachment-actions">
                                <button
                                    class="btn-outline"
                                    :loading="uploadState.idCardFrontUrl"
                                    @click="uploadAuthImage('idCardFrontUrl')"
                                >
                                    Upload
                                </button>
                                <button
                                    v-if="authForm.idCardFrontUrl"
                                    class="btn-secondary"
                                    @click="
                                        previewImage(authForm.idCardFrontUrl)
                                    "
                                >
                                    Preview
                                </button>
                            </view>
                        </view>
                    </view>

                    <view class="field field-span">
                        <text class="field-label">Back ID card URL</text>
                        <view class="attachment-field">
                            <input
                                v-model="authForm.idCardBackUrl"
                                class="field-control flex-input"
                                placeholder="Enter the back ID card image URL"
                            />
                            <view class="action-wrap attachment-actions">
                                <button
                                    class="btn-outline"
                                    :loading="uploadState.idCardBackUrl"
                                    @click="uploadAuthImage('idCardBackUrl')"
                                >
                                    Upload
                                </button>
                                <button
                                    v-if="authForm.idCardBackUrl"
                                    class="btn-secondary"
                                    @click="previewImage(authForm.idCardBackUrl)"
                                >
                                    Preview
                                </button>
                            </view>
                        </view>
                    </view>

                    <view class="field">
                        <text class="field-label">Contact phone</text>
                        <input
                            v-model="authForm.contactPhone"
                            class="field-control"
                            placeholder="Enter the contact phone number"
                        />
                    </view>

                    <view class="field field-span">
                        <text class="field-label">Contact address</text>
                        <textarea
                            v-model="authForm.contactAddress"
                            class="field-control textarea"
                            placeholder="Enter the contact address"
                        />
                    </view>
                </view>

                <view class="action-wrap">
                    <button
                        class="btn-primary"
                        :loading="authSaving"
                        @click="submitAuth"
                    >
                        Submit authentication
                    </button>
                    <button
                        v-if="authInfo"
                        class="btn-outline"
                        @click="revokeAuth"
                    >
                        Revoke auth
                    </button>
                </view>
            </view>

            <view class="surface-card panel-block fade-in-up">
                <view class="panel-head">
                    <view>
                        <text class="panel-title">Merchant statistics</text>
                        <text class="section-subtitle"
                            >Operational figures returned by the merchant
                            service.</text
                        >
                    </view>
                </view>

                <view v-if="statsEntries.length === 0" class="empty-state">
                    <text>No statistics available.</text>
                </view>

                <view v-else class="stats-grid">
                    <view
                        v-for="entry in statsEntries"
                        :key="entry.rawKey"
                        class="surface-muted stats-card"
                    >
                        <text class="stats-key">{{ entry.key }}</text>
                        <text class="stats-value">{{
                            entry.value ?? "-"
                        }}</text>
                    </view>
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.merchant-page {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.metric-label,
.kv-key,
.field-label,
.stats-key,
.quick-action-desc {
    color: var(--text-muted);
}

.quick-action-desc,
.kv-row,
.field-label {
    font-size: 14px;
}

.hero-status-row,
.input-row {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.status-badge {
    min-height: 34px;
    padding: 0 12px;
    border-radius: 999px;
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    background: rgba(148, 163, 184, 0.14);
    color: var(--text-muted);
}

.status-badge.success {
    background: rgba(16, 185, 129, 0.12);
    color: #7ee2b8;
}

.status-badge.warning {
    background: rgba(245, 158, 11, 0.12);
    color: #ffcf78;
}

.status-badge.danger {
    background: rgba(239, 68, 68, 0.12);
    color: #ff9e9e;
}

.metric-value,
.stats-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-main);
}

.panel-head {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: flex-start;
    flex-wrap: wrap;
}

.panel-title,
.quick-action-title,
.kv-value {
    color: var(--text-main);
    font-weight: 600;
}

.quick-action-list,
.kv-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.quick-action-card {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: center;
    flex-wrap: wrap;
}

.quick-action-main {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.readiness-value {
    margin-top: 6px;
}

.kv-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

.field {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.field-span {
    grid-column: span 2;
}

.textarea {
    width: 100%;
}

.textarea {
    min-height: 92px;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.stats-card {
    padding: 18px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.empty-state {
    padding: 28px 18px;
}

@media (max-width: 1100px) {
    .stats-grid {
        grid-template-columns: 1fr 1fr;
    }
}

@media (max-width: 760px) {
    .stats-grid,
    .form-grid {
        grid-template-columns: 1fr;
    }

    .field-span {
        grid-column: span 1;
    }
}
</style>
