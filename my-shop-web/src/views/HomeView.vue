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
      <div>
        <p class="eyebrow">Daily Briefing</p>
        <h3>Operations Snapshot</h3>
        <p class="muted">Real-time values from product and order services.</p>
      </div>
      <el-button :loading="loading" round type="primary" @click="loadDashboard">Refresh</el-button>
    </section>

    <section class="stats">
      <article class="glass-card metric">
        <p>Total products</p>
        <strong>{{ productsTotal ?? '--' }}</strong>
      </article>
      <article class="glass-card metric">
        <p>Total orders</p>
        <strong>{{ ordersTotal ?? '--' }}</strong>
      </article>
      <article class="glass-card metric">
        <p>Pending payment ratio</p>
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
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.hero h3 {
  margin: 8px 0;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  padding: 18px;
}

.metric p {
  margin: 0;
  color: var(--text-muted);
}

.metric strong {
  display: block;
  margin-top: 8px;
  font-size: 1.4rem;
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

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats {
    grid-template-columns: 1fr;
  }
}
</style>
