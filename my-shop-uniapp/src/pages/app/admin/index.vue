<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import AppShell from "../../../components/AppShell.vue";
import {
    getMerchants,
    approveMerchant,
    rejectMerchant,
} from "../../../api/merchant";
import {
    listMerchantAuthByStatus,
    reviewMerchantAuth,
} from "../../../api/merchant-auth";
import { searchUsers } from "../../../api/user-management";
import { getAdmins } from "../../../api/admin";
import { getStatisticsOverview } from "../../../api/statistics";
import { toast } from "../../../utils/ui";

type TabKey = "overview" | "review" | "merchants" | "users" | "admins";

const tabs: { key: TabKey; label: string; desc: string }[] = [
    { key: "overview", label: "Overview", desc: "Control room" },
    { key: "review", label: "Merchant Review", desc: "Audit queue" },
    { key: "merchants", label: "Merchants", desc: "Store ops" },
    { key: "users", label: "Users", desc: "Accounts" },
    { key: "admins", label: "Admins", desc: "Permissions" },
];

const activeTab = ref<TabKey>("review");

const reviewLoading = ref(false);
const reviewStatus = ref("0");
const reviewRemark = ref("");
const reviewRows = ref<Record<string, any>[]>([]);

const merchantLoading = ref(false);
const merchantRows = ref<Record<string, any>[]>([]);

const userLoading = ref(false);
const userRows = ref<Record<string, any>[]>([]);

const adminLoading = ref(false);
const adminRows = ref<Record<string, any>[]>([]);

const overview = reactive({
    merchantCount: 0,
    userCount: 0,
    adminCount: 0,
    pendingReviews: 0,
});

const statisticsOverview = ref<Record<string, any> | null>(null);

const heroCards = computed(() => [
    { label: "Pending reviews", value: overview.pendingReviews },
    { label: "Merchants", value: overview.merchantCount },
    { label: "Users", value: overview.userCount },
    { label: "Admins", value: overview.adminCount },
]);

function authStatusText(status?: number): string {
    if (status === 1) return "Approved";
    if (status === 2) return "Rejected";
    if (status === 0) return "Pending";
    return "Unknown";
}

function merchantStatusText(status?: number): string {
    if (status === 1) return "Enabled";
    if (status === 0) return "Disabled";
    return "Unknown";
}

async function loadReviewRows(): Promise<void> {
    reviewLoading.value = true;
    try {
        reviewRows.value = await listMerchantAuthByStatus(
            Number(reviewStatus.value),
        );
        overview.pendingReviews = reviewRows.value.length;
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to load review queue",
        );
    } finally {
        reviewLoading.value = false;
    }
}

async function loadMerchants(): Promise<void> {
    merchantLoading.value = true;
    try {
        const result = await getMerchants({ page: 1, size: 12 });
        merchantRows.value = result.records || [];
        overview.merchantCount = result.total || merchantRows.value.length;
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Failed to load merchants",
        );
    } finally {
        merchantLoading.value = false;
    }
}

async function loadUsers(): Promise<void> {
    userLoading.value = true;
    try {
        const result = await searchUsers({ page: 1, size: 12 });
        userRows.value = result.records || [];
        overview.userCount = result.total || userRows.value.length;
    } catch (error) {
        toast(error instanceof Error ? error.message : "Failed to load users");
    } finally {
        userLoading.value = false;
    }
}

async function loadAdmins(): Promise<void> {
    adminLoading.value = true;
    try {
        const result = await getAdmins({ page: 1, size: 12 });
        adminRows.value = result.records || [];
        overview.adminCount = result.total || adminRows.value.length;
    } catch (error) {
        toast(error instanceof Error ? error.message : "Failed to load admins");
    } finally {
        adminLoading.value = false;
    }
}

async function loadOverview(): Promise<void> {
    try {
        statisticsOverview.value = (await getStatisticsOverview()) as Record<
            string,
            any
        >;
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to load statistics",
        );
    }
}

async function approveReview(row: Record<string, any>): Promise<void> {
    const merchantId = Number(row.merchantId);
    if (!Number.isFinite(merchantId)) {
        toast("Merchant ID is missing");
        return;
    }
    try {
        await reviewMerchantAuth(
            merchantId,
            1,
            reviewRemark.value.trim() || undefined,
        );
        toast("Merchant review approved", "success");
        await Promise.all([loadReviewRows(), loadMerchants()]);
    } catch (error) {
        toast(error instanceof Error ? error.message : "Approve failed");
    }
}

async function rejectReview(row: Record<string, any>): Promise<void> {
    const merchantId = Number(row.merchantId);
    if (!Number.isFinite(merchantId)) {
        toast("Merchant ID is missing");
        return;
    }
    try {
        await reviewMerchantAuth(
            merchantId,
            2,
            reviewRemark.value.trim() || undefined,
        );
        toast("Merchant review rejected", "success");
        await Promise.all([loadReviewRows(), loadMerchants()]);
    } catch (error) {
        toast(error instanceof Error ? error.message : "Reject failed");
    }
}

async function approveMerchantRow(row: Record<string, any>): Promise<void> {
    if (typeof row.id !== "number") return;
    try {
        await approveMerchant(row.id);
        toast("Merchant approved", "success");
        await Promise.all([loadMerchants(), loadReviewRows()]);
    } catch (error) {
        toast(error instanceof Error ? error.message : "Approve failed");
    }
}

async function rejectMerchantRow(row: Record<string, any>): Promise<void> {
    if (typeof row.id !== "number") return;
    try {
        await rejectMerchant(row.id, reviewRemark.value.trim() || undefined);
        toast("Merchant rejected", "success");
        await Promise.all([loadMerchants(), loadReviewRows()]);
    } catch (error) {
        toast(error instanceof Error ? error.message : "Reject failed");
    }
}

async function init(): Promise<void> {
    await Promise.all([
        loadOverview(),
        loadReviewRows(),
        loadMerchants(),
        loadUsers(),
        loadAdmins(),
    ]);
}

onMounted(() => {
    void init();
});
</script>

<template>
    <AppShell title="Admin Center">
        <view class="admin-page">
            <view class="display-panel dashboard-hero fade-in-up">
                <view class="dashboard-hero-copy">
                    <text class="hero-eyebrow">Administrator workspace</text>
                    <text class="hero-title">Focus merchant review first.</text>
                    <text class="hero-subtitle">
                        Prioritize merchant audit, then manage stores, users,
                        and platform operators in one place.
                    </text>
                    <view class="action-wrap">
                        <button class="btn-primary" @click="activeTab = 'review'">
                            Open review queue
                        </button>
                        <button class="btn-outline" @click="init">
                            Refresh workspace
                        </button>
                    </view>
                </view>

                <view class="dashboard-hero-stats">
                    <view
                        v-for="item in heroCards"
                        :key="item.label"
                        class="metric-card"
                    >
                        <text class="metric-label">{{ item.label }}</text>
                        <text class="metric-value">{{ item.value }}</text>
                    </view>
                </view>
            </view>

            <view class="tab-strip fade-in-up">
                <view
                    v-for="tab in tabs"
                    :key="tab.key"
                    class="tab-pill"
                    :class="{ active: activeTab === tab.key }"
                    @click="activeTab = tab.key"
                >
                    <text class="tab-label">{{ tab.label }}</text>
                    <text class="tab-desc">{{ tab.desc }}</text>
                </view>
            </view>

            <view
                v-if="activeTab === 'overview'"
                class="dashboard-grid-main fade-in-up"
            >
                <view class="surface-card panel-block">
                    <text class="panel-title">Platform snapshot</text>
                    <text class="section-subtitle"
                        >A concise summary of the current platform state.</text
                    >
                    <view class="kv-list">
                        <view class="kv-row">
                            <text class="kv-key">Pending merchant reviews</text>
                            <text class="kv-value">{{
                                overview.pendingReviews
                            }}</text>
                        </view>
                        <view class="kv-row">
                            <text class="kv-key">Merchant accounts</text>
                            <text class="kv-value">{{
                                overview.merchantCount
                            }}</text>
                        </view>
                        <view class="kv-row">
                            <text class="kv-key">User accounts</text>
                            <text class="kv-value">{{
                                overview.userCount
                            }}</text>
                        </view>
                        <view class="kv-row">
                            <text class="kv-key">Admin accounts</text>
                            <text class="kv-value">{{
                                overview.adminCount
                            }}</text>
                        </view>
                    </view>
                </view>

                <view class="surface-card panel-block sticky-side">
                    <text class="panel-title">Immediate actions</text>
                    <text class="section-subtitle">
                        Move to the next operational queue without scanning the
                        whole screen.
                    </text>
                    <view class="action-stack">
                        <button
                            class="btn-primary"
                            @click="activeTab = 'review'"
                        >
                            Review merchant materials
                        </button>
                        <button
                            class="btn-outline"
                            @click="activeTab = 'merchants'"
                        >
                            Open merchant directory
                        </button>
                        <button
                            class="btn-outline"
                            @click="activeTab = 'users'"
                        >
                            Inspect user accounts
                        </button>
                    </view>

                    <view class="surface-muted panel-block code-panel">
                        <text class="metric-label">Statistics response</text>
                        <text class="code-block">{{
                            statisticsOverview
                                ? JSON.stringify(statisticsOverview, null, 2)
                                : "--"
                        }}</text>
                    </view>
                </view>
            </view>

            <view
                v-else-if="activeTab === 'review'"
                class="surface-card panel-block fade-in-up"
            >
                <view class="panel-head">
                    <view>
                        <text class="panel-title">Merchant review queue</text>
                        <text class="section-subtitle">Review merchant profiles, business licenses, ID documents and contact information.</text>
                    </view>
                    <button
                        class="btn-outline"
                        :loading="reviewLoading"
                        @click="loadReviewRows"
                    >
                        Refresh
                    </button>
                </view>

                <view class="toolbar">
                    <picker
                        :range="['Pending', 'Approved', 'Rejected']"
                        :value="Number(reviewStatus)"
                        @change="
                            reviewStatus = String($event.detail.value);
                            loadReviewRows();
                        "
                    >
                        <view class="select-like field-control field-control-pill"
                            >Status:
                            {{ authStatusText(Number(reviewStatus)) }}</view
                        >
                    </picker>
                    <input
                        v-model="reviewRemark"
                        class="field-control review-input"
                        placeholder="Review remark or rejection reason"
                    />
                </view>

                <view v-if="reviewRows.length === 0" class="empty-state">
                    <text>No merchant reviews in this queue.</text>
                </view>

                <view v-else class="stack-list">
                    <view
                        v-for="row in reviewRows"
                        :key="row.id || row.merchantId"
                        class="surface-muted panel-block review-card"
                    >
                        <view class="review-main">
                            <text class="review-title">{{
                                row.merchantName ||
                                row.username ||
                                `Merchant #${row.merchantId}`
                            }}</text>
                            <text class="review-meta"
                                >Merchant ID: {{ row.merchantId ?? "--" }}</text
                            >
                            <text class="review-meta"
                                >Status:
                                {{ authStatusText(row.authStatus) }}</text
                            >
                            <text class="review-meta"
                                >License No:
                                {{ row.businessLicenseNumber || "--" }}</text
                            >
                            <text class="review-meta"
                                >Contact Phone:
                                {{ row.contactPhone || "--" }}</text
                            >
                            <text class="review-meta"
                                >Contact Address:
                                {{ row.contactAddress || "--" }}</text
                            >
                            <text class="review-meta"
                                >Business License URL:
                                {{ row.businessLicenseUrl || "--" }}</text
                            >
                            <text class="review-meta"
                                >Front ID URL:
                                {{ row.idCardFrontUrl || "--" }}</text
                            >
                            <text class="review-meta"
                                >Back ID URL:
                                {{ row.idCardBackUrl || "--" }}</text
                            >
                        </view>
                        <view class="review-actions">
                            <button
                                class="btn-primary"
                                @click="approveReview(row)"
                            >
                                Approve review
                            </button>
                            <button
                                class="btn-outline"
                                @click="rejectReview(row)"
                            >
                                Reject review
                            </button>
                        </view>
                    </view>
                </view>
            </view>

            <view
                v-else-if="activeTab === 'merchants'"
                class="surface-card panel-block fade-in-up"
            >
                <view class="panel-head">
                    <view>
                        <text class="panel-title">Merchant management</text>
                        <text class="section-subtitle"
                            >Review merchant status, approval, and store
                            availability.</text
                        >
                    </view>
                    <button
                        class="btn-outline"
                        :loading="merchantLoading"
                        @click="loadMerchants"
                    >
                        Refresh
                    </button>
                </view>

                <view v-if="merchantRows.length === 0" class="empty-state">
                    <text>No merchants found.</text>
                </view>

                <view v-else class="stack-list">
                    <view
                        v-for="row in merchantRows"
                        :key="row.id"
                        class="surface-muted panel-block row-card"
                    >
                        <view class="row-main">
                            <text class="row-title">{{
                                row.merchantName || row.username || "Merchant"
                            }}</text>
                            <text class="row-meta"
                                >Merchant ID: {{ row.id }}</text
                            >
                            <text class="row-meta"
                                >Phone: {{ row.phone || "--" }}</text
                            >
                            <text class="row-meta"
                                >Email: {{ row.email || "--" }}</text
                            >
                            <text class="row-meta"
                                >Status:
                                {{ merchantStatusText(row.status) }}</text
                            >
                            <text class="row-meta"
                                >Auth Status:
                                {{ authStatusText(row.authStatus) }}</text
                            >
                        </view>
                        <view class="row-actions">
                            <button
                                class="btn-primary"
                                @click="approveMerchantRow(row)"
                            >
                                Approve
                            </button>
                            <button
                                class="btn-outline"
                                @click="rejectMerchantRow(row)"
                            >
                                Reject
                            </button>
                        </view>
                    </view>
                </view>
            </view>

            <view
                v-else-if="activeTab === 'users'"
                class="surface-card panel-block fade-in-up"
            >
                <view class="panel-head">
                    <view>
                        <text class="panel-title">User management</text>
                        <text class="section-subtitle"
                            >Quick user directory for support and
                            governance.</text
                        >
                    </view>
                    <button
                        class="btn-outline"
                        :loading="userLoading"
                        @click="loadUsers"
                    >
                        Refresh
                    </button>
                </view>

                <view v-if="userRows.length === 0" class="empty-state">
                    <text>No users found.</text>
                </view>

                <view v-else class="stack-list">
                    <view
                        v-for="row in userRows"
                        :key="row.id"
                        class="surface-muted panel-block row-card"
                    >
                        <view class="row-main">
                            <text class="row-title">{{
                                row.username || row.nickname || "User"
                            }}</text>
                            <text class="row-meta">User ID: {{ row.id }}</text>
                            <text class="row-meta"
                                >Phone: {{ row.phone || "--" }}</text
                            >
                            <text class="row-meta"
                                >Email: {{ row.email || "--" }}</text
                            >
                            <text class="row-meta"
                                >Status:
                                {{
                                    row.status === 1 ? "Enabled" : "Disabled"
                                }}</text
                            >
                        </view>
                    </view>
                </view>
            </view>

            <view
                v-else-if="activeTab === 'admins'"
                class="surface-card panel-block fade-in-up"
            >
                <view class="panel-head">
                    <view>
                        <text class="panel-title">Administrator accounts</text>
                        <text class="section-subtitle"
                            >Inspect platform operator accounts.</text
                        >
                    </view>
                    <button
                        class="btn-outline"
                        :loading="adminLoading"
                        @click="loadAdmins"
                    >
                        Refresh
                    </button>
                </view>

                <view v-if="adminRows.length === 0" class="empty-state">
                    <text>No admins found.</text>
                </view>

                <view v-else class="stack-list">
                    <view
                        v-for="row in adminRows"
                        :key="row.id"
                        class="surface-muted panel-block row-card"
                    >
                        <view class="row-main">
                            <text class="row-title">{{
                                row.realName || row.username || "Admin"
                            }}</text>
                            <text class="row-meta">Admin ID: {{ row.id }}</text>
                            <text class="row-meta"
                                >Username: {{ row.username || "--" }}</text
                            >
                            <text class="row-meta"
                                >Role: {{ row.role || "--" }}</text
                            >
                            <text class="row-meta"
                                >Phone: {{ row.phone || "--" }}</text
                            >
                        </view>
                    </view>
                </view>
            </view>

            <view v-else class="surface-card panel-block fade-in-up">
                <text class="panel-title">Module in progress</text>
                <text class="section-subtitle"
                    >This section is reserved for the next admin
                    iteration.</text
                >
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.admin-page {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.row-meta,
.review-meta,
.metric-label,
.tab-desc,
.code-block,
.kv-key {
    color: var(--text-muted);
}

.row-meta,
.review-meta,
.kv-row {
    font-size: 14px;
}

.row-actions,
.review-actions,
.action-stack,
.toolbar {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    flex-wrap: wrap;
}

.metric-value {
    font-size: 30px;
    font-weight: 700;
    color: var(--text-main);
}

.tab-strip {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    padding-bottom: 4px;
}

.tab-pill {
    min-width: 160px;
    padding: 16px 18px;
    border-radius: 22px;
    background: rgba(10, 22, 35, 0.84);
    border: 1px solid var(--panel-border);
    display: flex;
    flex-direction: column;
    gap: 4px;
    transition:
        transform 0.22s ease,
        border-color 0.22s ease,
        background-color 0.22s ease;
}

.tab-pill.active {
    background: linear-gradient(
        135deg,
        rgba(95, 209, 194, 0.16),
        rgba(240, 182, 90, 0.12)
    );
    border-color: var(--panel-border-strong);
}

.tab-label,
.panel-title,
.row-title,
.review-title,
.kv-value {
    color: var(--text-main);
    font-weight: 600;
}

.panel-head {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: flex-start;
    flex-wrap: wrap;
}

.kv-list,
.stack-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.kv-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.review-card,
.row-card {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
}

.review-main,
.row-main {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 240px;
    flex: 1;
}

.code-panel {
    gap: 12px;
}

.code-block {
    white-space: pre-wrap;
    font-size: 12px;
    line-height: 1.7;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 18px;
    padding: 14px;
    border: 1px solid var(--panel-border);
}

.select-like {
    display: flex;
    align-items: center;
}

.review-input {
    flex: 1;
    min-width: 260px;
}

.empty-state {
    padding: 32px 18px;
}

@media (max-width: 1100px) {
    .review-actions,
    .row-actions {
        width: 100%;
    }
}

@media (max-width: 760px) {
    .review-card,
    .row-card {
        flex-direction: column;
    }

    .review-input {
        min-width: 100%;
    }
}

@media (hover: hover) {
    .tab-pill:hover {
        transform: translateY(-1px);
        border-color: var(--panel-border-strong);
    }
}
</style>
