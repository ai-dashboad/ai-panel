import { get, post } from './request'
import type { CatalogModel, ModelGroup, ModelMetrics } from '@/types/model'

export const modelApi = {
  list: (params?: { provider?: string; type?: string; keyword?: string; group?: string }) =>
    get<CatalogModel[]>('/models', { params }),

  getById: (modelId: string) => get<CatalogModel>(`/models/${encodeURIComponent(modelId)}`),

  listGroups: (enabledOnly = true) =>
    get<ModelGroup[]>('/model-groups', { params: { enabledOnly } }),

  getGroup: (code: string) => get<ModelGroup>(`/model-groups/${encodeURIComponent(code)}`),

  saveGroup: (data: Partial<ModelGroup> & { code: string; name: string }) =>
    post<ModelGroup>('/model-groups', data),

  healthMetrics: (hours = 24, sparkline = 24) =>
    get<Record<string, ModelMetrics>>('/stats/models/health', { params: { hours, sparkline } }),

  refreshCache: () => post<string>('/models/cache/refresh'),
}
