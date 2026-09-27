<script setup lang="ts">
import { computed } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { watchDebounced } from "@vueuse/core";
import AppShell from "../../../components/AppShell.vue";
import {
    smartSearchProductsWithFallback,
} from "../../../api/search-ops";
import { isAuthenticated } from "../../../auth/session";
import { useRole } from "../../../auth/permission";
import { useProductSearchFeed } from "../../../composables/useProductSearchFeed";
import { useLocale } from "../../../i18n/locale";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";
import { addToCart } from "../../../store/cart";
import type { EntityId, ProductItem } from "../../../types/domain";
import { formatPrice } from "../../../utils/format";
import { resolveProductImageUrl } from "../../../utils/image";
import {
    mapSearchDocumentToProduct,
    resolveCartSkuId,
} from "../../../utils/product";
import { toast } from "../../../utils/ui";

const skuIdCache = new Map<EntityId, EntityId | null>();
const skuLookupCache = new Map<EntityId, Promise<EntityId | null>>();

const { isAdmin, isMerchant } = useRole();
const { locale } = useLocale();
const canManage = computed(() => isAdmin.value || isMerchant.value);
const {
    failedImageIds,
    hasMore,
    hotKeywords,
    initialize,
    keyword,
    loading,
    markImageFailed,
    onKeywordSelect,
    onLoadMore,
    onSearch,
    recommendations,
    refreshKeywords,
    rows,
} = useProductSearchFeed({
    async loadPage({ keyword, page, size }) {
        const result = await smartSearchProductsWithFallback({
            keyword: keyword || undefined,
            page,
            size,
            sortField: "hotScore",
            sortOrder: "desc",
        });
        return {
            items: result.documents.map(mapSearchDocumentToProduct),
            total: result.total,
        };
    },
    onLoadError(error) {
        toast(error instanceof Error ? error.message : copy.value.loadFailed);
    },
});

const copy = computed(() =>
    locale.value === "en-US"
        ? {
              pageTitle: "Catalog",
              eyebrow: "Catalog",
              heroTitle: "Search the product index with less interruption.",
              heroSubtitle:
                  "Use recommendation signals to locate products, categories, and brands, then jump into catalog operations when your role allows it.",
              primaryAction: "Search now",
              manageAction: "Manage catalog",
              searchTitle: "Search the catalog",
              searchSubtitle:
                  "Start from keywords and narrow quickly with the recommendation feed.",
              searchPlaceholder: "Search by product, category, or brand",
              searchAction: "Search",
              hotKeywords: "Popular searches",
              recommendedKeywords: "Suggested keywords",
              resultTitle: "Catalog results",
              resultSubtitle:
                  "Review the latest matches and add purchasable items into the cart flow.",
              stockPrefix: "Stock",
              addToCart: "Add to cart",
              empty: "No products matched the current search conditions.",
              loadMore: "Load more",
              noMore: "No more products",
              invalidPrice: "Product price is unavailable.",
              invalidShop: "Shop metadata is unavailable.",
              outOfStock: "This product is out of stock.",
              authHint: "Please sign in before adding items to cart.",
              addSuccess: "Added to cart",
              addFailed: "Failed to add item to cart",
              loadFailed: "Failed to load products",
          }
        : {
              pageTitle: "商品",
              eyebrow: "商品",
              heroTitle: "更适合快速发现商品的目录浏览体验。",
              heroSubtitle:
                  "结合推荐关键词快速定位商品、类目和品牌，并在权限允许时直接进入商品管理动作。",
              primaryAction: "立即搜索",
              manageAction: "管理商品",
              searchTitle: "搜索商品目录",
              searchSubtitle:
                  "从关键词开始，并通过推荐词流快速缩小范围。",
              searchPlaceholder: "搜索商品、类目或品牌",
              searchAction: "搜索",
              hotKeywords: "热门搜索",
              recommendedKeywords: "推荐关键词",
              resultTitle: "商品结果",
              resultSubtitle:
                  "查看最新搜索结果，并把可购买商品加入购物车。",
              stockPrefix: "库存",
              addToCart: "加入购物车",
              empty: "当前搜索条件下暂无匹配商品。",
              loadMore: "加载更多",
              noMore: "没有更多商品了",
              invalidPrice: "商品价格不可用。",
              invalidShop: "店铺信息不可用。",
              outOfStock: "商品库存不足。",
              authHint: "请先登录后再加入购物车。",
              addSuccess: "已加入购物车",
              addFailed: "加入购物车失败",
              loadFailed: "加载商品失败",
          },
);

function productImageSrc(item: ProductItem): string {
    return resolveProductImageUrl(
        item.imageUrl,
        item.name,
        !!failedImageIds.value[String(item.id)],
    );
}

function stockTone(stock?: number): string {
    if (typeof stock !== "number") return "status-muted";
    if (stock <= 0) return "status-danger";
    if (stock <= 10) return "status-warning";
    return "status-success";
}

async function onAddToCart(item: ProductItem): Promise<void> {
    if (!isAuthenticated()) {
        toast(copy.value.authHint || "Please sign in before adding items to cart.");
        navigateTo(Routes.login, { redirect: Routes.appCatalog });
        return;
    }
    if (typeof item.price !== "number" || item.price <= 0) {
        toast(copy.value.invalidPrice);
        return;
    }
    if (typeof item.shopId !== "string" || !item.shopId) {
        toast(copy.value.invalidShop);
        return;
    }
    if (typeof item.stockQuantity === "number" && item.stockQuantity <= 0) {
        toast(copy.value.outOfStock);
        return;
    }
    try {
        const skuId = await resolveCartSkuId(item, skuIdCache, skuLookupCache);
        if (typeof skuId !== "string") {
            return;
        }
        addToCart({
            productId: item.id,
            skuId,
            productName: item.name,
            price: item.price,
            shopId: item.shopId,
        });
        toast(copy.value.addSuccess, "success");
    } catch (error) {
        toast(error instanceof Error ? error.message : copy.value.addFailed);
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
    initialize();
});
</script>

<template>
    <AppShell :title="copy.pageTitle">
        <view class="catalog-layout">
            <view class="display-panel dashboard-hero fade-in-up">
                <view class="dashboard-hero-copy">
                    <text class="hero-eyebrow">{{ copy.eyebrow }}</text>
                    <text class="hero-title">{{ copy.heroTitle }}</text>
                    <text class="hero-subtitle">{{ copy.heroSubtitle }}</text>

                    <view class="action-wrap">
                        <button class="btn-primary" @click="onSearch">
                            {{ copy.primaryAction }}
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
                            {{ copy.manageAction }}
                        </button>
                    </view>
                </view>

                <view class="dashboard-hero-stats">
                    <view class="metric-card">
                        <text class="metric-label">{{
                            copy.recommendedKeywords
                        }}</text>
                        <text class="metric-value">
                            {{ recommendations.length }}
                        </text>
                    </view>
                    <view class="metric-card">
                        <text class="metric-label">{{ copy.hotKeywords }}</text>
                        <text class="metric-value">{{ hotKeywords.length }}</text>
                    </view>
                </view>
            </view>

            <view class="dashboard-grid-main fade-in-up">
                <view class="surface-card panel-block search-card">
                    <view class="section-block compact-block">
                        <text class="section-title">{{ copy.searchTitle }}</text>
                        <text class="section-subtitle">{{
                            copy.searchSubtitle
                        }}</text>
                    </view>

                    <view class="search-row">
                        <input
                            v-model="keyword"
                            class="field-control field-control-pill"
                            type="text"
                            name="catalog-search"
                            confirm-type="search"
                            :placeholder="copy.searchPlaceholder"
                            @confirm="onSearch"
                        />
                        <button class="btn-primary" @click="onSearch">
                            {{ copy.searchAction }}
                        </button>
                    </view>

                    <view class="keyword-grid">
                        <view class="keyword-block" v-if="hotKeywords.length">
                            <text class="keyword-title">{{
                                copy.hotKeywords
                            }}</text>
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

                        <view
                            class="keyword-block"
                            v-if="recommendations.length"
                        >
                            <text class="keyword-title">
                                {{ copy.recommendedKeywords }}
                            </text>
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

                <view class="surface-card panel-block sticky-side">
                    <view class="section-head">
                        <text class="section-title">{{ copy.resultTitle }}</text>
                        <text class="section-subtitle">{{
                            copy.resultSubtitle
                        }}</text>
                    </view>
                </view>
            </view>

            <view v-if="rows.length" class="product-grid fade-in-up">
                <view
                    v-for="item in rows"
                    :key="item.id"
                    class="product-card surface-card"
                >
                    <image
                        :src="productImageSrc(item)"
                        :alt="item.name"
                        class="product-image"
                        mode="aspectFit"
                        @error="markImageFailed(item.id)"
                    />
                    <view class="product-main">
                        <text class="product-name">{{ item.name }}</text>
                        <text class="product-price">{{ formatPrice(item.price) }}</text>
                        <view class="meta-inline">
                            <text
                                class="meta-chip"
                                :class="stockTone(item.stockQuantity)"
                            >
                                {{ copy.stockPrefix }}
                                {{ item.stockQuantity ?? "--" }}
                            </text>
                        </view>
                    </view>
                    <button
                        class="btn-outline action-button"
                        @click="onAddToCart(item)"
                    >
                        {{ copy.addToCart }}
                    </button>
                </view>
            </view>

            <view v-else class="empty-state">{{ copy.empty }}</view>

            <view class="load-more">
                <button
                    v-if="hasMore"
                    class="btn-outline"
                    :loading="loading"
                    @click="onLoadMore"
                >
                    {{ copy.loadMore }}
                </button>
                <text v-else class="text-muted">{{ copy.noMore }}</text>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.catalog-layout {
    display: flex;
    flex-direction: column;
    gap: 22px;
}

.catalog-layout :deep(.dashboard-hero) {
    padding: 46px 58px;
}

.catalog-layout :deep(.dashboard-hero-copy) {
    min-height: 280px;
}

.catalog-layout :deep(.hero-title) {
    max-width: 760px;
    font-size: 42px;
    line-height: 1.18;
}

.catalog-layout :deep(.hero-subtitle) {
    max-width: 680px;
}

.compact-block {
    gap: 6px;
}

.section-head {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.search-row {
    display: flex;
    gap: 10px;
    align-items: center;
}

.search-row .btn-primary {
    min-width: 96px;
    white-space: nowrap;
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
    color: #848c96;
    font-weight: 800;
    letter-spacing: 0;
    text-transform: uppercase;
}

.keyword-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.keyword-chip {
    min-height: 30px;
    padding: 0 12px;
    border-radius: 4px;
    background: #ffffff;
    font-size: 12px;
    border: 1px solid #e7ebf0;
    color: #4b535d;
    transition:
        transform 0.22s ease,
        box-shadow 0.22s ease,
        border-color 0.22s ease,
        color 0.22s ease;
}

.product-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.product-card {
    overflow: hidden;
    padding: 0;
    display: flex;
    flex-direction: column;
    min-width: 0;
    transition:
        transform 0.22s ease,
        box-shadow 0.22s ease,
        border-color 0.22s ease;
}

.product-image {
    width: 100%;
    height: 176px;
    padding: 18px;
    background: #f7f8fa;
    border: 0;
    border-radius: 0;
}

.product-main {
    display: flex;
    flex-direction: column;
    gap: 7px;
    padding: 14px 14px 12px;
    min-width: 0;
}

.product-name {
    min-height: 40px;
    color: #23272d;
    font-size: 13px;
    font-weight: 500;
    line-height: 1.55;
    letter-spacing: 0;
    overflow-wrap: anywhere;
}

.product-price {
    font-size: 17px;
    font-weight: 900;
    color: #ff3b30;
    letter-spacing: 0;
}

.action-button {
    height: 38px;
    width: auto;
    margin: auto 14px 14px;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
}

.load-more {
    display: flex;
    justify-content: center;
    padding: 4px 0 8px;
}

@media (hover: hover) {
    .keyword-chip:hover {
        transform: translateY(-1px);
        border-color: #dfe5ec;
        box-shadow: 0 12px 24px rgba(18, 24, 35, 0.06);
        color: #ff3b30;
    }

    .product-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 24px 70px rgba(18, 24, 35, 0.09);
        border-color: #e2e6ec;
    }
}

@media (max-width: 1180px) {
    .product-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (max-width: 900px) {
    .keyword-grid,
    .product-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .search-row {
        flex-direction: column;
        align-items: stretch;
    }
}

@media (max-width: 520px) {
    .keyword-grid,
    .product-grid {
        grid-template-columns: minmax(0, 1fr);
    }

    .product-image {
        height: 200px;
    }
}
</style>
