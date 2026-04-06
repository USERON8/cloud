<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import AppShell from "../../../components/AppShell.vue";
import { getStockLedger } from "../../../api/stock";
import { useRole } from "../../../auth/permission";
import { redirectTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";
import type { StockLedger } from "../../../types/domain";
import { toast } from "../../../utils/ui";

const { isAdmin } = useRole();

const skuId = ref("");
const ledger = ref<StockLedger | null>(null);
const loading = ref(false);

const heroCards = computed(() => [
    { label: "Available", value: ledger.value?.availableQty ?? "--" },
    { label: "Locked", value: ledger.value?.lockedQty ?? "--" },
    { label: "Sold", value: ledger.value?.soldQty ?? "--" },
]);

function ensureAdminAccess(): boolean {
    if (isAdmin.value) {
        return true;
    }
    toast("Administrator access is required");
    redirectTo(Routes.forbidden);
    return false;
}

function formatLedgerStatus(status?: number): string {
    if (typeof status !== "number") {
        return "Unknown";
    }
    if (status === 1) {
        return "Active";
    }
    return `Status ${status}`;
}

function resolveLedgerHealth(
    currentLedger?: StockLedger | null,
): { label: string; tone: string } {
    if (!currentLedger) {
        return { label: "Unknown", tone: "status-muted" };
    }
    if (currentLedger.status !== 1) {
        return { label: "Inactive", tone: "status-danger" };
    }
    const availableQty = currentLedger.availableQty;
    const alertThreshold = currentLedger.alertThreshold;
    if (typeof availableQty !== "number") {
        return { label: "Active", tone: "status-success" };
    }
    if (availableQty <= 0) {
        return { label: "Out of stock", tone: "status-danger" };
    }
    if (
        typeof alertThreshold === "number" &&
        availableQty <= Math.max(alertThreshold, 0)
    ) {
        return { label: "Low stock", tone: "status-warning" };
    }
    return { label: "Healthy", tone: "status-success" };
}

function ledgerStatusTone(status?: number): string {
    if (typeof status !== "number") {
        return "status-muted";
    }
    if (status === 1) {
        return "status-success";
    }
    return "status-danger";
}

async function queryLedger(): Promise<void> {
    if (!ensureAdminAccess()) {
        return;
    }
    const id = Number(skuId.value);
    if (!Number.isFinite(id) || id <= 0) {
        toast("Enter a valid SKU ID");
        return;
    }
    loading.value = true;
    try {
        ledger.value = await getStockLedger(id);
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to query the stock ledger",
        );
    } finally {
        loading.value = false;
    }
}

onShow(() => {
    void Promise.resolve().then(() => {
        ensureAdminAccess();
    });
});
</script>

<template>
    <AppShell title="Stock Ledger">
        <view class="stock-page">
            <view class="display-panel dashboard-hero fade-in-up">
                <view class="dashboard-hero-copy">
                    <text class="hero-eyebrow">Admin operations</text>
                    <text class="hero-title">Stock ledger console</text>
                    <text class="hero-subtitle">
                        Query live inventory by SKU ID and inspect availability,
                        locked volume, and ledger status in one panel.
                    </text>

                    <view class="toolbar-grid">
                        <input
                            v-model="skuId"
                            class="field-control field-control-pill"
                            placeholder="Enter SKU ID"
                            type="number"
                            @confirm="queryLedger"
                        />
                        <button
                            class="btn-primary"
                            :loading="loading"
                            @click="queryLedger"
                        >
                            Query
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

            <view v-if="ledger" class="surface-card panel-block fade-in-up">
                <view class="section-head">
                    <text class="section-title">Ledger detail</text>
                    <text class="section-subtitle">
                        Current stock counters and threshold fields for the
                        requested SKU.
                    </text>
                </view>

                <view class="meta-inline">
                    <text
                        class="meta-chip"
                        :class="ledgerStatusTone(ledger.status)"
                    >
                        {{ formatLedgerStatus(ledger.status) }}
                    </text>
                    <text
                        class="meta-chip"
                        :class="resolveLedgerHealth(ledger).tone"
                    >
                        {{ resolveLedgerHealth(ledger).label }}
                    </text>
                    <text class="meta-chip status-muted">
                        SKU ID: {{ ledger.skuId ?? "--" }}
                    </text>
                </view>

                <view class="summary-grid ledger-grid">
                    <view class="summary-item">
                        <text class="summary-label">Available Qty</text>
                        <text class="summary-value">{{
                            ledger.availableQty ?? "--"
                        }}</text>
                    </view>
                    <view class="summary-item">
                        <text class="summary-label">Locked Qty</text>
                        <text class="summary-value">{{
                            ledger.lockedQty ?? "--"
                        }}</text>
                    </view>
                    <view class="summary-item">
                        <text class="summary-label">Sold Qty</text>
                        <text class="summary-value">{{
                            ledger.soldQty ?? "--"
                        }}</text>
                    </view>
                    <view class="summary-item">
                        <text class="summary-label">Segment Count</text>
                        <text class="summary-value">{{
                            ledger.segmentCount ?? "--"
                        }}</text>
                    </view>
                    <view class="summary-item">
                        <text class="summary-label">Alert Threshold</text>
                        <text class="summary-value">{{
                            ledger.alertThreshold ?? "--"
                        }}</text>
                    </view>
                    <view class="summary-item">
                        <text class="summary-label">Updated At</text>
                        <text class="summary-value">{{
                            ledger.updatedAt ?? "--"
                        }}</text>
                    </view>
                </view>
            </view>

            <view v-else-if="!loading" class="empty-state fade-in-up">
                <text>Enter a SKU ID and query to inspect stock details.</text>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.stock-page {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.toolbar-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 12px;
    align-items: center;
}

.ledger-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 768px) {
    .toolbar-grid,
    .ledger-grid {
        grid-template-columns: 1fr;
    }
}
</style>
