export const Routes = {
  market: '/pages/market/index',
  login: '/pages/login/index',
  error: '/pages/error/index',
  forbidden: '/pages/forbidden/index',
  oauthSuccess: '/pages/oauth/success',
  oauthError: '/pages/oauth/error',
  webview: '/pages/webview/index',
  appHome: '/pages/app/home/index',
  appCatalog: '/pages/app/catalog/index',
  appCatalogManage: '/pages/app/catalog-manage/index',
  appOrders: '/pages/app/orders/index',
  appOrdersManage: '/pages/app/orders-manage/index',
  appProfile: '/pages/app/profile/index',
  appAddresses: '/pages/app/addresses/index',
  appCart: '/pages/app/cart/index',
  appPayments: '/pages/app/payments/index',
  appStock: '/pages/app/stock/index',
  appMerchant: '/pages/app/merchant/index',
  appAdmin: '/pages/app/admin/index',
  appOps: '/pages/app/ops/index'
} as const

export type RoutePath = (typeof Routes)[keyof typeof Routes]
