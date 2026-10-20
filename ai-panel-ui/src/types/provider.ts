export interface Provider {
  id: number
  name: string
  type: string
  baseUrl: string
  enabled: boolean
  priority: number
  timeoutSeconds: number
  retryCount: number
  description: string
  createdAt: string
  updatedAt: string
  keyCount: number
  activeKeyCount: number
  models: string[]
}

export interface ProviderApiKey {
  id: number
  providerId: number
  providerName: string
  providerType: string
  keyName: string
  keyPrefix: string
  enabled: boolean
  weight: number
  rateLimitPerMinute: number | null
  dailyQuota: number | null
  usedQuotaToday: number
  totalRequests: number
  totalTokens: number
  lastUsedAt: string | null
  lastErrorAt: string | null
  lastErrorMessage: string | null
  consecutiveErrors: number
  status: string
  expiresAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateProviderRequest {
  name: string
  type: string
  baseUrl?: string
  enabled?: boolean
  priority?: number
  timeoutSeconds?: number
  retryCount?: number
  description?: string
}

export interface CreateProviderApiKeyRequest {
  providerId: number
  keyName: string
  apiKey: string
  enabled?: boolean
  weight?: number
  rateLimitPerMinute?: number
  dailyQuota?: number
  expiresAt?: string
}

export interface ProviderStatus {
  [providerType: string]: {
    keys: Array<{
      id: number
      name: string
      weight: number
      status: string
      dailyQuota: number | null
      usedQuotaToday: number
      consecutiveErrors: number
    }>
    totalKeys: number
    activeKeys: number
  }
}
