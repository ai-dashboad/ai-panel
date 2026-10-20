import { z } from 'zod'
import type { ApiKeyRow } from './db/schema'

const modelPattern = /^[a-zA-Z0-9._-]+$/

// Ported from CreateApiKeyRequest.java
export const createApiKeySchema = z.object({
  name: z.string().min(1, 'Key name is required').max(100, 'Name must be 1-100 characters'),
  allowedModels: z.array(z.string().regex(modelPattern)).max(20, 'Maximum 20 models allowed').optional(),
  rateLimitPerMinute: z.number().int().min(1).max(10000).optional(),
  expiresAt: z.coerce.date().refine((d) => d > new Date(), 'Expiration date must be in the future').optional(),
  description: z.string().max(500, 'Description must not exceed 500 characters').optional(),
})
export type CreateApiKeyInput = z.infer<typeof createApiKeySchema>

// Ported from UpdateApiKeyRequest.java — all fields optional (partial update)
export const updateApiKeySchema = z.object({
  name: z.string().min(1).max(100, 'Name must be 1-100 characters').optional(),
  allowedModels: z.array(z.string().regex(modelPattern)).max(20, 'Maximum 20 models allowed').optional(),
  rateLimitPerMinute: z.number().int().min(1).max(10000).optional(),
  expiresAt: z.coerce.date().nullable().optional(),
  description: z.string().max(500).optional(),
  status: z.union([z.literal(0), z.literal(1)]).optional(),
})
export type UpdateApiKeyInput = z.infer<typeof updateApiKeySchema>

export const validateApiKeySchema = z.object({
  apiKey: z.string().min(1, 'API Key is required'),
  model: z.string().optional(),
})

function parseJsonArray(value: string | null): string[] | null {
  if (!value) return null
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

// Mirrors ApiKeyResponse.java — fullKey only populated on create/regenerate
export function toApiKeyResponse(row: ApiKeyRow, fullKey?: string) {
  return {
    id: row.keyId,
    keyPrefix: row.keyPrefix,
    fullKey: fullKey ?? null,
    name: row.name,
    description: row.description,
    status: row.status,
    allowedModels: parseJsonArray(row.allowedModels),
    rateLimitPerMinute: row.rateLimitOverride,
    expiresAt: row.expiresAt,
    lastUsedAt: row.lastUsedAt,
    createdAt: row.createdAt,
  }
}
