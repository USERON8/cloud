<script setup lang="ts">
import { computed, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { cartItems, cartTotal, clearCart, removeFromCart, setCartItemQuantity } from '../../../store/cart'
import type { CartEntry } from '../../../store/cart'
import { createOrder } from '../../../api/order'
import { formatPrice } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'
import { navigateTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'

interface ShopGroup {
  shopId: number
  items: CartEntry[]
  subtotal: number
}

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

function changeQuantity(item: CartEntry, delta: number): void {
  const next = item.quantity + delta
  if (next <= 0) {
    void onRemove(item)
    return
  }
  setCartItemQuantity(item.productId, item.skuId, next)
}

async function onRemove(item: CartEntry): Promise<void> {
  const ok = await confirm(`Remove ${item.productName} from the cart?`)
  if (!ok) return
  removeFromCart(item.productId, item.skuId)
  toast('Item removed', 'success')
}

async function onClearCart(): Promise<void> {
  const ok = await confirm('Clear the cart?')
  if (!ok) return
  clearCart()
  toast('Cart cleared', 'success')
}

async function onPlaceOrder(): Promise<void> {
  if (shopGroups.value.length === 0) {
    toast('Cart is empty')
    return
  }
  const ok = await confirm(`Submit ${shopGroups.value.length} shop order(s)?`)
  if (!ok) return

  placing.value = true
  const orderGroups = shopGroups.value.map((group) => ({
    shopId: group.shopId,
    items: group.items.map((item) => ({ ...item }))
  }))
  const failedShops = new Set<number>()
  const successfulItems: CartEntry[] = []

  try {
    for (const group of orderGroups) {
      for (const item of group.items) {
        if (typeof item.skuId !== 'number') {
          failedShops.add(group.shopId)
          toast(`Shop ${group.shopId} item is missing a SKU`)
          continue
        }
        try {
          await createOrder({
            shopId: group.shopId,
            spuId: item.productId,
            skuId: item.skuId,
            quantity: item.quantity,
            price: item.price
          })
          successfulItems.push(item)
        } catch (error) {
          failedShops.add(group.shopId)
          const msg = error instanceof Error ? error.message : 'Unknown error'
          toast(`Shop ${group.shopId}: ${msg}`)
        }
      }
    }
  } finally {
    placing.value = false
  }

  successfulItems.forEach((item) => {
    removeFromCart(item.productId, item.skuId)
  })

  if (failedShops.size === 0) {
    toast(`Submitted ${successfulItems.length} item(s)`, 'success')
    navigateTo(Routes.appOrders, undefined, { requiresAuth: true })
    return
  }

  if (successfulItems.length > 0) {
    toast('Some items were submitted. Only failed items remain in the cart.')
    return
  }

  toast(`Failed shops: ${[...failedShops].join(', ')}`)
}
</script>

<template>
  <AppShell title="Cart">
    <view class="panel glass-card">
      <view class="header">
        <text class="section-title">Cart</text>
        <view class="header-actions">
          <button class="btn-outline" @click="navigateTo(Routes.appCatalog, undefined, { requiresAuth: true })">
            Continue shopping
          </button>
          <button v-if="cartItems.length > 0" class="btn-outline" @click="onClearCart">Clear</button>
        </view>
      </view>

      <view v-if="cartItems.length === 0" class="empty">
        <text class="text-muted">Your cart is empty</text>
      </view>

      <view v-else>
        <view v-for="group in shopGroups" :key="group.shopId" class="shop-group">
          <text class="shop-title">Shop {{ group.shopId }} - Subtotal {{ formatPrice(group.subtotal) }}</text>
          <view v-for="item in group.items" :key="item.productId" class="item-row">
            <view class="item-info">
              <text class="item-name">{{ item.productName }}</text>
              <text class="item-meta">{{ formatPrice(item.price) }}</text>
            </view>
            <view class="item-actions">
              <button class="btn-outline" @click="changeQuantity(item, -1)">-</button>
              <text class="qty">{{ item.quantity }}</text>
              <button class="btn-outline" @click="changeQuantity(item, 1)">+</button>
              <button class="btn-outline" @click="onRemove(item)">Remove</button>
            </view>
          </view>
        </view>

        <view class="summary">
          <text class="summary-text">Total: {{ formatPrice(cartTotal) }}</text>
          <button class="btn-primary" :loading="placing" @click="onPlaceOrder">Submit orders</button>
        </view>
      </view>
    </view>
  </AppShell>
</template>

<style scoped>
.panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.empty {
  padding: 16px 0;
  text-align: center;
}

.shop-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.shop-title {
  font-size: 12px;
  color: var(--text-muted);
}

.item-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
}

.item-meta {
  font-size: 12px;
  color: var(--text-muted);
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.qty {
  min-width: 24px;
  text-align: center;
}

.summary {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.summary-text {
  font-size: 14px;
  font-weight: 600;
}
</style>
