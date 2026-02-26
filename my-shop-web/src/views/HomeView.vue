<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listOrders } from '../api/order'
import { listProducts } from '../api/product'

const loading = ref(false)
const productsTotal = ref<number | null>(null)
const ordersTotal = ref<number | null>(null)
const pendingPayment = ref<number | null>(null)
const loadError = ref('')

const pendingRate = computed(() => {
  if (!ordersTotal.value || !pendingPayment.value) {
    return '0%'
  }
  return `${Math.round((pendingPayment.value / ordersTotal.value) * 100)}%`
})

async function loadDashboard(): Promise<void> {
  loading.value = true
  loadError.value = ''

  const [productsResult, ordersResult] = await Promise.allSettled([
    listProducts({ page: 1, size: 1 }),
    listOrders({ page: 1, size: 20 })
  ])

  if (productsResult.status === 'fulfilled') {
    productsTotal.value = productsResult.value.total
  } else {
    productsTotal.value = null
    loadError.value = productsResult.reason instanceof Error ? productsResult.reason.message : 'Products API unavailable'
  }

  if (ordersResult.status === 'fulfilled') {
    ordersTotal.value = ordersResult.value.total
    pendingPayment.value = ordersResult.value.records.filter((item) => item.status === 0).length
  } else {
    ordersTotal.value = null
    pendingPayment.value = null
    loadError.value = ordersResult.reason instanceof Error ? ordersResult.reason.message : 'Orders API unavailable'
  }

  loading.value = false
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="grid">
    <section class="glass-card hero">
      <div class="hero-copy">
        <p class="eyebrow">Daily Briefing</p>
        <h3>Operations Snapshot</h3>
        <p class="muted">Real-time values from product and order services.</p>
      </div>
      <el-button :loading="loading" round type="primary" @click="loadDashboard">
        <span class="btn-wrap">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M19 6v5h-5M5 18v-5h5M19 11a7 7 0 0 0-12-3M5 13a7 7 0 0 0 12 3" />
          </svg>
          Refresh
        </span>
      </el-button>
    </section>

    <section class="stats">
      <article class="glass-card metric">
        <div class="metric-title">
          <span class="metric-icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 7.5 12 4l8 3.5-8 3.5L4 7.5Zm0 3.5 8 3.5 8-3.5M4 14.5 12 18l8-3.5" />
            </svg>
          </span>
          <p>Total products</p>
        </div>
        <strong>{{ productsTotal ?? '--' }}</strong>
      </article>
      <article class="glass-card metric">
        <div class="metric-title">
          <span class="metric-icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 5h10l3 3v11H6V5Zm10 0v3h3M9 12h6M9 15h6" />
            </svg>
          </span>
          <p>Total orders</p>
        </div>
        <strong>{{ ordersTotal ?? '--' }}</strong>
      </article>
      <article class="glass-card metric">
        <div class="metric-title">
          <span class="metric-icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 12h16M12 4v16M6.7 6.7l10.6 10.6M17.3 6.7 6.7 17.3" />
            </svg>
          </span>
          <p>Pending payment ratio</p>
        </div>
        <strong>{{ pendingRate }}</strong>
      </article>
    </section>

    <el-alert v-if="loadError" :closable="false" show-icon type="warning" :title="loadError" />
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  gap: 14px;
}

.hero {
  padding: clamp(1rem, 1.5vw, 1.25rem);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.hero-copy {
  min-width: 0;
}

.hero h3 {
  margin: 0.45rem 0;
  font-size: clamp(1.18rem, 1.45vw, 1.55rem);
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(11.5rem, 1fr));
  gap: 12px;
}

.metric {
  padding: clamp(0.9rem, 1.3vw, 1.1rem);
  min-height: 8.5rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.metric-icon {
  width: 1.3rem;
  height: 1.3rem;
  color: var(--accent);
  flex: 0 0 auto;
}

.metric-icon svg {
  width: 100%;
  height: 100%;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.metric p {
  margin: 0;
  color: var(--text-muted);
}

.metric strong {
  display: block;
  margin-top: 8px;
  font-size: clamp(1.45rem, 2vw, 1.9rem);
  letter-spacing: 0.01em;
}

.eyebrow {
  margin: 0;
  color: var(--accent);
  font-weight: 600;
  font-size: 0.8rem;
}

.muted {
  margin: 0;
  color: var(--text-muted);
}

.btn-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-wrap svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats {
    grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  }
}
</style>
