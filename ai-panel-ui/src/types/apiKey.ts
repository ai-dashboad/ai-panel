export interface ApiKey {
  id: string
  keyPrefix: string
  name: string
  description?: string
  status: number
  allowedModels?: string[]
  rateLimitPerMinute?: number
  expiresAt?: string
  lastUsedAt?: string
  createdAt: string
}

export interface CreateApiKeyRequest {
  name: string
  description?: string
  allowedModels?: string[]
  rateLimitPerMinute?: number
  expiresAt?: string
}

export interface CreateApiKeyResponse {
  id: string
  keyPrefix: string
  fullKey: string  // Full key, only shown once
  name: string
  description?: string
  status: number
  allowedModels?: string[]
  rateLimitPerMinute?: number
  expiresAt?: string
  lastUsedAt?: string
  createdAt: string
}
