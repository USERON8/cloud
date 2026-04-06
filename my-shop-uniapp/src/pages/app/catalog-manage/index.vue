<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import AppShell from "../../../components/AppShell.vue";
import { listProducts, updateProductStatus } from "../../../api/product";
import type { ProductItem } from "../../../types/domain";
import { formatPrice, formatProductStatus } from "../../../utils/format";
import { confirm, toast } from "../../../utils/ui";

const keyword = ref("");
const loading = ref(false);
const rows = ref<ProductItem[]>([]);

const publishedCount = computed(
    () => rows.value.filter((row) => row.status === 1).length,
);
const unpublishedCount = computed(
    () => rows.value.filter((row) => row.status !== 1).length,
);

function productStatusTone(status?: number): string {
    if (status === 1) return "status-success";
    if (status === 0) return "status-warning";
    return "status-muted";
}

async function loadProducts(): Promise<void> {
    if (loading.value) return;
    loading.value = true;
    try {
        const result = await listProducts({
            page: 1,
            size: 50,
            name: keyword.value || undefined,
        });
        rows.value = result.records;
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Failed to load products",
        );
    } finally {
        loading.value = false;
    }
}

async function toggleStatus(item: ProductItem): Promise<void> {
    if (typeof item.id !== "number") return;
    const nextStatus: 0 | 1 = item.status === 1 ? 0 : 1;
    const action = nextStatus === 1 ? "publish" : "unpublish";
    const ok = await confirm(`Set "${item.name}" to ${action}?`);
    if (!ok) return;
    try {
        await updateProductStatus(item.id, nextStatus);
        toast("Status updated", "success");
        await loadProducts();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to update status",
        );
    }
}

onMounted(() => {
    void loadProducts();
});
</script>

<template>
    <AppShell title="Product Admin">
        <view class="catalog-page">
            <view class="display-panel dashboard-hero fade-in-up">
                <view class="dashboard-hero-copy">
                    <text class="hero-eyebrow">Merchant operations</text>
                    <text class="hero-title">Catalog control room</text>
                    <text class="hero-subtitle">
                        Search products, review publication state, and switch
                        listings without leaving the merchant workflow.
                    </text>

                    <view class="action-wrap">
                        <button
                            class="btn-primary"
                            :loading="loading"
                            @click="loadProducts"
                        >
                            Refresh catalog
                        </button>
                    </view>
                </view>

                <view class="dashboard-hero-stats">
                    <view class="metric-card">
                        <text class="metric-label">Published</text>
                        <text class="metric-value">{{ publishedCount }}</text>
                    </view>
                    <view class="metric-card">
                        <text class="metric-label">Unpublished</text>
                        <text class="metric-value">{{ unpublishedCount }}</text>
                    </view>
                    <view class="metric-card">
                        <text class="metric-label">Total</text>
                        <text class="metric-value">{{ rows.length }}</text>
                    </view>
                </view>
            </view>

            <view class="dashboard-grid-main fade-in-up">
                <view class="surface-card panel-block">
                    <view class="section-head">
                        <text class="section-title">Search inventory</text>
                        <text class="section-subtitle">
                            Narrow the list by product name before taking a
                            publication action.
                        </text>
                    </view>

                    <view class="toolbar-grid">
                        <input
                            v-model="keyword"
                            class="field-control"
                            placeholder="Search products"
                            @confirm="loadProducts"
                        />
                        <button
                            class="btn-primary"
                            :loading="loading"
                            @click="loadProducts"
                        >
                            Search
                        </button>
                    </view>
                </view>

                <view class="surface-card panel-block sticky-side">
                    <view class="section-head">
                        <text class="section-title">Current rhythm</text>
                        <text class="section-subtitle">
                            Use publish for ready products and keep draft items
                            out of the live catalog.
                        </text>
                    </view>

                    <view class="surface-muted panel-block hint-card">
                        <text class="metric-label">Focus</text>
                        <text class="hint-copy">
                            Each row shows only the core product signal first:
                            name, price, status, and available action.
                        </text>
                    </view>
                </view>
            </view>

            <view v-if="rows.length === 0" class="empty-state fade-in-up">
                <text>No products found</text>
            </view>

            <view v-else class="catalog-list fade-in-up">
                <view
                    v-for="item in rows"
                    :key="item.id"
                    class="surface-card panel-block panel-hover catalog-row"
                >
                    <view class="row-main">
                        <text class="row-title">{{ item.name }}</text>

                        <view class="meta-inline">
                            <text
                                class="meta-chip"
                                :class="productStatusTone(item.status)"
                            >
                                {{ formatProductStatus(item.status) }}
                            </text>
                            <text class="meta-chip status-muted">
                                {{
                                    typeof item.id === "number"
                                        ? `Product ID: ${item.id}`
                                        : "Product ID: --"
                                }}
                            </text>
                        </view>

                        <view class="summary-grid">
                            <view class="summary-item">
                                <text class="summary-label">Price</text>
                                <text class="summary-value">{{
                                    formatPrice(item.price)
                                }}</text>
                            </view>
                            <view class="summary-item">
                                <text class="summary-label">Status</text>
                                <text class="summary-value">{{
                                    formatProductStatus(item.status)
                                }}</text>
                            </view>
                        </view>
                    </view>

                    <view class="action-wrap row-actions">
                        <button class="btn-outline" @click="toggleStatus(item)">
                            {{ item.status === 1 ? "Unpublish" : "Publish" }}
                        </button>
                    </view>
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.catalog-page {
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

.hint-card {
    gap: 12px;
}

.hint-copy {
    font-size: 14px;
    line-height: 1.8;
    color: var(--text-muted);
}

.catalog-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.catalog-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
}

.row-main {
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    min-width: 260px;
}

.row-title {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.03em;
    color: var(--text-main);
}

.meta-inline {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.meta-chip {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 12px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid var(--panel-border);
    font-size: 12px;
    color: var(--text-muted);
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.summary-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 14px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid var(--panel-border);
}

.summary-label {
    font-size: 12px;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: var(--text-soft);
}

.summary-value {
    font-size: 14px;
    line-height: 1.7;
    color: var(--text-main);
}

.row-actions {
    align-items: center;
}

@media (max-width: 768px) {
    .toolbar-grid,
    .summary-grid {
        grid-template-columns: 1fr;
    }

    .row-actions {
        width: 100%;
    }
}
</style>
