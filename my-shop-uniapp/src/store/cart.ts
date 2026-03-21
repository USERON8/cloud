import { computed } from 'vue'
import { defineStore } from 'pinia'
import { sessionState, subscribeSessionChange } from '../auth/session'
import { pinia } from '../stores/pinia'
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

function readCart(key: string): CartEntry[] {
  return normalizeCartEntries(getStorage<CartEntry[]>(key))
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [] as CartEntry[],
    activeCartKey: resolveCartStorageKey(sessionState.user),
    hasHydrated: false
  }),
  getters: {
    cartCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    cartTotal: (state) => state.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
  },
  actions: {
    persist(): void {
      setStorage(this.activeCartKey, this.items)
    },
    loadCart(key: string): void {
      this.activeCartKey = key
      this.items = readCart(key)
    },
    migrateLegacyCart(targetKey: string): void {
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
    },
    syncCartStorage(user: UserInfo | null): void {
      const nextKey = resolveCartStorageKey(user)
      if (!this.hasHydrated) {
        this.activeCartKey = nextKey
        return
      }
      if (nextKey === this.activeCartKey) {
        return
      }
      this.loadCart(nextKey)
    },
    hydrateFromStorage(): void {
      const targetKey = resolveCartStorageKey(sessionState.user)
      this.migrateLegacyCart(targetKey)
      this.loadCart(targetKey)
      this.hasHydrated = true
    },
    add(entry: Omit<CartEntry, 'quantity'> & { quantity?: number }): void {
      const qty = Math.max(1, entry.quantity ?? 1)
      const existing = this.items.find((item) => item.productId === entry.productId && item.skuId === entry.skuId)
      if (existing) {
        existing.quantity += qty
      } else {
        this.items.push({
          ...entry,
          quantity: qty
        })
      }
      this.persist()
    },
    remove(productId: number, skuId: number): void {
      const idx = this.items.findIndex((item) => item.productId === productId && item.skuId === skuId)
      if (idx !== -1) {
        this.items.splice(idx, 1)
        this.persist()
      }
    },
    setQuantity(productId: number, skuId: number, quantity: number): void {
      const item = this.items.find((candidate) => candidate.productId === productId && candidate.skuId === skuId)
      if (!item) {
        return
      }
      if (quantity <= 0) {
        this.remove(productId, skuId)
        return
      }
      item.quantity = quantity
      this.persist()
    },
    clear(): void {
      this.items = []
      removeStorage(this.activeCartKey)
    },
    removeItemsByShops(shopIds: number[]): void {
      if (shopIds.length === 0) {
        return
      }
      const shopSet = new Set(shopIds)
      const nextItems = this.items.filter((item) => !shopSet.has(item.shopId))
      if (nextItems.length !== this.items.length) {
        this.items = nextItems
        this.persist()
      }
    }
  }
})

export const cartStore = useCartStore(pinia)

export function hydrateCartFromStorage(): void {
  cartStore.hydrateFromStorage()
}

export function addToCart(entry: Omit<CartEntry, 'quantity'> & { quantity?: number }): void {
  cartStore.add(entry)
}

export function removeFromCart(productId: number, skuId: number): void {
  cartStore.remove(productId, skuId)
}

export function setCartItemQuantity(productId: number, skuId: number, quantity: number): void {
  cartStore.setQuantity(productId, skuId, quantity)
}

export function clearCart(): void {
  cartStore.clear()
}

export function removeItemsByShops(shopIds: number[]): void {
  cartStore.removeItemsByShops(shopIds)
}

export const cartItems = computed(() => cartStore.items)
export const cartCount = computed(() => cartStore.cartCount)
export const cartTotal = computed(() => cartStore.cartTotal)

subscribeSessionChange((user) => {
  cartStore.syncCartStorage(user)
})
