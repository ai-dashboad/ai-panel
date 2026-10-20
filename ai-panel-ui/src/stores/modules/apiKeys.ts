import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ApiKey, CreateApiKeyRequest } from '@/types/apiKey'
import { apiKeyApi } from '@/api/apiKey'

export const useApiKeyStore = defineStore('apiKeys', () => {
  const keys = ref<ApiKey[]>([])
  const loading = ref(false)

  async function fetchKeys() {
    loading.value = true
    try {
      const res = await apiKeyApi.listKeys()
      keys.value = res.data.list || []
    } finally {
      loading.value = false
    }
  }

  async function createKey(data: CreateApiKeyRequest) {
    const res = await apiKeyApi.createKey(data)
    await fetchKeys()
    return res.data
  }

  async function deleteKey(id: string) {
    await apiKeyApi.deleteKey(id)
    keys.value = keys.value.filter(k => k.id !== id)
  }

  return {
    keys,
    loading,
    fetchKeys,
    createKey,
    deleteKey,
  }
})
