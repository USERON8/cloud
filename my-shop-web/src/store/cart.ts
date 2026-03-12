import { computed, reactive } from 'vue'

export interface CartEntry {
  productId: number
  productName: string
  price: number
  quantity: number
  shopId: number
}

const CART_KEY = 'shop.cart'

const state = reactive<{ items: CartEntry[] }>({ items: [] })

function persist(): void {
  localStorage.setItem(CART_KEY, JSON.stringify(state.items))
}

export function hydrateCartFromStorage(): void {
  try {
    const raw = localStorage.getItem(CART_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw) as CartEntry[]
    if (Array.isArray(parsed)) {
      state.items = parsed.filter(
        (item) =>
          typeof item.productId === 'number' &&
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
  } catch {
    localStorage.removeItem(CART_KEY)
  }
}

export function addToCart(entry: Omit<CartEntry, 'quantity'> & { quantity?: number }): void {
  const qty = Math.max(1, entry.quantity ?? 1)
  const existing = state.items.find((item) => item.productId === entry.productId)
  if (existing) {
    existing.quantity += qty
  } else {
    state.items.push({
      productId: entry.productId,
      productName: entry.productName,
      price: entry.price,
      quantity: qty,
      shopId: entry.shopId
    })
  }
  persist()
}

export function removeFromCart(productId: number): void {
  const idx = state.items.findIndex((item) => item.productId === productId)
  if (idx !== -1) {
    state.items.splice(idx, 1)
    persist()
  }
}

export function setCartItemQuantity(productId: number, quantity: number): void {
  const item = state.items.find((i) => i.productId === productId)
  if (!item) return
  if (quantity <= 0) {
    removeFromCart(productId)
  } else {
    item.quantity = quantity
    persist()
  }
}

export function clearCart(): void {
  state.items = []
  localStorage.removeItem(CART_KEY)
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
