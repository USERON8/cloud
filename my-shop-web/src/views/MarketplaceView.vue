<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isAuthenticated, sessionState } from '../auth/session'
import { smartSearchProductsWithFallback } from '../api/product'
import type { SearchProductDocument } from '../types/domain'

interface ShopCard {
  id: number
  name: string
  productCount: number
  minPrice: number
  maxPrice: number
}

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const products = ref<SearchProductDocument[]>([])
const shops = ref<ShopCard[]>([])
const loginGateVisible = ref(false)
const pendingPath = ref('/login')
const pendingFeature = ref('')
const ratesLoading = ref(false)
const ratesError = ref('')
const rateUpdatedAt = ref('')
const exchangeRates = ref<Record<'JPY' | 'USD' | 'EUR' | 'GBP', number>>({
  JPY: 0,
  USD: 0,
  EUR: 0,
  GBP: 0
})

const displayName = computed(() => sessionState.user?.nickname || sessionState.user?.username || '访客')
const loginStatus = computed(() => (isAuthenticated() ? '已登录' : '未登录'))

const featuredProducts = computed(() => products.value.slice(0, 12))
const featuredShops = computed(() => shops.value.slice(0, 8))

function normalizeProducts(source: SearchProductDocument[]): SearchProductDocument[] {
  return source.filter((item) => item.status === 1 && typeof item.productId === 'number')
}

function buildShops(source: SearchProductDocument[]): ShopCard[] {
  const map = new Map<number, ShopCard>()
  source.forEach((item) => {
    if (typeof item.shopId !== 'number' || typeof item.price !== 'number') {
      return
    }
    const current = map.get(item.shopId)
    if (!current) {
      map.set(item.shopId, {
        id: item.shopId,
        name: item.shopName?.trim() || `Shop #${item.shopId}`,
        productCount: 1,
        minPrice: item.price,
        maxPrice: item.price
      })
      return
    }
    current.productCount += 1
    current.minPrice = Math.min(current.minPrice, item.price)
    current.maxPrice = Math.max(current.maxPrice, item.price)
  })
  return [...map.values()].sort((a, b) => b.productCount - a.productCount)
}

async function loadMarketData(): Promise<void> {
  loading.value = true
  try {
    const result = await smartSearchProductsWithFallback({
      keyword: keyword.value.trim() || undefined,
      page: 1,
      size: 80,
      sortField: 'score',
      sortOrder: 'desc'
    })
    const normalized = normalizeProducts(result.documents || [])
    products.value = normalized
    shops.value = buildShops(normalized)
  } catch (error) {
    const message = error instanceof Error ? error.message : '加载首页失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function loadExchangeRates(): Promise<void> {
  ratesLoading.value = true
  ratesError.value = ''
  try {
    const response = await fetch(
      'https://api.frankfurter.app/latest?from=CNY&to=JPY,USD,EUR,GBP'
    )
    if (!response.ok) {
      throw new Error(`汇率接口异常: HTTP ${response.status}`)
    }
    const payload = (await response.json()) as {
      date?: string
      rates?: Partial<Record<'JPY' | 'USD' | 'EUR' | 'GBP', number>>
    }
    exchangeRates.value = {
      JPY: Number(payload.rates?.JPY || 0),
      USD: Number(payload.rates?.USD || 0),
      EUR: Number(payload.rates?.EUR || 0),
      GBP: Number(payload.rates?.GBP || 0)
    }
    rateUpdatedAt.value = payload.date || ''
  } catch (error) {
    ratesError.value = error instanceof Error ? error.message : '汇率加载失败'
  } finally {
    ratesLoading.value = false
  }
}

async function goProtected(path: string, feature: string): Promise<void> {
  if (isAuthenticated()) {
    await router.push(path)
    return
  }
  pendingPath.value = path
  pendingFeature.value = feature
  loginGateVisible.value = true
}

async function toLoginFromGate(): Promise<void> {
  loginGateVisible.value = false
  await router.push({ path: '/login', query: { redirect: pendingPath.value } })
}

function toCatalogSearch(name: string): void {
  void router.push({ path: '/app/catalog', query: { keyword: name } })
}

onMounted(() => {
  void loadMarketData()
  void loadExchangeRates()
})
</script>

<template>
  <div class="market-page">
    <header class="glass-card topbar">
      <div class="brand-wrap">
        <p class="eyebrow">My Shop Marketplace</p>
        <h1>认证商家与在售商品</h1>
        <p class="sub">当前用户：{{ displayName }} · {{ loginStatus }} · 可先浏览再登录</p>
      </div>
      <div class="entry-actions">
        <el-button round @click="$router.push('/login')">用户登录</el-button>
        <el-button round type="primary" @click="$router.push('/login?entry=merchant')">商家登录入口</el-button>
      </div>
    </header>

    <section class="glass-card search-hero">
      <div class="search-line">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索已开店商家 / 已上架商品"
          @keyup.enter="loadMarketData"
        />
        <el-button :loading="loading" round type="primary" @click="loadMarketData">搜索</el-button>
      </div>
      <div class="quick-actions">
        <el-button round @click="goProtected('/app/orders', '订单管理')">我的订单</el-button>
        <el-button round @click="goProtected('/app/profile', '个人中心')">个人中心</el-button>
        <el-button round @click="goProtected('/app/catalog/manage', '商家商品管理')">商家管理</el-button>
      </div>
    </section>

    <section class="glass-card rates-panel">
      <div class="rates-head">
        <h2>实时汇率面板（基准：1 CNY）</h2>
        <el-button :loading="ratesLoading" round size="small" @click="loadExchangeRates">刷新汇率</el-button>
      </div>
      <p v-if="rateUpdatedAt" class="rate-time">更新时间：{{ rateUpdatedAt }}</p>
      <el-alert v-if="ratesError" :closable="false" show-icon type="warning" :title="ratesError" />
      <div class="rate-grid">
        <article class="rate-card">
          <p>日元 ¥ (JPY)</p>
          <strong>¥ {{ exchangeRates.JPY.toFixed(4) }}</strong>
        </article>
        <article class="rate-card">
          <p>美元 $ (USD)</p>
          <strong>$ {{ exchangeRates.USD.toFixed(4) }}</strong>
        </article>
        <article class="rate-card">
          <p>欧元 € (EUR)</p>
          <strong>€ {{ exchangeRates.EUR.toFixed(4) }}</strong>
        </article>
        <article class="rate-card">
          <p>英镑 £ (GBP)</p>
          <strong>£ {{ exchangeRates.GBP.toFixed(4) }}</strong>
        </article>
      </div>
    </section>

    <section class="section-head">
      <h2>已认证已开店商家</h2>
      <span>{{ featuredShops.length }} 家</span>
    </section>
    <section class="shop-grid">
      <article v-for="shop in featuredShops" :key="shop.id" class="glass-card shop-card" @click="keyword = shop.name; loadMarketData()">
        <div class="badges">
          <span class="badge verified">已认证</span>
          <span class="badge open">已开店</span>
        </div>
        <h3>{{ shop.name }}</h3>
        <p>在售商品 {{ shop.productCount }} 件</p>
        <p>价格带 CNY {{ shop.minPrice.toFixed(2) }} ~ {{ shop.maxPrice.toFixed(2) }}</p>
      </article>
    </section>

    <section class="section-head">
      <h2>已上架商品</h2>
      <span>{{ featuredProducts.length }} 件</span>
    </section>
    <section class="product-grid">
      <article v-for="item in featuredProducts" :key="item.productId" class="glass-card product-card" @click="toCatalogSearch(item.productName || '')">
        <div class="cover-svg" aria-hidden="true">
          <svg viewBox="0 0 120 80">
            <defs>
              <linearGradient id="cardGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#eff4ff" />
                <stop offset="100%" stop-color="#d9e6ff" />
              </linearGradient>
            </defs>
            <rect x="4" y="6" width="112" height="68" rx="14" fill="url(#cardGrad)" />
            <path d="M22 52h76M30 42h60M40 32h40" stroke="#3a6fe9" stroke-width="3" stroke-linecap="round" />
          </svg>
        </div>
        <h3>{{ item.productName || '未命名商品' }}</h3>
        <p class="muted">{{ item.shopName || `Shop #${item.shopId ?? '-'}` }}</p>
        <p class="price">CNY {{ typeof item.price === 'number' ? item.price.toFixed(2) : '--' }}</p>
      </article>
    </section>

    <el-dialog
      v-model="loginGateVisible"
      width="420px"
      align-center
      :show-close="false"
      :close-on-click-modal="true"
      class="login-gate-dialog"
      modal-class="login-gate-mask"
    >
      <template #header>
        <div class="login-gate-title">需要登录</div>
      </template>
      <p class="login-gate-text">
        {{ pendingFeature }} 需要登录后使用。你可以先继续浏览商家和商品，或立即登录。
      </p>
      <div class="login-gate-actions">
        <el-button round @click="loginGateVisible = false">继续浏览</el-button>
        <el-button round type="primary" @click="toLoginFromGate">去登录</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.market-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: clamp(0.8rem, 1.2vw, 1.2rem);
  display: grid;
  gap: 0.9rem;
}

.topbar,
.search-hero {
  padding: clamp(0.9rem, 1.3vw, 1.2rem);
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: center;
  flex-wrap: wrap;
}

.brand-wrap h1 {
  margin: 0.3rem 0;
  font-size: clamp(1.2rem, 1.8vw, 1.9rem);
}

.eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: 0.8rem;
  font-weight: 600;
}

.sub {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.88rem;
}

.entry-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.search-line {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.55rem;
}

.quick-actions {
  margin-top: 0.7rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0.1rem;
}

.rates-panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.rates-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.rates-head h2 {
  margin: 0;
  font-size: clamp(1rem, 1.2vw, 1.18rem);
}

.rate-time {
  margin: 0.3rem 0 0.7rem;
  color: var(--text-muted);
  font-size: 0.82rem;
}

.rate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 0.55rem;
}

.rate-card {
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.6);
  border-radius: 14px;
  padding: 0.7rem 0.8rem;
}

.rate-card p {
  margin: 0;
  color: var(--text-muted);
}

.rate-card strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.15rem;
}

.section-head h2 {
  margin: 0;
  font-size: clamp(1rem, 1.3vw, 1.25rem);
}

.section-head span {
  color: var(--text-muted);
}

.shop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.7rem;
}

.shop-card,
.product-card {
  padding: 0.9rem;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.shop-card:hover,
.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(26, 46, 92, 0.14);
}

.badges {
  display: flex;
  gap: 0.35rem;
  margin-bottom: 0.45rem;
}

.badge {
  border-radius: 999px;
  padding: 0.14rem 0.5rem;
  font-size: 0.72rem;
}

.verified {
  background: rgba(44, 119, 255, 0.12);
  color: #1b58ca;
}

.open {
  background: rgba(20, 156, 97, 0.12);
  color: #17835b;
}

.shop-card h3,
.product-card h3 {
  margin: 0.2rem 0 0.4rem;
  font-size: 1.02rem;
}

.shop-card p {
  margin: 0.15rem 0;
  color: var(--text-muted);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.7rem;
}

.cover-svg {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 0.55rem;
}

.cover-svg svg {
  width: 100%;
  height: auto;
  display: block;
}

.muted {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.85rem;
}

.price {
  margin: 0.35rem 0 0;
  font-weight: 700;
  color: #17336d;
}

.login-gate-title {
  font-size: 1.05rem;
  font-weight: 700;
}

.login-gate-text {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.login-gate-actions {
  margin-top: 0.95rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

:deep(.login-gate-dialog .el-dialog) {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px) saturate(1.15);
  -webkit-backdrop-filter: blur(16px) saturate(1.15);
}

:deep(.login-gate-mask) {
  background: rgba(22, 35, 61, 0.28);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

@media (max-width: 760px) {
  .search-line {
    grid-template-columns: 1fr;
  }
}
</style>
