<script setup lang="ts">
import { computed, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { cartItems, cartTotal, clearCart, removeFromCart, removeItemsByShops, setCartItemQuantity } from '../../../store/cart'
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
  const ok = await confirm(`确认移除 ${item.productName}？`)
  if (!ok) return
  removeFromCart(item.productId, item.skuId)
  toast('已移除', 'success')
}

async function onClearCart(): Promise<void> {
  const ok = await confirm('确认清空购物车？')
  if (!ok) return
  clearCart()
  toast('购物车已清空', 'success')
}

async function onPlaceOrder(): Promise<void> {
  if (shopGroups.value.length === 0) {
    toast('?????')
    return
  }
  const ok = await confirm(`???? ${shopGroups.value.length} ????`)
  if (!ok) return

  placing.value = true
  const successShops: number[] = []
  const failedShops: number[] = []

  for (const group of shopGroups.value) {
    for (const item of group.items) {
      if (typeof item.skuId !== 'number') {
        failedShops.push(group.shopId)
        toast(`?? ${group.shopId}??? SKU ??`)
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
        successShops.push(group.shopId)
      } catch (error) {
        failedShops.push(group.shopId)
        const msg = error instanceof Error ? error.message : '??????'
        toast(`?? ${group.shopId}?${msg}`)
      }
    }
  }

  placing.value = false

  if (successShops.length > 0) {
    removeItemsByShops(successShops)
    toast(`???? ${successShops.length} ?`, 'success')
    if (failedShops.length === 0) {
      navigateTo(Routes.appOrders, undefined, { requiresAuth: true })
      return
    }
  }

  if (failedShops.length > 0) {
    toast(`?????${failedShops.join(', ')}`)
  }
}
</script>

<template>
  <AppShell title="Cart">
    <view class="panel glass-card">
      <view class="header">
        <text class="section-title">购物车</text>
        <view class="header-actions">
          <button class="btn-outline" @click="navigateTo(Routes.appCatalog, undefined, { requiresAuth: true })">
            继续购物
          </button>
          <button v-if="cartItems.length > 0" class="btn-outline" @click="onClearCart">清空</button>
        </view>
      </view>

      <view v-if="cartItems.length === 0" class="empty">
        <text class="text-muted">购物车为空</text>
      </view>

      <view v-else>
        <view v-for="group in shopGroups" :key="group.shopId" class="shop-group">
          <text class="shop-title">店铺 {{ group.shopId }} · 小计 {{ formatPrice(group.subtotal) }}</text>
          <view v-for="item in group.items" :key="item.productId" class="item-row">
            <view class="item-info">
              <text class="item-name">{{ item.productName }}</text>
              <text class="item-meta">{{ formatPrice(item.price) }}</text>
            </view>
            <view class="item-actions">
              <button class="btn-outline" @click="changeQuantity(item, -1)">-</button>
              <text class="qty">{{ item.quantity }}</text>
              <button class="btn-outline" @click="changeQuantity(item, 1)">+</button>
              <button class="btn-outline" @click="onRemove(item)">移除</button>
            </view>
          </view>
        </view>

        <view class="summary">
          <text class="summary-text">合计：{{ formatPrice(cartTotal) }}</text>
          <button class="btn-primary" :loading="placing" @click="onPlaceOrder">提交订单</button>
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
