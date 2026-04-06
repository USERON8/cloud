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
import { isAuthenticated } from "../../auth/session";
import LocaleSwitch from "../../components/LocaleSwitch.vue";
import { useLocale } from "../../i18n/locale";
import { navigateTo } from "../../router/navigation";
import { Routes } from "../../router/routes";
import { addToCart } from "../../store/cart";
import type { ProductItem } from "../../types/domain";
import { formatPrice } from "../../utils/format";
import { resolveProductImageUrl } from "../../utils/image";
import { mapSearchDocumentToProduct, resolveCartSkuId } from "../../utils/product";
import { toast } from "../../utils/ui";

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
const failedImageIds = ref<Record<string, boolean>>({});

const { locale } = useLocale();

const loggedIn = computed(() => isAuthenticated());
const activeKeyword = computed(() => keyword.value.trim());

const copy = computed(() =>
    locale.value === "en-US"
        ? {
              eyebrow: "Storefront",
              title: "A calmer product browser for cloud commerce.",
              subtitle:
                  "Search hot items, move through curated discovery, and keep the shopping path readable from first glance to checkout.",
              login: "Sign in now",
              workspace: "Open console",
              catalog: "Browse catalog",
              viewMode: "View mode",
              searchMode: "Search mode",
              hotMode: "Hot picks",
              recommendedCount: "Keyword signals",
              searchTitle: "Search the storefront",
              searchSubtitle:
                  "Use keywords to jump to products, categories, and brands.",
              searchPlaceholder: "Search by product, category, or brand",
              searchAction: "Search",
              hotKeywords: "Trending now",
              recommendedKeywords: "Suggested keywords",
              resultSearch: "Search results",
              resultToday: "Today hot picks",
              resultHintSearch: `Keyword: ${activeKeyword.value}`,
              resultHintToday: "Sorted by today relevance and recent demand.",
              stockPrefix: "Stock",
              addToCart: "Add to cart",
              addToCartLogin: "Login to add",
              empty: "No matching products for the current condition.",
              loadMore: "Load more",
              noMore: "No more products",
              authHint: "Please sign in before adding items to cart.",
              invalidPrice: "Product price is unavailable.",
              invalidShop: "Shop metadata is unavailable.",
              addCartSuccess: "Added to cart",
              delayedStock:
                  "Added to cart. Stock data might still be catching up.",
              loadFailed: "Failed to load products",
          }
        : {
              eyebrow: "商城",
              title: "更安静，也更聚焦的云端商品浏览界面。",
              subtitle:
                  "把热销商品、搜索入口和加购动作收进更清晰的结构里，让从发现到下单的路径更顺手。",
              login: "立即登录",
              workspace: "进入工作台",
              catalog: "浏览商品",
              viewMode: "当前视图",
              searchMode: "搜索模式",
              hotMode: "热销榜",
              recommendedCount: "关键词信号",
              searchTitle: "搜索商城",
              searchSubtitle: "通过关键词快速定位商品、类目和品牌。",
              searchPlaceholder: "搜索商品、类目或品牌",
              searchAction: "搜索",
              hotKeywords: "当前热搜",
              recommendedKeywords: "推荐关键词",
              resultSearch: "搜索结果",
              resultToday: "今日热销",
              resultHintSearch: `关键词：${activeKeyword.value}`,
              resultHintToday: "按今日热度与近时段成交关注度排序。",
              stockPrefix: "库存",
              addToCart: "加入购物车",
              addToCartLogin: "登录后加入",
              empty: "当前条件下暂无匹配商品。",
              loadMore: "加载更多",
              noMore: "没有更多商品了",
              authHint: "请先登录后再加入购物车。",
              invalidPrice: "商品价格不可用。",
              invalidShop: "店铺信息不可用。",
              addCartSuccess: "已加入购物车",
              delayedStock: "已加入购物车，当前库存数据可能存在延迟。",
              loadFailed: "加载商品失败",
          },
);

const resultsTitle = computed(() =>
    activeKeyword.value ? copy.value.resultSearch : copy.value.resultToday,
);

const resultsHint = computed(() =>
    activeKeyword.value ? copy.value.resultHintSearch : copy.value.resultHintToday,
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
            : await listTodayHotSellingProductsWithFallback(page.value, size.value);

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
        if (reset) {
            failedImageIds.value = {};
        }
        rows.value = reset ? items : rows.value.concat(items);
        hasMore.value = rows.value.length < result.total;
        await refreshKeywords(activeKeyword.value);
    } catch (error) {
        if (requestId !== latestLoadRequestId.value) {
            return;
        }
        toast(error instanceof Error ? error.message : copy.value.loadFailed);
    } finally {
        if (requestId === latestLoadRequestId.value) {
            loading.value = false;
        }
    }
}

function productImageSrc(item: ProductItem): string {
    return resolveProductImageUrl(
        item.imageUrl,
        item.name,
        !!failedImageIds.value[String(item.id)],
    );
}

function markImageFailed(id: number | string): void {
    failedImageIds.value = {
        ...failedImageIds.value,
        [String(id)]: true,
    };
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
        toast(copy.value.authHint);
        goLogin();
        return;
    }
    if (typeof item.price !== "number" || item.price <= 0) {
        toast(copy.value.invalidPrice);
        return;
    }
    if (typeof item.shopId !== "number" || item.shopId <= 0) {
        toast(copy.value.invalidShop);
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
            toast(copy.value.delayedStock, "success");
            return;
        }
        toast(copy.value.addCartSuccess, "success");
    } catch (error) {
        toast(error instanceof Error ? error.message : copy.value.loadFailed);
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
            <view class="market-topbar fade-in-up">
                <view class="market-brand">
                    <text class="market-brand-mark">MS</text>
                    <text class="market-brand-name">My Shop Cloud</text>
                </view>
                <LocaleSwitch />
            </view>

            <view class="hero-panel display-panel fade-in-up">
                <view class="hero-main">
                    <text class="hero-eyebrow">{{ copy.eyebrow }}</text>
                    <text class="hero-title">{{ copy.title }}</text>
                    <text class="hero-subtitle">{{ copy.subtitle }}</text>

                    <view class="hero-actions">
                        <button
                            v-if="!loggedIn"
                            class="btn-primary"
                            @click="goLogin"
                        >
                            {{ copy.login }}
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
                            {{ copy.workspace }}
                        </button>
                        <button
                            class="btn-secondary"
                            @click="navigateTo(Routes.appCatalog)"
                        >
                            {{ copy.catalog }}
                        </button>
                    </view>
                </view>

                <view class="hero-stats">
                    <view class="info-card">
                        <text class="info-label">{{ copy.viewMode }}</text>
                        <text class="info-value">
                            {{ activeKeyword ? copy.searchMode : copy.hotMode }}
                        </text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">{{ copy.recommendedCount }}</text>
                        <text class="info-value">
                            {{ hotKeywords.length + recommendations.length }}
                        </text>
                    </view>
                </view>
            </view>

            <view class="search-panel surface-card fade-in-up">
                <view class="section-block compact-block">
                    <text class="section-title">{{ copy.searchTitle }}</text>
                    <text class="section-subtitle">{{ copy.searchSubtitle }}</text>
                </view>

                <view class="search-row">
                    <input
                        v-model="keyword"
                        class="search-input"
                        :placeholder="copy.searchPlaceholder"
                        @confirm="onSearch"
                    />
                    <button class="btn-primary" @click="onSearch">
                        {{ copy.searchAction }}
                    </button>
                </view>

                <view class="keyword-grid">
                    <view class="keyword-section" v-if="hotKeywords.length">
                        <text class="keyword-title">{{ copy.hotKeywords }}</text>
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
                        :src="productImageSrc(item)"
                        class="product-image"
                        mode="aspectFill"
                        @error="markImageFailed(item.id)"
                    />

                    <view class="product-main">
                        <text class="product-name">{{ item.name }}</text>
                        <text class="product-price">{{ formatPrice(item.price) }}</text>
                        <text class="product-meta">
                            {{ copy.stockPrefix }} {{ item.stockQuantity ?? "--" }}
                        </text>
                    </view>

                    <button
                        class="btn-outline card-action"
                        @click="onAddToCart(item)"
                    >
                        {{ loggedIn ? copy.addToCart : copy.addToCartLogin }}
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

.market-topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    flex-wrap: wrap;
}

.market-brand {
    display: inline-flex;
    align-items: center;
    gap: 12px;
}

.market-brand-mark {
    width: 42px;
    height: 42px;
    border-radius: 14px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--accent), var(--highlight));
    color: #04111c;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.12em;
}

.market-brand-name {
    font-size: 12px;
    letter-spacing: 0.18em;
    text-transform: uppercase;
    color: var(--text-muted);
    font-weight: 800;
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
    background: rgba(255, 255, 255, 0.04);
    border-radius: 999px;
    padding: 12px 16px;
    font-size: 14px;
    border: 1px solid var(--panel-border);
}

.search-input:focus {
    border-color: rgba(95, 209, 194, 0.4);
    box-shadow: 0 0 0 3px rgba(95, 209, 194, 0.12);
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
    font-weight: 800;
    letter-spacing: 0.08em;
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
    background: rgba(255, 255, 255, 0.04);
    font-size: 12px;
    border: 1px solid var(--panel-border);
    color: var(--text-main);
    transition:
        transform 0.22s ease,
        box-shadow 0.22s ease,
        border-color 0.22s ease,
        color 0.22s ease;
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
        transform 0.22s ease,
        box-shadow 0.22s ease,
        border-color 0.22s ease;
}

.product-image {
    width: 100%;
    aspect-ratio: 1.7 / 1;
    border-radius: 18px;
    background: linear-gradient(180deg, #0f2033, #0b1828);
    border: 1px solid var(--panel-border);
}

.product-main {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.product-name {
    font-size: 18px;
    font-weight: 800;
    line-height: 1.4;
    letter-spacing: -0.03em;
}

.product-price {
    font-size: 22px;
    font-weight: 800;
    color: var(--text-main);
    letter-spacing: -0.04em;
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
        border-color: rgba(95, 209, 194, 0.2);
        box-shadow: 0 12px 22px rgba(1, 7, 14, 0.28);
        color: var(--accent-strong);
    }

    .product-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 18px 34px rgba(1, 7, 14, 0.34);
        border-color: var(--panel-border-strong);
    }
}

@media (max-width: 980px) {
    .hero-panel,
    .keyword-grid,
    .product-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .hero-panel {
        padding: 24px;
    }

    .hero-main {
        min-height: auto;
    }

    .search-row {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
