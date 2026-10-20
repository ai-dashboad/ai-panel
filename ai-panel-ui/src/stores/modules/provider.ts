import { defineStore } from 'pinia'
import { providerApi } from '@/api/provider'
import type {
  Provider,
  ProviderApiKey,
  CreateProviderRequest,
  CreateProviderApiKeyRequest,
  ProviderStatus
} from '@/types/provider'

interface ProviderState {
  providers: Provider[]
  apiKeys: ProviderApiKey[]
  status: ProviderStatus | null
  loading: boolean
  keysLoading: boolean
}

export const useProviderStore = defineStore('provider', {
  state: (): ProviderState => ({
    providers: [],
    apiKeys: [],
    status: null,
    loading: false,
    keysLoading: false,
  }),

  getters: {
    enabledProviders: (state) => state.providers.filter(p => p.enabled),

    getProviderById: (state) => (id: number) =>
      state.providers.find(p => p.id === id),

    getKeysByProvider: (state) => (providerId: number) =>
      state.apiKeys.filter(k => k.providerId === providerId),

    totalActiveKeys: (state) =>
      state.providers.reduce((sum, p) => sum + p.activeKeyCount, 0),
  },

  actions: {
    async fetchProviders() {
      this.loading = true
      try {
        const response = await providerApi.listProviders()
        this.providers = response.data
      } finally {
        this.loading = false
      }
    },

    async createProvider(data: CreateProviderRequest) {
      const response = await providerApi.createProvider(data)
      this.providers.push(response.data)
      return response.data
    },

    async updateProvider(id: number, data: CreateProviderRequest) {
      const response = await providerApi.updateProvider(id, data)
      const index = this.providers.findIndex(p => p.id === id)
      if (index !== -1) {
        this.providers[index] = response.data
      }
      return response.data
    },

    async deleteProvider(id: number) {
      await providerApi.deleteProvider(id)
      this.providers = this.providers.filter(p => p.id !== id)
      this.apiKeys = this.apiKeys.filter(k => k.providerId !== id)
    },

    async fetchApiKeys(providerId?: number) {
      this.keysLoading = true
      try {
        const response = await providerApi.listApiKeys(providerId)
        this.apiKeys = response.data
      } finally {
        this.keysLoading = false
      }
    },

    async createApiKey(data: CreateProviderApiKeyRequest) {
      const response = await providerApi.createApiKey(data)
      this.apiKeys.unshift(response.data)
      // Update provider key count
      const provider = this.providers.find(p => p.id === data.providerId)
      if (provider) {
        provider.keyCount++
        if (data.enabled !== false) {
          provider.activeKeyCount++
        }
      }
      return response.data
    },

    async updateApiKey(id: number, data: Partial<CreateProviderApiKeyRequest>) {
      const response = await providerApi.updateApiKey(id, data)
      const index = this.apiKeys.findIndex(k => k.id === id)
      if (index !== -1) {
        this.apiKeys[index] = response.data
      }
      return response.data
    },

    async deleteApiKey(id: number) {
      const key = this.apiKeys.find(k => k.id === id)
      await providerApi.deleteApiKey(id)
      this.apiKeys = this.apiKeys.filter(k => k.id !== id)
      // Update provider key count
      if (key) {
        const provider = this.providers.find(p => p.id === key.providerId)
        if (provider) {
          provider.keyCount--
          if (key.enabled && key.status === 'active') {
            provider.activeKeyCount--
          }
        }
      }
    },

    async resetApiKeyStatus(id: number) {
      await providerApi.resetApiKeyStatus(id)
      const key = this.apiKeys.find(k => k.id === id)
      if (key) {
        key.status = 'active'
        key.consecutiveErrors = 0
        key.lastErrorAt = null
        key.lastErrorMessage = null
      }
    },

    async fetchStatus() {
      const response = await providerApi.getProvidersStatus()
      this.status = response.data
    },

    async reloadKeyPool() {
      await providerApi.reloadKeyPool()
      await this.fetchProviders()
      await this.fetchApiKeys()
    },
  },
})
