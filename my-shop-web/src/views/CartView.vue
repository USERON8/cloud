<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartItems, cartTotal, clearCart, removeFromCart, removeItemsByShops, setCartItemQuantity } from '../store/cart'
import { createOrder } from '../api/order'
import type { CartEntry } from '../store/cart'

interface ShopGroup {
  shopId: number
  items: CartEntry[]
  subtotal: number
}

const router = useRouter()
const placing = ref(false)

const shopGroups = computed<ShopGroup[]>(() => {
  const map = new Map<number, CartEntry[]>()
  for (const item of cartItems.value) {
    const list = map.get(item.shopId)
    if (list) {
      list.push(item)
    } else {
      map.set(item.shopId, [item])
    }
  }
  return [...map.entries()].map(([shopId, items]) => ({
    shopId,
    items,
    subtotal: items.reduce((sum, i) => sum + i.price * i.quantity, 0)
  }))
})

function formatPrice(value: number): string {
  return `CNY ${value.toFixed(2)}`
}

function onQuantityChange(productId: number, value: number): void {
  setCartItemQuantity(productId, value)
}

async function onRemove(item: CartEntry): Promise<void> {
  try {
    await ElMessageBox.confirm(`Remove "${item.productName}" from cart?`, 'Confirm', { type: 'warning' })
    removeFromCart(item.productId)
    ElMessage.success('Item removed.')
  } catch {
    // user cancelled
  }
}

async function onClearCart(): Promise<void> {
  try {
    await ElMessageBox.confirm('Clear all items in cart?', 'Confirm', { type: 'warning' })
    clearCart()
    ElMessage.success('Cart cleared.')
  } catch {
    // user cancelled
  }
}

async function onPlaceOrder(): Promise<void> {
  if (shopGroups.value.length === 0) {
    ElMessage.warning('Your cart is empty.')
    return
  }

  try {
    await ElMessageBox.confirm(
      `Place ${shopGroups.value.length > 1 ? shopGroups.value.length + ' orders' : '1 order'} for a total of ${formatPrice(cartTotal.value)}?`,
      'Confirm Order',
      { type: 'info', confirmButtonText: 'Place Order', cancelButtonText: 'Cancel' }
    )
  } catch {
    return
  }

  placing.value = true
  const successShops: number[] = []
  const failedShops: number[] = []

  for (const group of shopGroups.value) {
    try {
      await createOrder({
        shopId: group.shopId,
        items: group.items.map((i) => ({
          productId: i.productId,
          quantity: i.quantity,
          price: i.price
        }))
      })
      successShops.push(group.shopId)
    } catch (error) {
      failedShops.push(group.shopId)
      const msg = error instanceof Error ? error.message : 'Order creation failed'
      ElMessage.error(`Shop #${group.shopId}: ${msg}`)
    }
  }

  placing.value = false

  if (successShops.length > 0) {
    removeItemsByShops(successShops)
    ElMessage.success(`${successShops.length} order(s) placed successfully!`)
    if (failedShops.length === 0) {
      await router.push('/app/orders')
      return
    }
  }

  if (failedShops.length > 0) {
    ElMessage.warning(`Orders failed for ${failedShops.map((id) => `Shop #${id}`).join(', ')}. Items were kept in cart.`)
  }
}
</script>

<template>
  <section class="glass-card panel">
    <div class="header">
      <h3>
        <span class="title-wrap">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M6 2H3L2 11h18l-1-9h-3M6 2l2 9h8l2-9M9 19a2 2 0 1 0 0-4 2 2 0 1 0 0 4Zm7 0a2 2 0 1 0 0-4 2 2 0 1 0 0 4Z" />
          </svg>
          Shopping Cart
        </span>
      </h3>
      <div class="header-actions">
        <router-link class="inline-link" to="/app/catalog">
          <el-button round>
            <span class="btn-wrap">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5M12 5l-7 7 7 7" /></svg>
              Continue Shopping
            </span>
          </el-button>
        </router-link>
        <el-button v-if="cartItems.length > 0" round @click="onClearCart">Clear Cart</el-button>
      </div>
    </div>

    <div v-if="cartItems.length === 0" class="empty-state">
      <div class="empty-icon" aria-hidden="true">
        <svg viewBox="0 0 64 64">
          <circle cx="32" cy="32" r="28" fill="rgba(59,130,246,0.08)" />
          <path d="M20 18h-4l-2 18h32l-2-18h-4M20 18l4 18h16l4-18M26 46a3 3 0 1 0 0 6 3 3 0 0 0 0-6Zm12 0a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"
            fill="none" stroke="#93a3be" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </div>
      <p class="empty-title">Your cart is empty</p>
      <p class="empty-sub">Browse products and add items to get started.</p>
      <router-link to="/app/catalog">
        <el-button round type="primary">Browse Products</el-button>
      </router-link>
    </div>

    <template v-else>
      <div v-for="group in shopGroups" :key="group.shopId" class="shop-group glass-card">
        <div class="shop-label">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 9l9-7 9 7v11H3V9ZM9 22V12h6v10" />
          </svg>
          Shop #{{ group.shopId }}
          <span class="shop-subtotal">Subtotal: {{ formatPrice(group.subtotal) }}</span>
        </div>

        <div v-for="item in group.items" :key="item.productId" class="cart-row">
          <div class="item-info">
            <span class="item-name">{{ item.productName }}</span>
            <span class="item-price">{{ formatPrice(item.price) }} each</span>
          </div>
          <div class="item-controls">
            <el-input-number
              :model-value="item.quantity"
              :min="1"
              :max="999"
              :step="1"
              size="small"
              controls-position="right"
              @change="(val: number) => onQuantityChange(item.productId, val)"
            />
            <span class="item-line-total">{{ formatPrice(item.price * item.quantity) }}</span>
            <el-button circle plain size="small" type="danger" @click="onRemove(item)">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12" /></svg>
            </el-button>
          </div>
        </div>
      </div>

      <div class="summary">
        <div class="summary-row">
          <span>Total items</span>
          <span>{{ cartItems.reduce((s, i) => s + i.quantity, 0) }}</span>
        </div>
        <div class="summary-row total-row">
          <span>Grand Total</span>
          <strong>{{ formatPrice(cartTotal) }}</strong>
        </div>
        <el-button
          :loading="placing"
          round
          size="large"
          type="primary"
          class="checkout-btn"
          @click="onPlaceOrder"
        >
          <span class="btn-wrap">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 7H4v13h16V7ZM16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2M9 12h6M9 16h4" /></svg>
            Place Order
          </span>
        </el-button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.header h3 {
  margin: 0;
  font-size: clamp(1.02rem, 1.25vw, 1.28rem);
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.title-wrap svg,
.btn-wrap svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.btn-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.inline-link {
  text-decoration: none;
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem 1rem;
  gap: 0.6rem;
  text-align: center;
}

.empty-icon svg {
  width: 80px;
  height: 80px;
}

.empty-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.empty-sub {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

/* Shop group */
.shop-group {
  margin-bottom: 12px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-radius: 16px;
}

.shop-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.shop-label svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex: 0 0 auto;
}

.shop-subtotal {
  margin-left: auto;
  font-size: 0.85rem;
  color: var(--text-muted);
}

/* Cart row */
.cart-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  flex: 1;
}

.item-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  font-size: 0.82rem;
  color: var(--text-muted);
}

.item-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.item-controls :deep(.el-input-number) {
  width: 100px;
}

.item-line-total {
  font-weight: 700;
  font-size: 0.95rem;
  color: #17336d;
  min-width: 88px;
  text-align: right;
}

.item-controls .el-button.is-circle svg {
  width: 0.9rem;
  height: 0.9rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

/* Summary */
.summary {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.total-row {
  font-size: 1.05rem;
  color: var(--text-main);
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.07);
}

.total-row strong {
  font-size: 1.2rem;
  color: #17336d;
}

.checkout-btn {
  width: 100%;
  margin-top: 4px;
}

@media (max-width: 700px) {
  .cart-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .item-controls {
    width: 100%;
    justify-content: flex-start;
  }

  .item-line-total {
    text-align: left;
  }
}
</style>
