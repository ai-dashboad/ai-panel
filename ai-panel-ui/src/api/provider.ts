import { get, post, put, del } from './request'
import type {
  Provider,
  ProviderApiKey,
  CreateProviderRequest,
  CreateProviderApiKeyRequest,
  ProviderStatus,
  ProviderModel,
  ProviderModelRequest
} from '@/types/provider'

export const providerApi = {
  // Provider configuration
  listProviders: () => get<Provider[]>('/providers'),

  getProvider: (id: number) => get<Provider>(`/providers/${id}`),

  createProvider: (data: CreateProviderRequest) => post<Provider>('/providers', data),

  updateProvider: (id: number, data: CreateProviderRequest) => put<Provider>(`/providers/${id}`, data),

  deleteProvider: (id: number) => del(`/providers/${id}`),

  // Models served by a provider (any model id, e.g. Qwen/Qwen3-32B)
  listModels: (providerId: number) => get<ProviderModel[]>(`/providers/${providerId}/models`),

  addModel: (providerId: number, data: ProviderModelRequest) =>
    post<ProviderModel>(`/providers/${providerId}/models`, data),

  updateModel: (providerId: number, modelPk: number, data: ProviderModelRequest) =>
    put<ProviderModel>(`/providers/${providerId}/models/${modelPk}`, data),

  deleteModel: (providerId: number, modelPk: number) => del(`/providers/${providerId}/models/${modelPk}`),

  // Ask the upstream server which models it serves (GET {baseUrl}/models); nothing is saved
  discoverModels: (providerId: number) => get<string[]>(`/providers/${providerId}/models/discover`),

  // API Keys
  listApiKeys: (providerId?: number) => {
    const params = providerId ? { providerId } : {}
    return get<ProviderApiKey[]>('/providers/keys', { params })
  },

  getApiKey: (id: number) => get<ProviderApiKey>(`/providers/keys/${id}`),

  createApiKey: (data: CreateProviderApiKeyRequest) => post<ProviderApiKey>('/providers/keys', data),

  updateApiKey: (id: number, data: Partial<CreateProviderApiKeyRequest>) =>
    put<ProviderApiKey>(`/providers/keys/${id}`, data),

  deleteApiKey: (id: number) => del(`/providers/keys/${id}`),

  resetApiKeyStatus: (id: number) => post(`/providers/keys/${id}/reset`),

  // Status and operations
  getProvidersStatus: () => get<ProviderStatus>('/providers/status'),

  reloadKeyPool: () => post('/providers/reload'),

  // Validate provider API key
  validateKey: (data: { type: string; apiKey: string }) =>
    post<{ valid: boolean; balance?: string; message?: string }>('/providers/validate-key', data),

  // Quick setup - configure provider with just API key
  quickSetup: (data: { type: string; apiKey: string }) =>
    post<Provider>('/providers/quick-setup', data),

  // Batch setup multiple providers
  batchSetup: (providers: Array<{ type: string; apiKey: string }>) =>
    post<Provider[]>('/providers/batch-setup', { providers }),
}
