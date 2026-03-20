import { computed, reactive } from 'vue'
import { getStorage, removeStorage, setStorage } from '../utils/storage'

export interface CartEntry {
  productId: number
  skuId: number
  productName: string
  price: number
  quantity: number
  shopId: number
}

const CART_KEY = 'shop.cart'

const state = reactive<{ items: CartEntry[] }>({ items: [] })

function persist(): void {
  setStorage(CART_KEY, state.items)
}

export function hydrateCartFromStorage(): void {
  const parsed = getStorage<CartEntry[]>(CART_KEY)
  if (!parsed) {
    return
  }
  if (Array.isArray(parsed)) {
    state.items = parsed.filter(
      (item) =>
        typeof item.productId === 'number' &&
        typeof item.skuId === 'number' &&
        typeof item.shopId === 'number' &&
        item.shopId > 0 &&
        typeof item.productName === 'string' &&
        item.productName.trim().length > 0 &&
        typeof item.price === 'number' &&
        item.price > 0 &&
        typeof item.quantity === 'number' &&
        item.quantity > 0
    )
  }
}

export function addToCart(entry: Omit<CartEntry, 'quantity'> & { quantity?: number }): void {
  const qty = Math.max(1, entry.quantity ?? 1)
  const existing = state.items.find((item) => item.productId === entry.productId && item.skuId === entry.skuId)
  if (existing) {
    existing.quantity += qty
  } else {
    state.items.push({
      ...entry,
      quantity: qty
    })
  }
  persist()
}

export function removeFromCart(productId: number, skuId: number): void {
  const idx = state.items.findIndex((item) => item.productId === productId && item.skuId === skuId)
  if (idx !== -1) {
    state.items.splice(idx, 1)
    persist()
  }
}

export function setCartItemQuantity(productId: number, skuId: number, quantity: number): void {
  const item = state.items.find((i) => i.productId === productId && i.skuId === skuId)
  if (!item) return
  if (quantity <= 0) {
    removeFromCart(productId, skuId)
  } else {
    item.quantity = quantity
    persist()
  }
}

export function clearCart(): void {
  state.items = []
  removeStorage(CART_KEY)
}

export function removeItemsByShops(shopIds: number[]): void {
  if (shopIds.length === 0) return
  const set = new Set(shopIds)
  const next = state.items.filter((item) => !set.has(item.shopId))
  if (next.length !== state.items.length) {
    state.items = next
    persist()
  }
}

export const cartItems = computed(() => state.items)
export const cartCount = computed(() => state.items.reduce((sum, i) => sum + i.quantity, 0))
export const cartTotal = computed(() => state.items.reduce((sum, i) => sum + i.price * i.quantity, 0))
