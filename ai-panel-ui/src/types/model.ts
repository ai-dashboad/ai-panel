export type PricingType = 'usage' | 'per_request' | 'task'
export type ViewMode = 'grid' | 'list'
export type PriceUnit = '1M' | '1K'

export interface ModelGroup {
  id?: number
  code: string
  name: string
  priceMultiplier: number
  description?: string
  enabled?: boolean
  sortOrder?: number
  modelCount?: number
  modelIds?: string[]
}

export interface ModelMetrics {
  modelId: string
  successRate: number
  avgLatencySeconds: number
  throughputTokensPerSecond: number
  requestCount: number
  recentStatuses: number[]
}

export interface CatalogModel {
  id: string
  displayName: string
  provider: string
  modelType: string
  contextWindow: number
  maxOutputTokens: number
  /** Cost per 1K tokens */
  inputPrice: number
  outputPrice: number
  features?: string[]
  deprecated?: boolean
  description?: string
  groupCode?: string
  groupName?: string
  priceMultiplier?: number
  sellInputPricePerMillion?: number
  sellOutputPricePerMillion?: number
  labels?: string[]
  pricingType?: PricingType
  metrics?: ModelMetrics
}
