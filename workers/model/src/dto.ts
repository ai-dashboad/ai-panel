import { z } from 'zod'
import type { ModelRow, ProviderRow } from './db/schema'

// Ported from CreateModelRequest.java
export const createModelSchema = z.object({
  modelId: z.string().min(1, 'Model ID is required'),
  displayName: z.string().min(1, 'Display name is required'),
  providerCode: z.string().min(1, 'Provider code is required'),
  modelType: z.string().min(1, 'Model type is required'),
  contextWindow: z.number().int().optional(),
  maxOutputTokens: z.number().int().optional(),
  inputPrice: z.number(),
  outputPrice: z.number(),
  features: z.array(z.string()).optional(),
  fallbackChain: z.array(z.string()).optional(),
  description: z.string().optional(),
})
export type CreateModelInput = z.infer<typeof createModelSchema>

// Ported from UpdateModelRequest.java — all fields optional (partial update)
export const updateModelSchema = z.object({
  displayName: z.string().min(1).optional(),
  contextWindow: z.number().int().optional(),
  maxOutputTokens: z.number().int().optional(),
  inputPrice: z.number().optional(),
  outputPrice: z.number().optional(),
  features: z.array(z.string()).optional(),
  fallbackChain: z.array(z.string()).optional(),
  description: z.string().optional(),
  enabled: z.boolean().optional(),
  deprecated: z.boolean().optional(),
})
export type UpdateModelInput = z.infer<typeof updateModelSchema>

function parseJsonArray(value: string): string[] {
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export type GroupPricing = {
  groupCode: string
  groupName: string
  priceMultiplier: number
}

/** Catalog shape expected by ai-panel-ui Models ops page (dispatch ModelInfoResponse). */
export function toCatalogModelResponse(row: ModelRow, group?: GroupPricing | null) {
  const mult = group?.priceMultiplier ?? 1
  const sellIn = +(row.inputPrice * mult * 1000).toFixed(4)
  const sellOut = +(row.outputPrice * mult * 1000).toFixed(4)
  return {
    id: row.modelId,
    displayName: row.displayName,
    provider: row.providerCode,
    providerCode: row.providerCode,
    modelType: row.modelType,
    contextWindow: row.contextWindow,
    maxOutputTokens: row.maxOutputTokens,
    inputPrice: row.inputPrice,
    outputPrice: row.outputPrice,
    features: parseJsonArray(row.features),
    fallbackChain: parseJsonArray(row.fallbackChain),
    enabled: row.enabled,
    deprecated: row.deprecated,
    description: row.description,
    groupCode: group?.groupCode ?? null,
    groupName: group?.groupName ?? null,
    priceMultiplier: mult,
    sellInputPricePerMillion: sellIn,
    sellOutputPricePerMillion: sellOut,
    labels: ['chat', row.providerCode],
    pricingType: 'usage',
    createTime: row.createTime,
    updateTime: row.updateTime,
  }
}

// Mirrors ModelDetailResponse.java (legacy worker shape)
export function toModelDetailResponse(row: ModelRow, providerName?: string) {
  return {
    id: row.modelId,
    displayName: row.displayName,
    providerCode: row.providerCode,
    provider: row.providerCode,
    providerName: providerName ?? null,
    modelType: row.modelType,
    contextWindow: row.contextWindow,
    maxOutputTokens: row.maxOutputTokens,
    inputPrice: row.inputPrice,
    outputPrice: row.outputPrice,
    features: parseJsonArray(row.features),
    fallbackChain: parseJsonArray(row.fallbackChain),
    enabled: row.enabled,
    deprecated: row.deprecated,
    description: row.description,
    createTime: row.createTime,
    updateTime: row.updateTime,
  }
}

// Mirrors ProviderResponse.java — deliberately omits apiKey
export function toProviderResponse(row: ProviderRow, modelCount: number) {
  return {
    providerCode: row.providerCode,
    providerName: row.providerName,
    baseUrl: row.baseUrl,
    priority: row.priority,
    rateLimitPerMinute: row.rateLimitPerMinute,
    timeout: row.timeout,
    enabled: row.enabled,
    healthStatus: row.healthStatus === 1 ? 'HEALTHY' : 'UNKNOWN',
    lastHealthCheck: row.lastHealthCheck,
    modelCount,
  }
}
