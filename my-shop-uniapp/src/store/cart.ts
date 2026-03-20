import { computed, reactive } from 'vue'
import { sessionState, subscribeSessionChange } from '../auth/session'
import type { UserInfo } from '../types/domain'
import { getStorage, removeStorage, setStorage } from '../utils/storage'

export interface CartEntry {
  productId: number
  skuId: number
  productName: string
  price: number
  quantity: number
  shopId: number
}

const LEGACY_CART_KEY = 'shop.cart'
const CART_KEY_PREFIX = 'shop.cart'
const GUEST_CART_KEY = `${CART_KEY_PREFIX}.guest`

const state = reactive<{ items: CartEntry[] }>({ items: [] })
let activeCartKey = resolveCartStorageKey(sessionState.user)
let hasHydrated = false

function resolveCartStorageKey(user: Pick<UserInfo, 'id' | 'username'> | null | undefined): string {
  if (typeof user?.id === 'number' && user.id > 0) {
    return `${CART_KEY_PREFIX}.user.${user.id}`
  }
  const username = typeof user?.username === 'string' ? user.username.trim().toLowerCase() : ''
  if (username) {
    return `${CART_KEY_PREFIX}.user.${encodeURIComponent(username)}`
  }
  return GUEST_CART_KEY
}

function normalizeCartEntries(parsed: CartEntry[] | null): CartEntry[] {
  if (!Array.isArray(parsed)) {
    return []
  }
  return parsed.filter(
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

function persist(): void {
  setStorage(activeCartKey, state.items)
}

function readCart(key: string): CartEntry[] {
  return normalizeCartEntries(getStorage<CartEntry[]>(key))
}

function loadCart(key: string): void {
  activeCartKey = key
  state.items = readCart(key)
}

function migrateLegacyCart(targetKey: string): void {
  if (targetKey === LEGACY_CART_KEY || getStorage<CartEntry[]>(targetKey)) {
    return
  }

  const legacyItems = readCart(LEGACY_CART_KEY)
  if (legacyItems.length === 0) {
    removeStorage(LEGACY_CART_KEY)
    return
  }

  setStorage(targetKey, legacyItems)
  removeStorage(LEGACY_CART_KEY)
}

function syncCartStorage(user: UserInfo | null): void {
  const nextKey = resolveCartStorageKey(user)
  if (!hasHydrated) {
    activeCartKey = nextKey
    return
  }
  if (nextKey === activeCartKey) {
    return
  }
  loadCart(nextKey)
}

export function hydrateCartFromStorage(): void {
  const targetKey = resolveCartStorageKey(sessionState.user)
  migrateLegacyCart(targetKey)
  loadCart(targetKey)
  hasHydrated = true
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
  removeStorage(activeCartKey)
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

subscribeSessionChange((user) => {
  syncCartStorage(user)
})
