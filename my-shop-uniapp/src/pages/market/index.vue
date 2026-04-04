<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { watchDebounced } from "@vueuse/core";
import {
    listSearchHotKeywordsWithFallback,
    listSearchKeywordRecommendationsWithFallback,
    listTodayHotSellingProductsWithFallback,
    smartSearchProductsWithFallback,
} from "../../api/search-ops";
import type { ProductItem } from "../../types/domain";
import { addToCart } from "../../store/cart";
import { isAuthenticated } from "../../auth/session";
import { navigateTo } from "../../router/navigation";
import { Routes } from "../../router/routes";
import { formatPrice } from "../../utils/format";
import { toast } from "../../utils/ui";
import { mapSearchDocumentToProduct, resolveCartSkuId } from "../../utils/product";

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

const loggedIn = computed(() => isAuthenticated());
const activeKeyword = computed(() => keyword.value.trim());
const resultsTitle = computed(() =>
    activeKeyword.value ? "Search Results" : "Today Hot Selling",
);
const resultsHint = computed(() =>
    activeKeyword.value
        ? `Keyword: ${activeKeyword.value}`
        : "Ranked by completed sales today",
);


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
        let result = activeKeyword.value
            ? await smartSearchProductsWithFallback({
                  keyword: activeKeyword.value,
                  page: page.value,
                  size: size.value,
                  sortField: "score",
                  sortOrder: "desc",
              })
            : await listTodayHotSellingProductsWithFallback(
                  page.value,
                  size.value,
              );

        if (!activeKeyword.value && reset && result.documents.length === 0) {
            await refreshKeywords("");
            const fallbackKeyword =
                hotKeywords.value[0] || recommendations.value[0] || "";
            if (fallbackKeyword) {
                keyword.value = fallbackKeyword;
                result = await smartSearchProductsWithFallback({
                    keyword: fallbackKeyword,
                    page: 1,
                    size: size.value,
                    sortField: "score",
                    sortOrder: "desc",
                });
            }
        }

        if (requestId !== latestLoadRequestId.value) {
            return;
        }
        const items = result.documents.map(mapSearchDocumentToProduct);
        rows.value = reset ? items : rows.value.concat(items);
        hasMore.value = rows.value.length < result.total;
        await refreshKeywords(activeKeyword.value);
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
    if (!loggedIn.value) {
        toast("Sign in to add items to cart");
        goLogin();
        return;
    }
    if (typeof item.price !== "number" || item.price <= 0) {
        toast("Product price is unavailable");
        return;
    }
    if (typeof item.shopId !== "number" || item.shopId <= 0) {
        toast("Shop information is unavailable");
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
        if (typeof item.stockQuantity === "number" && item.stockQuantity <= 0) {
            toast("Added to cart. Search stock data may be stale.", "success");
            return;
        }
        toast("Added to cart", "success");
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to add product to cart",
        );
    }
}

function goLogin(): void {
    navigateTo(Routes.login, { redirect: Routes.appHome });
}

watchDebounced(
    () => activeKeyword.value,
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
    <view class="page">
        <view class="page-container market-layout">
            <view class="hero-panel display-panel fade-in-up">
                <view class="hero-main">
                    <text class="hero-eyebrow">Marketplace</text>
                    <text class="hero-title"
                        >Discover products in a calmer, more premium
                        storefront.</text
                    >
                    <text class="hero-subtitle">
                        Explore what is selling today, search by keyword, and
                        move from inspiration to checkout with a cleaner H5
                        experience.
                    </text>

                    <view class="hero-actions">
                        <button
                            v-if="!loggedIn"
                            class="btn-primary"
                            @click="goLogin"
                        >
                            Sign in
                        </button>
                        <button
                            v-else
                            class="btn-outline"
                            @click="
                                navigateTo(Routes.appHome, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            Open dashboard
                        </button>
                        <button
                            class="btn-secondary"
                            @click="navigateTo(Routes.appCatalog)"
                        >
                            Browse catalog
                        </button>
                    </view>
                </view>

                <view class="hero-stats">
                    <view class="info-card">
                        <text class="info-label">Current view</text>
                        <text class="info-value">{{
                            activeKeyword ? "Search" : "Hot selling"
                        }}</text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">Suggestions</text>
                        <text class="info-value">{{
                            hotKeywords.length + recommendations.length
                        }}</text>
                    </view>
                </view>
            </view>

            <view class="search-panel surface-card fade-in-up">
                <view class="section-block compact-block">
                    <text class="section-title">Search the marketplace</text>
                    <text class="section-subtitle">
                        Find products, categories, and brands with keyword
                        guidance.
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
                    <view class="keyword-section" v-if="hotKeywords.length">
                        <text class="keyword-title">Trending now</text>
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

                    <view class="keyword-section" v-if="recommendations.length">
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

            <view class="section-head fade-in-up">
                <text class="section-title">{{ resultsTitle }}</text>
                <text class="section-subtitle">{{ resultsHint }}</text>
            </view>

            <view v-if="rows.length" class="product-grid fade-in-up">
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
                        class="btn-outline card-action"
                        @click="onAddToCart(item)"
                    >
                        {{ loggedIn ? "Add to cart" : "Sign in to add" }}
                    </button>
                </view>
            </view>

            <view v-else class="empty-state"
                >No products matched the current query.</view
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
    </view>
</template>

<style scoped>
.page {
    padding: 24px 0 40px;
}

.market-layout {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.hero-panel {
    padding: 36px;
    display: grid;
    grid-template-columns: minmax(0, 1.45fr) 300px;
    gap: 24px;
    align-items: stretch;
}

.hero-main {
    display: flex;
    flex-direction: column;
    gap: 18px;
    justify-content: center;
    min-height: 420px;
}

.hero-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding-top: 8px;
}

.hero-stats {
    display: flex;
    flex-direction: column;
    gap: 14px;
    justify-content: flex-end;
}

.search-panel {
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
    border: 1px solid rgba(20, 20, 20, 0.12);
}

.search-input:focus {
    border-color: rgba(11, 107, 95, 0.4);
    box-shadow: 0 0 0 3px rgba(11, 107, 95, 0.12);
}

.keyword-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.keyword-section {
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
    border: 1px solid rgba(20, 20, 20, 0.08);
    color: var(--text-main);
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease,
        color 0.2s ease;
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
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

.product-image {
    width: 100%;
    aspect-ratio: 1.7 / 1;
    border-radius: 18px;
    background: linear-gradient(180deg, #f7f7f9, #f1f1f4);
    border: 1px solid rgba(20, 20, 20, 0.08);
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

.card-action {
    width: 100%;
}

.load-more {
    display: flex;
    justify-content: center;
    padding: 4px 0 24px;
}

@media (hover: hover) {
    .keyword-chip:hover {
        transform: translateY(-1px);
        border-color: rgba(11, 107, 95, 0.2);
        box-shadow: 0 10px 20px rgba(20, 20, 20, 0.08);
        color: var(--accent);
    }

    .product-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 16px 30px rgba(20, 20, 20, 0.12);
        border-color: rgba(20, 20, 20, 0.12);
    }
}

@media (max-width: 900px) {
    .hero-panel,
    .keyword-grid,
    .product-grid {
        grid-template-columns: 1fr;
    }

    .hero-panel {
        padding: 26px;
    }

    .hero-main {
        min-height: auto;
    }
}
</style>
