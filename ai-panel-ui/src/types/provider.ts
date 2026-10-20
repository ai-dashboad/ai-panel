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
  /** Optional on create; not needed for vllm / ollama / openai_compatible */
  apiKey?: string
  /** Optional model ids to register on create, e.g. ["Qwen/Qwen3-32B"] */
  models?: string[]
}

export interface ProviderModel {
  id: number
  providerId: number
  modelId: string
  modelName: string
  enabled: boolean
  maxTokens: number | null
  contextWindow: number | null
  supportsVision: boolean | null
  supportsFunctionCall: boolean | null
  createdAt: string
}

export interface ProviderModelRequest {
  modelId: string
  modelName?: string
  enabled?: boolean
  maxTokens?: number
  contextWindow?: number
  supportsVision?: boolean
  supportsFunctionCall?: boolean
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
