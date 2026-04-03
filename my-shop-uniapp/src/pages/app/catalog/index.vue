<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { watchDebounced } from "@vueuse/core";
import AppShell from "../../../components/AppShell.vue";
import {
    listSearchHotKeywordsWithFallback,
    listSearchKeywordRecommendationsWithFallback,
    smartSearchProductsWithFallback,
} from "../../../api/search-ops";
import type { ProductItem } from "../../../types/domain";
import { addToCart } from "../../../store/cart";
import { formatPrice } from "../../../utils/format";
import { toast } from "../../../utils/ui";
import { useRole } from "../../../auth/permission";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";
import { mapSearchDocumentToProduct, resolveCartSkuId } from "../../../utils/product";

const keyword = ref("");
const loading = ref(false);
const rows = ref<ProductItem[]>([]);
const page = ref(1);
const size = ref(10);
const hasMore = ref(true);
const hotKeywords = ref<string[]>([]);
const recommendations = ref<string[]>([]);
const skuIdCache = new Map<number | string, number | null>();
const skuLookupCache = new Map<number | string, Promise<number | null>>();
const initialized = ref(false);
const latestLoadRequestId = ref(0);

const { isAdmin, isMerchant } = useRole();
const canManage = computed(() => isAdmin.value || isMerchant.value);


async function refreshKeywords(seed = ""): Promise<void> {
    const [hotResult, recResult] = await Promise.allSettled([
        listSearchHotKeywordsWithFallback(8),
        listSearchKeywordRecommendationsWithFallback(seed, 10),
    ]);
    hotKeywords.value = hotResult.status === "fulfilled" ? hotResult.value : [];
    recommendations.value =
        recResult.status === "fulfilled" ? recResult.value : [];
}

async function loadProducts(reset = false): Promise<void> {
    if (loading.value) {
        return;
    }
    const requestId = latestLoadRequestId.value + 1;
    latestLoadRequestId.value = requestId;
    if (reset) {
        page.value = 1;
        hasMore.value = true;
    }
    loading.value = true;
    try {
        const result = await smartSearchProductsWithFallback({
            keyword: keyword.value || undefined,
            page: page.value,
            size: size.value,
            sortField: "score",
            sortOrder: "desc",
        });
        if (requestId !== latestLoadRequestId.value) {
            return;
        }
        const items = result.documents.map(mapSearchDocumentToProduct);
        rows.value = reset ? items : rows.value.concat(items);
        hasMore.value = rows.value.length < result.total;
        await refreshKeywords(keyword.value);
    } catch (error) {
        if (requestId !== latestLoadRequestId.value) {
            return;
        }
        toast(
            error instanceof Error ? error.message : "Failed to load products",
        );
    } finally {
        if (requestId === latestLoadRequestId.value) {
            loading.value = false;
        }
    }
}

function onSearch(): void {
    void loadProducts(true);
}

function onKeywordSelect(value: string): void {
    keyword.value = value;
    void loadProducts(true);
}

function onLoadMore(): void {
    if (!hasMore.value || loading.value) {
        return;
    }
    page.value += 1;
    void loadProducts();
}

async function onAddToCart(item: ProductItem): Promise<void> {
    if (typeof item.price !== "number" || item.price <= 0) {
        toast("Product price is unavailable");
        return;
    }
    if (typeof item.shopId !== "number" || item.shopId <= 0) {
        toast("Shop information is unavailable");
        return;
    }
    if (typeof item.stockQuantity === "number" && item.stockQuantity <= 0) {
        toast("Product is out of stock");
        return;
    }
    try {
        const skuId = await resolveCartSkuId(item, skuIdCache, skuLookupCache);
        if (typeof skuId !== "number") {
            return;
        }
        addToCart({
            productId: item.id,
            skuId,
            productName: item.name,
            price: item.price,
            shopId: item.shopId,
        });
        toast("Added to cart", "success");
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to add product to cart",
        );
    }
}

watchDebounced(
    () => keyword.value.trim(),
    (value) => {
        void refreshKeywords(value);
    },
    { debounce: 250, maxWait: 800 },
);

onShow(() => {
    if (initialized.value) {
        return;
    }
    initialized.value = true;
    void loadProducts(true);
    void refreshKeywords("");
});
</script>

<template>
    <AppShell title="Products">
        <view class="catalog-layout">
            <view class="hero-card display-panel">
                <view class="hero-copy">
                    <text class="hero-eyebrow">Products</text>
                    <text class="hero-title"
                        >A cleaner product catalog built for fast
                        discovery.</text
                    >
                    <text class="hero-subtitle">
                        Search the indexed catalog, explore guided keywords, and
                        move directly into product management when your role
                        allows it.
                    </text>
                </view>

                <view class="hero-actions">
                    <button class="btn-primary" @click="onSearch">
                        Search now
                    </button>
                    <button
                        v-if="canManage"
                        class="btn-outline"
                        @click="
                            navigateTo(Routes.appCatalogManage, undefined, {
                                requiresAuth: true,
                            })
                        "
                    >
                        Manage products
                    </button>
                </view>
            </view>

            <view class="surface-card search-card">
                <view class="section-block compact-block">
                    <text class="section-title">Search the catalog</text>
                    <text class="section-subtitle">
                        Find products, categories, and brands with keyword
                        suggestions.
                    </text>
                </view>

                <view class="search-row">
                    <input
                        v-model="keyword"
                        class="search-input"
                        placeholder="Search products, categories, or brands"
                        @confirm="onSearch"
                    />
                    <button class="btn-primary" @click="onSearch">
                        Search
                    </button>
                </view>

                <view class="keyword-grid">
                    <view class="keyword-block" v-if="hotKeywords.length">
                        <text class="keyword-title">Trending</text>
                        <view class="keyword-list">
                            <text
                                v-for="item in hotKeywords"
                                :key="`hot-${item}`"
                                class="keyword-chip"
                                @click="onKeywordSelect(item)"
                            >
                                {{ item }}
                            </text>
                        </view>
                    </view>

                    <view class="keyword-block" v-if="recommendations.length">
                        <text class="keyword-title">Recommended</text>
                        <view class="keyword-list">
                            <text
                                v-for="item in recommendations"
                                :key="`rec-${item}`"
                                class="keyword-chip"
                                @click="onKeywordSelect(item)"
                            >
                                {{ item }}
                            </text>
                        </view>
                    </view>
                </view>
            </view>

            <view class="section-head">
                <text class="section-title">Catalog results</text>
                <text class="section-subtitle"
                    >Browse the latest search results and add eligible items to
                    the cart.</text
                >
            </view>

            <view v-if="rows.length" class="product-grid">
                <view
                    v-for="item in rows"
                    :key="item.id"
                    class="product-card surface-card"
                >
                    <image
                        v-if="item.imageUrl"
                        :src="item.imageUrl"
                        class="product-image"
                        mode="aspectFill"
                    />
                    <view v-else class="product-image placeholder"
                        >No image</view
                    >
                    <view class="product-main">
                        <text class="product-name">{{ item.name }}</text>
                        <text class="product-price">{{
                            formatPrice(item.price)
                        }}</text>
                        <text class="product-meta"
                            >Stock {{ item.stockQuantity ?? "--" }}</text
                        >
                    </view>
                    <button
                        class="btn-outline action-button"
                        @click="onAddToCart(item)"
                    >
                        Add to cart
                    </button>
                </view>
            </view>

            <view v-else class="empty-state"
                >No products matched the current search conditions.</view
            >

            <view class="load-more">
                <button
                    v-if="hasMore"
                    class="btn-outline"
                    :loading="loading"
                    @click="onLoadMore"
                >
                    Load more
                </button>
                <text v-else class="text-muted">No more products</text>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.catalog-layout {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.hero-card {
    padding: 36px;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24px;
}

.hero-copy {
    display: flex;
    flex-direction: column;
    gap: 18px;
    max-width: 760px;
}

.hero-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.search-card {
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.compact-block {
    gap: 6px;
}

.search-row {
    display: flex;
    gap: 10px;
    align-items: center;
}

.search-input {
    flex: 1;
    min-height: 48px;
    background: #fff;
    border-radius: 999px;
    padding: 12px 16px;
    font-size: 14px;
    border: 1px solid rgba(15, 23, 42, 0.08);
}

.keyword-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.keyword-block {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.keyword-title {
    font-size: 12px;
    color: var(--text-muted);
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
}

.keyword-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.keyword-chip {
    padding: 8px 14px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.95);
    font-size: 12px;
    border: 1px solid rgba(15, 23, 42, 0.06);
}

.section-head {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.product-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.product-card {
    padding: 18px;
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.product-image {
    width: 100%;
    aspect-ratio: 1.7 / 1;
    border-radius: 18px;
    background: linear-gradient(180deg, #f7f7f9, #f1f1f4);
}

.product-image.placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: var(--text-muted);
}

.product-main {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.product-name {
    font-size: 18px;
    font-weight: 700;
    line-height: 1.4;
    letter-spacing: -0.02em;
}

.product-price {
    font-size: 22px;
    font-weight: 700;
    color: var(--text-main);
    letter-spacing: -0.03em;
}

.product-meta {
    font-size: 13px;
    color: var(--text-muted);
}

.action-button {
    width: 100%;
}

.load-more {
    display: flex;
    justify-content: center;
    padding: 4px 0 8px;
}

@media (max-width: 900px) {
    .hero-card,
    .keyword-grid,
    .product-grid {
        grid-template-columns: 1fr;
    }

    .hero-card {
        align-items: flex-start;
        flex-direction: column;
        padding: 26px;
    }
}
</style>
