import { get, post, put, del } from './request'
import type {
  PlatformConfig,
  ProductSku,
  ExternalOrder,
  DeliveryLog,
  CreatePlatformConfigRequest,
  CreateProductSkuRequest,
  OrderPageResult
} from '@/types/delivery'

export const deliveryApi = {
  // Platform Configuration
  listPlatforms: () => get<PlatformConfig[]>('/admin/platforms'),

  getPlatform: (id: number) => get<PlatformConfig>(`/admin/platforms/${id}`),

  createPlatform: (data: CreatePlatformConfigRequest) =>
    post<PlatformConfig>('/admin/platforms', data),

  updatePlatform: (id: number, data: CreatePlatformConfigRequest) =>
    put<PlatformConfig>(`/admin/platforms/${id}`, data),

  deletePlatform: (id: number) => del(`/admin/platforms/${id}`),

  getWebhookUrl: (platform: string) =>
    get<string>(`/admin/platforms/webhook-url/${platform}`),

  // Product SKUs
  listSkus: (platform?: string) => {
    const params = platform ? { platform } : {}
    return get<ProductSku[]>('/admin/skus', { params })
  },

  getSku: (id: number) => get<ProductSku>(`/admin/skus/${id}`),

  createSku: (data: CreateProductSkuRequest) =>
    post<ProductSku>('/admin/skus', data),

  updateSku: (id: number, data: CreateProductSkuRequest) =>
    put<ProductSku>(`/admin/skus/${id}`, data),

  deleteSku: (id: number) => del(`/admin/skus/${id}`),

  // External Orders
  listOrders: (params: {
    pageNum?: number
    pageSize?: number
    platform?: string
    status?: string
    keyword?: string
  }) => get<OrderPageResult>('/admin/orders', { params }),

  getOrder: (id: number) => get<ExternalOrder>(`/admin/orders/${id}`),

  getOrderLogs: (id: number) => get<DeliveryLog[]>(`/admin/orders/${id}/logs`),

  retryOrder: (id: number) => post(`/admin/orders/${id}/retry`),

  cancelOrder: (id: number) => post(`/admin/orders/${id}/cancel`),

  retryFailedOrders: (maxRetryCount?: number) =>
    post<number>('/admin/orders/retry-failed', null, { params: { maxRetryCount } }),
}
