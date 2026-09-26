<script setup lang="ts">
import { computed } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { watchDebounced } from "@vueuse/core";
import {
    listTodayHotSellingProductsWithFallback,
    smartSearchProductsWithFallback,
} from "../../api/search-ops";
import { isAuthenticated } from "../../auth/session";
import LocaleSwitch from "../../components/LocaleSwitch.vue";
import { useProductSearchFeed } from "../../composables/useProductSearchFeed";
import { useLocale } from "../../i18n/locale";
import { navigateTo } from "../../router/navigation";
import { Routes } from "../../router/routes";
import { addToCart, cartCount } from "../../store/cart";
import type { ProductItem } from "../../types/domain";
import { formatPrice } from "../../utils/format";
import { resolveProductImageUrl } from "../../utils/image";
import { mapSearchDocumentToProduct, resolveCartSkuId } from "../../utils/product";
import { toast } from "../../utils/ui";

const skuIdCache = new Map<number | string, number | null>();
const skuLookupCache = new Map<number | string, Promise<number | null>>();
const { locale } = useLocale();

const loggedIn = computed(() => isAuthenticated());
const {
    activeKeyword,
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
    async loadPage({
        keyword,
        page,
        size,
        reset,
        hotKeywords,
        recommendations,
        setKeyword,
        refreshKeywords,
    }) {
        let result = keyword
            ? await smartSearchProductsWithFallback({
                  keyword,
                  page,
                  size,
                  sortField: "hotScore",
                  sortOrder: "desc",
              })
            : await listTodayHotSellingProductsWithFallback(page, size);

        if (!keyword && reset && result.documents.length === 0) {
            await refreshKeywords("");
            const fallbackKeyword =
                hotKeywords[0] || recommendations[0] || "";
            if (fallbackKeyword) {
                setKeyword(fallbackKeyword);
                result = await smartSearchProductsWithFallback({
                    keyword: fallbackKeyword,
                    page: 1,
                    size,
                    sortField: "hotScore",
                    sortOrder: "desc",
                });
            }
        }

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
              brand: "ShopMall",
              nav: ["Home", "Category", "New", "Hot", "Brands", "Deals"],
              searchPlaceholder: "Search products, brands, or categories",
              login: "Sign in",
              profile: "Account",
              favorite: "Wishlist",
              cart: "Cart",
              categoryAll: "All categories",
              categories: [
                  "Phones",
                  "Computers",
                  "Smart wearables",
                  "Beauty",
                  "Home appliances",
                  "Food",
                  "Sports",
                  "Mother and baby",
                  "Office",
                  "More categories",
              ],
              heroTitle: "Light shopping, better sound",
              heroSubtitle: "Popular digital products and daily goods are ready.",
              heroAction: "Shop now",
              service: [
                  ["Authentic guarantee", "100% official quality"],
                  ["Fast delivery", "Multi-warehouse express"],
                  ["7-day return", "Worry-free after-sales"],
                  ["Member perks", "More coupons and points"],
              ],
              hotTitle: "Hot recommendations",
              viewAll: "View all",
              searchAction: "Search",
              trending: "Trending searches",
              suggested: "Suggested",
              resultSearch: "Search results",
              resultToday: "Hot products today",
              resultHintSearch: `Keyword: ${activeKeyword.value}`,
              resultHintToday: "Sorted by current demand and sales signals.",
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
              brand: "ShopMall",
              nav: ["首页", "分类", "新品", "热销", "品牌", "优惠"],
              searchPlaceholder: "搜索商品、品牌或分类",
              login: "登录",
              profile: "个人中心",
              favorite: "收藏",
              cart: "购物车",
              categoryAll: "全部分类",
              categories: [
                  "手机数码",
                  "电脑办公",
                  "智能穿戴",
                  "美妆个护",
                  "家用电器",
                  "食品生鲜",
                  "运动户外",
                  "母婴用品",
                  "办公文具",
                  "更多分类",
              ],
              heroTitle: "轻盈随行 静享好音质",
              heroSubtitle: "真实商品数据驱动首页推荐，从发现到加购更清晰。",
              heroAction: "立即购买",
              service: [
                  ["正品保障", "100% 正品保障"],
                  ["极速配送", "多仓直发 极速送达"],
                  ["7天无理由退换", "购物无忧 放心买"],
                  ["会员专享", "享受更多会员权益"],
              ],
              hotTitle: "热门推荐",
              viewAll: "查看全部",
              searchAction: "搜索",
              trending: "当前热搜",
              suggested: "推荐关键词",
              resultSearch: "搜索结果",
              resultToday: "今日热销",
              resultHintSearch: `关键词：${activeKeyword.value}`,
              resultHintToday: "按当前热度、销量和推荐信号排序。",
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

const featuredProducts = computed(() => rows.value.slice(0, 5));
const heroProduct = computed(() => rows.value[0]);
const trendKeywords = computed(() =>
    [...hotKeywords.value, ...recommendations.value].slice(0, 8),
);

const resultsTitle = computed(() =>
    activeKeyword.value ? copy.value.resultSearch : copy.value.resultToday,
);

const resultsHint = computed(() =>
    activeKeyword.value ? copy.value.resultHintSearch : copy.value.resultHintToday,
);

function productImageSrc(item?: ProductItem): string {
    return resolveProductImageUrl(
        item?.imageUrl,
        item?.name,
        item ? !!failedImageIds.value[String(item.id)] : false,
    );
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
    navigateTo(Routes.login, { redirect: Routes.market });
}

function goProfile(): void {
    navigateTo(Routes.appProfile, undefined, { requiresAuth: true });
}

function goCart(): void {
    navigateTo(Routes.appCart, undefined, { requiresAuth: true });
}

function goCatalog(): void {
    navigateTo(Routes.appCatalog);
}

watchDebounced(
    () => activeKeyword.value,
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
    <view class="mall-page">
        <view class="mall-shell">
            <view class="mall-header">
                <view class="brand" @click="goCatalog">
                    <text class="brand-name">{{ copy.brand }}</text>
                </view>

                <scroll-view class="nav-scroll" scroll-x>
                    <view class="nav-list">
                        <button
                            v-for="(item, index) in copy.nav"
                            :key="item"
                            class="nav-link"
                            :class="{ active: index === 0 }"
                            @click="index === 1 ? goCatalog() : undefined"
                        >
                            {{ item }}
                        </button>
                    </view>
                </scroll-view>

                <view class="search-mini">
                    <input
                        v-model="keyword"
                        class="search-mini-input"
                        type="text"
                        name="market-search"
                        confirm-type="search"
                        :placeholder="copy.searchPlaceholder"
                        @confirm="onSearch"
                    />
                    <button class="icon-button" aria-label="Search" @click="onSearch">
                        <view class="search-icon" aria-hidden="true" />
                    </button>
                </view>

                <view class="header-actions">
                    <button
                        class="header-action"
                        @click="loggedIn ? goProfile() : goLogin()"
                    >
                        <view class="action-icon user-icon" aria-hidden="true" />
                        <text>{{ loggedIn ? copy.profile : copy.login }}</text>
                    </button>
                    <button class="header-action">
                        <text class="action-icon">♡</text>
                        <text>{{ copy.favorite }}</text>
                    </button>
                    <button class="header-action cart-action" @click="goCart">
                        <text class="action-icon">🛒</text>
                        <text>{{ copy.cart }}</text>
                        <text v-if="cartCount > 0" class="cart-badge">
                            {{ cartCount }}
                        </text>
                    </button>
                    <LocaleSwitch class="locale-compact" />
                </view>
            </view>

            <view class="hero-grid">
                <view class="category-panel">
                    <view class="category-title">
                        <view class="menu-icon" aria-hidden="true">
                            <view class="menu-bar" />
                            <view class="menu-bar" />
                            <view class="menu-bar" />
                        </view>
                        <text>{{ copy.categoryAll }}</text>
                    </view>
                    <button
                        v-for="item in copy.categories"
                        :key="item"
                        class="category-row"
                        @click="onKeywordSelect(item)"
                    >
                        <text>{{ item }}</text>
                        <text class="category-arrow">›</text>
                    </button>
                </view>

                <view class="hero-card">
                    <view class="hero-copy">
                        <text class="hero-title">{{ copy.heroTitle }}</text>
                        <text class="hero-subtitle">{{ copy.heroSubtitle }}</text>
                        <button class="hero-button" @click="goCatalog">
                            {{ copy.heroAction }}
                        </button>
                    </view>
                    <view class="hero-visual">
                        <view class="hero-product-ring">
                            <image
                                :src="productImageSrc(heroProduct)"
                                :alt="heroProduct?.name || copy.heroTitle"
                                class="hero-product-image"
                                mode="aspectFit"
                                @error="
                                    heroProduct && markImageFailed(heroProduct.id)
                                "
                            />
                        </view>
                    </view>
                </view>
            </view>

            <view class="service-row">
                <view
                    v-for="(item, index) in copy.service"
                    :key="item[0]"
                    class="service-item"
                >
                    <text class="service-icon">
                        {{ ["✓", "▣", "↺", "◇"][index] }}
                    </text>
                    <view class="service-copy">
                        <text class="service-title">{{ item[0] }}</text>
                        <text class="service-desc">{{ item[1] }}</text>
                    </view>
                </view>
            </view>

            <view class="keyword-panel">
                <view class="search-main">
                    <input
                        v-model="keyword"
                        class="search-main-input"
                        type="text"
                        name="market-search-large"
                        confirm-type="search"
                        :placeholder="copy.searchPlaceholder"
                        @confirm="onSearch"
                    />
                    <button class="search-main-button" @click="onSearch">
                        {{ copy.searchAction }}
                    </button>
                </view>

                <view v-if="trendKeywords.length" class="keyword-strip">
                    <text class="keyword-label">
                        {{ hotKeywords.length ? copy.trending : copy.suggested }}
                    </text>
                    <button
                        v-for="item in trendKeywords"
                        :key="item"
                        class="keyword-chip"
                        @click="onKeywordSelect(item)"
                    >
                        {{ item }}
                    </button>
                </view>
            </view>

            <view class="section-heading">
                <view class="section-heading-main">
                    <text class="section-title">
                        {{ featuredProducts.length ? copy.hotTitle : resultsTitle }}
                    </text>
                    <text class="section-subtitle">{{ resultsHint }}</text>
                </view>
                <button class="view-all" @click="goCatalog">{{ copy.viewAll }} ›</button>
            </view>

            <view v-if="featuredProducts.length" class="featured-grid">
                <view
                    v-for="item in featuredProducts"
                    :key="item.id"
                    class="product-card"
                >
                    <view class="product-image-wrap">
                        <image
                            :src="productImageSrc(item)"
                            :alt="item.name"
                            class="product-image"
                            mode="aspectFit"
                            @error="markImageFailed(item.id)"
                        />
                    </view>

                    <view class="product-info">
                        <text class="product-name">{{ item.name }}</text>
                        <text class="product-meta">
                            {{ copy.stockPrefix }} {{ item.stockQuantity ?? "--" }}
                        </text>
                        <text class="product-price">{{ formatPrice(item.price) }}</text>
                    </view>

                    <button class="card-action" @click="onAddToCart(item)">
                        {{ loggedIn ? copy.addToCart : copy.addToCartLogin }}
                    </button>
                </view>
            </view>

            <view v-else class="empty-state">{{ copy.empty }}</view>

            <view class="load-more">
                <button
                    v-if="hasMore"
                    class="load-more-button"
                    :loading="loading"
                    @click="onLoadMore"
                >
                    {{ copy.loadMore }}
                </button>
                <text v-else class="no-more">{{ copy.noMore }}</text>
            </view>
        </view>
    </view>
</template>

<style scoped>
.mall-page {
    min-height: 100vh;
    padding: 24px 0 44px;
    color: #15171a;
    background:
        linear-gradient(180deg, rgba(248, 249, 251, 0.96), #ffffff 44%),
        #ffffff;
}

.mall-shell {
    width: min(1280px, calc(100% - 32px));
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 22px;
}

.mall-header {
    min-height: 64px;
    display: grid;
    grid-template-columns: 128px minmax(240px, 1fr) minmax(280px, 360px) auto;
    align-items: center;
    gap: 18px;
    padding: 0 20px;
    border: 1px solid #edf0f4;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 18px 60px rgba(20, 25, 35, 0.06);
}

.brand {
    cursor: pointer;
}

.brand-name {
    font-size: 20px;
    font-weight: 800;
    color: #111316;
    letter-spacing: 0;
}

.nav-scroll {
    min-width: 0;
    white-space: nowrap;
}

.nav-list {
    display: flex;
    align-items: center;
    gap: 22px;
}

.nav-link,
.header-action,
.icon-button,
.category-row,
.keyword-chip,
.view-all,
.card-action,
.load-more-button,
.hero-button,
.search-main-button {
    margin: 0;
    border: 0;
    background: transparent;
    color: inherit;
    line-height: 1;
}

.nav-link {
    position: relative;
    min-height: 44px;
    padding: 0;
    font-size: 13px;
    color: #24272c;
    font-weight: 600;
}

.nav-link.active::after {
    content: "";
    position: absolute;
    left: 50%;
    bottom: 8px;
    width: 18px;
    height: 2px;
    border-radius: 999px;
    background: #111316;
    transform: translateX(-50%);
}

.search-mini {
    height: 36px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 8px 0 14px;
    border: 1px solid #e8ebef;
    border-radius: 4px;
    background: #fbfcfd;
}

.search-mini-input {
    flex: 1;
    min-width: 0;
    height: 34px;
    color: #16191d;
    font-size: 12px;
}

.icon-button {
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: #4c535c;
    font-size: 18px;
}

.header-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 14px;
    white-space: nowrap;
}

.header-actions :deep(.locale-compact) {
    gap: 0;
}

.header-actions :deep(.locale-compact .switch-copy) {
    display: none;
}

.header-actions :deep(.locale-compact .switch-segment) {
    gap: 2px;
    padding: 2px;
    border-color: #e8ebef;
    border-radius: 4px;
    background: #fbfcfd;
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
}

.header-actions :deep(.locale-compact .switch-option) {
    min-width: 34px;
    min-height: 28px;
    padding: 0 8px;
    border-radius: 3px;
    color: #6f7781;
    box-shadow: none;
}

.header-actions :deep(.locale-compact .switch-option.active) {
    border-color: #111316;
    background: #111316;
    color: #ffffff;
}

.header-actions :deep(.locale-compact .switch-long) {
    display: none;
}

.header-actions :deep(.locale-compact .switch-short) {
    font-size: 11px;
    letter-spacing: 0;
}

.header-action {
    min-height: 36px;
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    color: #25282d;
}

.action-icon {
    color: #1d2025;
    font-size: 15px;
}

.search-icon {
    position: relative;
    width: 13px;
    height: 13px;
    border: 1.5px solid #25282d;
    border-radius: 50%;
}

.search-icon::after {
    content: "";
    position: absolute;
    width: 6px;
    height: 1.5px;
    right: -5px;
    bottom: -2px;
    border-radius: 999px;
    background: #25282d;
    transform: rotate(45deg);
    transform-origin: left center;
}

.user-icon {
    position: relative;
    width: 15px;
    height: 15px;
}

.user-icon::before {
    content: "";
    position: absolute;
    top: 0;
    left: 50%;
    width: 5px;
    height: 5px;
    border: 1.5px solid #1d2025;
    border-radius: 50%;
    transform: translateX(-50%);
}

.user-icon::after {
    content: "";
    position: absolute;
    left: 2px;
    right: 2px;
    bottom: 0;
    height: 6px;
    border: 1.5px solid #1d2025;
    border-bottom: 0;
    border-radius: 8px 8px 0 0;
}

.cart-action {
    position: relative;
}

.cart-badge {
    position: absolute;
    top: 1px;
    right: -9px;
    min-width: 14px;
    height: 14px;
    padding: 0 4px;
    border-radius: 999px;
    background: #ff3b30;
    color: #ffffff;
    font-size: 9px;
    line-height: 14px;
    text-align: center;
}

.hero-grid {
    display: grid;
    grid-template-columns: 220px minmax(0, 1fr);
    gap: 18px;
}

.category-panel,
.hero-card,
.keyword-panel,
.product-card,
.empty-state {
    border: 1px solid #edf0f4;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 18px 58px rgba(18, 24, 35, 0.05);
}

.category-panel {
    padding: 12px 0;
}

.category-title {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 18px 10px;
    color: #171a1f;
    font-size: 14px;
    font-weight: 700;
}

.menu-icon {
    width: 14px;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.menu-bar {
    width: 14px;
    height: 1.5px;
    border-radius: 999px;
    background: #393f46;
}

.category-row {
    width: 100%;
    min-height: 38px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 14px 0 18px;
    color: #4a5058;
    font-size: 13px;
}

.category-arrow {
    color: #a0a6ae;
    font-size: 18px;
}

.hero-card {
    position: relative;
    min-height: 360px;
    overflow: hidden;
    display: grid;
    grid-template-columns: minmax(280px, 0.82fr) minmax(300px, 1fr);
    align-items: center;
    padding: 46px 58px;
    background:
        radial-gradient(circle at 84% 48%, rgba(213, 222, 236, 0.66), transparent 32%),
        linear-gradient(135deg, #f7f9fd 0%, #eef3fa 48%, #f9fbfe 100%);
}

.hero-copy {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
    position: relative;
    z-index: 2;
}

.hero-title {
    max-width: 400px;
    color: #101216;
    font-size: 42px;
    font-weight: 900;
    line-height: 1.18;
    letter-spacing: 0;
}

.hero-subtitle {
    max-width: 420px;
    color: #4f5965;
    font-size: 16px;
    line-height: 1.7;
}

.hero-button {
    min-width: 112px;
    height: 42px;
    margin-top: 8px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 3px;
    background: #111316;
    color: #ffffff;
    font-size: 13px;
    font-weight: 700;
}

.hero-visual {
    min-height: 260px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.hero-product-ring {
    width: min(390px, 88%);
    aspect-ratio: 1 / 1;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 999px;
    background:
        radial-gradient(circle, rgba(255, 255, 255, 0.88) 0%, rgba(255, 255, 255, 0.2) 62%, transparent 63%),
        linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(230, 237, 247, 0.32));
}

.hero-product-image {
    width: 76%;
    height: 76%;
    filter: drop-shadow(0 28px 32px rgba(32, 38, 48, 0.16));
}

.service-row {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    padding: 14px 8px 2px;
}

.service-item {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    min-width: 0;
}

.service-icon {
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #d9dee6;
    border-radius: 999px;
    color: #20242a;
    font-size: 14px;
    font-weight: 700;
}

.service-copy {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
}

.service-title {
    color: #16191d;
    font-size: 13px;
    font-weight: 800;
}

.service-desc {
    color: #7b838d;
    font-size: 11px;
}

.keyword-panel {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
}

.search-main {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 96px;
    gap: 10px;
}

.search-main-input {
    height: 42px;
    padding: 0 14px;
    border: 1px solid #e6e9ee;
    border-radius: 4px;
    color: #16191d;
    font-size: 13px;
    background: #fbfcfd;
}

.search-main-button {
    height: 42px;
    border-radius: 4px;
    background: #111316;
    color: #ffffff;
    font-size: 13px;
    font-weight: 700;
}

.keyword-strip {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.keyword-label {
    color: #848c96;
    font-size: 12px;
    margin-right: 2px;
}

.keyword-chip {
    min-height: 30px;
    padding: 0 12px;
    border: 1px solid #e7ebf0;
    border-radius: 4px;
    color: #4b535d;
    font-size: 12px;
    background: #ffffff;
}

.section-heading {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    padding-top: 2px;
}

.section-heading-main {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.section-title {
    color: #15181d;
    font-size: 20px;
    font-weight: 900;
}

.section-subtitle {
    color: #7c848f;
    font-size: 12px;
}

.view-all {
    color: #7b838e;
    font-size: 12px;
}

.featured-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 14px;
}

.product-card {
    min-width: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

.product-image-wrap {
    height: 176px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 18px;
    background: #f7f8fa;
}

.product-image {
    width: 100%;
    height: 100%;
}

.product-info {
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
    line-height: 1.55;
    overflow-wrap: anywhere;
}

.product-meta {
    color: #8b939e;
    font-size: 11px;
}

.product-price {
    color: #ff3b30;
    font-size: 17px;
    font-weight: 900;
}

.card-action {
    height: 38px;
    margin: auto 14px 14px;
    border: 1px solid #16191d;
    border-radius: 4px;
    color: #16191d;
    font-size: 12px;
    font-weight: 700;
}

.empty-state {
    padding: 28px 18px;
    text-align: center;
    color: #7d8590;
    font-size: 13px;
}

.load-more {
    display: flex;
    justify-content: center;
    padding: 4px 0 18px;
}

.load-more-button {
    min-width: 138px;
    height: 40px;
    border: 1px solid #d9dee6;
    border-radius: 4px;
    background: #ffffff;
    color: #1d2025;
    font-size: 13px;
    font-weight: 700;
}

.no-more {
    color: #9aa2ad;
    font-size: 12px;
}

button::after {
    border: none;
}

@media (hover: hover) {
    .category-row:hover,
    .keyword-chip:hover,
    .nav-link:hover,
    .header-action:hover,
    .view-all:hover {
        color: #ff3b30;
    }

    .product-card:hover {
        transform: translateY(-2px);
        border-color: #e2e6ec;
        box-shadow: 0 24px 70px rgba(18, 24, 35, 0.09);
    }

    .card-action:hover,
    .load-more-button:hover {
        background: #111316;
        color: #ffffff;
    }
}

@media (max-width: 1100px) {
    .mall-header {
        grid-template-columns: 116px minmax(0, 1fr);
    }

    .search-mini,
    .header-actions {
        grid-column: span 1;
    }

    .hero-grid {
        grid-template-columns: 190px minmax(0, 1fr);
    }

    .featured-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (max-width: 820px) {
    .mall-page {
        padding: 12px 0 32px;
    }

    .mall-shell {
        width: min(100% - 20px, 720px);
        gap: 16px;
    }

    .mall-header {
        grid-template-columns: 1fr;
        align-items: stretch;
        padding: 14px;
        gap: 12px;
    }

    .nav-list {
        gap: 18px;
    }

    .search-mini {
        width: 100%;
    }

    .header-actions {
        justify-content: flex-start;
        flex-wrap: wrap;
    }

    .hero-grid {
        grid-template-columns: 1fr;
    }

    .category-panel {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        padding: 10px;
    }

    .category-title {
        grid-column: 1 / -1;
        padding: 0 6px 8px;
    }

    .category-row {
        padding: 0 8px;
    }

    .hero-card {
        min-height: auto;
        grid-template-columns: 1fr;
        padding: 30px 24px;
    }

    .hero-title {
        font-size: 32px;
    }

    .hero-visual {
        min-height: 220px;
    }

    .service-row {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px 10px;
    }

    .service-item {
        justify-content: flex-start;
    }

    .featured-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 520px) {
    .category-panel,
    .featured-grid,
    .service-row,
    .search-main {
        grid-template-columns: 1fr;
    }

    .section-heading {
        align-items: flex-start;
        flex-direction: column;
    }

    .product-image-wrap {
        height: 200px;
    }
}
</style>
