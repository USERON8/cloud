import { computed, ref } from "vue";
import {
    listSearchHotKeywordsWithFallback,
    listSearchKeywordRecommendationsWithFallback,
} from "../api/search-ops";
import type { ProductItem } from "../types/domain";

export interface ProductSearchFeedResult {
    items: ProductItem[];
    total: number;
}

export interface ProductSearchFeedLoaderContext {
    keyword: string;
    page: number;
    size: number;
    reset: boolean;
    hotKeywords: string[];
    recommendations: string[];
    setKeyword: (value: string) => void;
    refreshKeywords: (seed?: string) => Promise<void>;
}

export interface ProductSearchFeedOptions {
    loadPage: (
        context: ProductSearchFeedLoaderContext,
    ) => Promise<ProductSearchFeedResult>;
    onLoadError: (error: unknown) => void;
    initialSize?: number;
}

export function useProductSearchFeed(options: ProductSearchFeedOptions) {
    const keyword = ref("");
    const loading = ref(false);
    const rows = ref<ProductItem[]>([]);
    const page = ref(1);
    const size = ref(options.initialSize ?? 10);
    const hasMore = ref(true);
    const hotKeywords = ref<string[]>([]);
    const recommendations = ref<string[]>([]);
    const initialized = ref(false);
    const latestLoadRequestId = ref(0);
    const failedImageIds = ref<Record<string, boolean>>({});

    const activeKeyword = computed(() => keyword.value.trim());

    async function refreshKeywords(seed = ""): Promise<void> {
        const [hotResult, recResult] = await Promise.all([
            listSearchHotKeywordsWithFallback(8).catch(() => [] as string[]),
            listSearchKeywordRecommendationsWithFallback(seed, 10).catch(
                () => [] as string[],
            ),
        ]);
        hotKeywords.value = hotResult;
        recommendations.value = recResult;
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
            const result = await options.loadPage({
                keyword: activeKeyword.value,
                page: page.value,
                size: size.value,
                reset,
                hotKeywords: hotKeywords.value,
                recommendations: recommendations.value,
                setKeyword: (value) => {
                    keyword.value = value;
                },
                refreshKeywords,
            });
            if (requestId !== latestLoadRequestId.value) {
                return;
            }
            if (reset) {
                failedImageIds.value = {};
            }
            rows.value = reset ? result.items : rows.value.concat(result.items);
            hasMore.value = rows.value.length < result.total;
            await refreshKeywords(activeKeyword.value);
        } catch (error) {
            if (requestId !== latestLoadRequestId.value) {
                return;
            }
            options.onLoadError(error);
        } finally {
            if (requestId === latestLoadRequestId.value) {
                loading.value = false;
            }
        }
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

    function initialize(): void {
        if (initialized.value) {
            return;
        }
        initialized.value = true;
        void loadProducts(true);
        void refreshKeywords("");
    }

    return {
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
    };
}
