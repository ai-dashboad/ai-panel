export interface PlatformConfig {
  id: number
  platform: string
  name: string
  appKey: string
  webhookUrl: string
  deliveryTemplate: string
  autoCreateUser: boolean
  autoDelivery: boolean
  enabled: boolean
  config: string | null
  createdAt: string
  updatedAt: string
}

export interface ProductSku {
  id: number
  platform: string | null
  sku: string
  name: string
  description: string | null
  quotaType: string
  quotaTokens: number | null
  quotaRequests: number | null
  validityDays: number
  allowedModels: string | null
  rateLimit: number | null
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface ExternalOrder {
  id: number
  platform: string
  externalOrderId: string
  externalItemId: string | null
  sku: string
  buyerNick: string
  buyerPhone: string | null
  buyerEmail: string | null
  buyerMessage: string | null
  quantity: number
  amount: number
  userId: number | null
  apiKeyId: number | null
  status: 'pending' | 'processing' | 'delivered' | 'failed' | 'cancelled'
  deliveryMethod: string | null
  deliveryContent: string | null
  deliveryTime: string | null
  failReason: string | null
  retryCount: number
  createdAt: string
  updatedAt: string
}

export interface DeliveryLog {
  id: number
  orderId: number
  action: string
  status: string
  message: string
  createdAt: string
}

export interface CreatePlatformConfigRequest {
  platform: string
  name: string
  appKey?: string
  appSecret?: string
  webhookSecret?: string
  deliveryTemplate?: string
  autoCreateUser?: boolean
  autoDelivery?: boolean
  enabled?: boolean
  config?: string
}

export interface CreateProductSkuRequest {
  platform?: string
  sku: string
  name: string
  description?: string
  quotaType?: string
  quotaTokens?: number
  quotaRequests?: number
  validityDays?: number
  allowedModels?: string
  rateLimit?: number
  enabled?: boolean
}

export interface OrderPageResult {
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
  list: ExternalOrder[]
  hasNext: boolean
  hasPrevious: boolean
}
