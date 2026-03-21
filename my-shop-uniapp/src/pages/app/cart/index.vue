<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '../../../components/AppShell.vue'
import { cartItems, cartTotal, clearCart, removeFromCart, setCartItemQuantity } from '../../../store/cart'
import type { CartEntry } from '../../../store/cart'
import { getDefaultAddress } from '../../../api/address'
import { createOrder } from '../../../api/order'
import { sessionState } from '../../../auth/session'
import type { UserAddress } from '../../../types/domain'
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
const loadingAddress = ref(false)
const selectedAddress = ref<UserAddress | null>(null)
const userId = computed(() => sessionState.user?.id)

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

function formatAddress(address: UserAddress | null): string {
  if (!address) {
    return ''
  }
  return [address.province, address.city, address.district, address.street, address.detailAddress]
    .map((part) => part?.trim())
    .filter((part): part is string => Boolean(part))
    .join(' ')
}

async function loadDefaultAddress(): Promise<void> {
  if (loadingAddress.value) {
    return
  }
  if (typeof userId.value !== 'number') {
    selectedAddress.value = null
    return
  }

  loadingAddress.value = true
  try {
    selectedAddress.value = await getDefaultAddress(userId.value)
  } catch (error) {
    selectedAddress.value = null
    toast(error instanceof Error ? error.message : 'Failed to load the default address')
  } finally {
    loadingAddress.value = false
  }
}

function openAddressBook(): void {
  navigateTo(Routes.appAddresses, undefined, { requiresAuth: true })
}

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
  if (!selectedAddress.value) {
    toast('Add a default address before checkout')
    openAddressBook()
    return
  }

  const receiverAddress = formatAddress(selectedAddress.value)
  const receiverName = selectedAddress.value.consignee.trim()
  const receiverPhone = selectedAddress.value.phone.trim()

  if (!receiverName || !receiverPhone || !receiverAddress) {
    toast('The default address is incomplete')
    openAddressBook()
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
            price: item.price,
            receiverName,
            receiverPhone,
            receiverAddress
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

onShow(() => {
  void loadDefaultAddress()
})
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

      <view class="address-card">
        <view class="address-copy">
          <text class="address-title">Delivery address</text>
          <template v-if="selectedAddress">
            <text class="address-name">{{ selectedAddress.consignee }} · {{ selectedAddress.phone }}</text>
            <text class="address-text">{{ formatAddress(selectedAddress) }}</text>
          </template>
          <text v-else-if="loadingAddress" class="address-text">Loading default address...</text>
          <text v-else class="address-text">No default address is available.</text>
        </view>
        <button class="btn-outline" @click="openAddressBook">
          {{ selectedAddress ? 'Manage addresses' : 'Add address' }}
        </button>
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
          <button class="btn-primary" :loading="placing" :disabled="placing || !selectedAddress" @click="onPlaceOrder">
            Submit orders
          </button>
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

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.65);
}

.address-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.address-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-muted);
}

.address-name {
  font-size: 14px;
  font-weight: 600;
}

.address-text {
  font-size: 12px;
  color: var(--text-muted);
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
