export function toast(message: string, icon: 'none' | 'success' | 'loading' = 'none'): void {
  uni.showToast({ title: message, icon })
}

export function confirm(content: string, title = 'Confirm'): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => resolve(Boolean(res.confirm)),
      fail: () => resolve(false)
    })
  })
}

export function showLoading(title = 'Loading'): void {
  uni.showLoading({ title, mask: true })
}

export function hideLoading(): void {
  uni.hideLoading()
}
