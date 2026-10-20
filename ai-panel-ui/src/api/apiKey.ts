import { get, post, put, del } from './request'
import type { ApiKey, CreateApiKeyRequest, CreateApiKeyResponse } from '@/types/apiKey'

export const apiKeyApi = {
  listKeys: () => get<{ list: ApiKey[], total: number }>('/keys'),

  getKey: (id: string) => get<ApiKey>(`/keys/${id}`),

  createKey: (data: CreateApiKeyRequest) => post<CreateApiKeyResponse>('/keys', data),

  updateKey: (id: string, data: Partial<CreateApiKeyRequest>) => put<ApiKey>(`/keys/${id}`, data),

  deleteKey: (id: string) => del(`/keys/${id}`),

  regenerateKey: (id: string) => post<CreateApiKeyResponse>(`/keys/${id}/regenerate`),

  // Ensure user has a default API key (auto-generate if needed)
  ensureDefaultKey: () => post<CreateApiKeyResponse>('/keys/ensure-default'),
}
