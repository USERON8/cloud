export function getStorage<T>(key: string): T | null {
  try {
    const value = uni.getStorageSync(key)
    if (!value) {
      return null
    }
    return JSON.parse(value) as T
  } catch {
    return null
  }
}

export function setStorage<T>(key: string, value: T): void {
  uni.setStorageSync(key, JSON.stringify(value))
}

export function removeStorage(key: string): void {
  uni.removeStorageSync(key)
}
