<template>
  <div class="providers-page">
    <div class="page-header">
      <h1>Provider Configuration</h1>
      <div class="header-actions">
        <el-button @click="handleReload" :loading="reloading">
          <el-icon><Refresh /></el-icon>
          Reload Pool
        </el-button>
        <el-button type="primary" @click="showAddKeyDialog = true">
          <el-icon><Plus /></el-icon>
          Add API Key
        </el-button>
      </div>
    </div>

    <!-- Provider Cards -->
    <div class="providers-grid" v-loading="providerStore.loading">
      <div
        v-for="provider in providerStore.providers"
        :key="provider.id"
        class="provider-card"
        :class="{ disabled: !provider.enabled }"
      >
        <div class="provider-header">
          <div class="provider-info">
            <div class="provider-icon" :class="provider.type">
              {{ getProviderInitial(provider.type) }}
            </div>
            <div>
              <h3>{{ provider.name }}</h3>
              <span class="provider-type">{{ provider.type }}</span>
            </div>
          </div>
          <el-switch
            v-model="provider.enabled"
            @change="toggleProvider(provider)"
          />
        </div>

        <div class="provider-stats">
          <div class="stat">
            <span class="stat-value">{{ provider.activeKeyCount }}</span>
            <span class="stat-label">Active Keys</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ provider.keyCount }}</span>
            <span class="stat-label">Total Keys</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ provider.models.length }}</span>
            <span class="stat-label">Models</span>
          </div>
        </div>

        <div class="provider-models">
          <el-tag
            v-for="model in provider.models.slice(0, 3)"
            :key="model"
            size="small"
            type="info"
          >
            {{ model }}
          </el-tag>
          <el-tag v-if="provider.models.length > 3" size="small" type="info">
            +{{ provider.models.length - 3 }} more
          </el-tag>
        </div>

        <div class="provider-actions">
          <el-button size="small" @click="viewProviderKeys(provider)">
            <el-icon><Key /></el-icon>
            Manage Keys
          </el-button>
          <el-button size="small" @click="editProvider(provider)">
            <el-icon><Edit /></el-icon>
            Edit
          </el-button>
        </div>
      </div>
    </div>

    <!-- API Keys Section -->
    <div class="keys-section">
      <div class="section-header">
        <h2>
          API Keys
          <el-tag v-if="selectedProvider" type="primary" class="filter-tag">
            {{ selectedProvider.name }}
            <el-icon class="close-icon" @click="clearProviderFilter"><Close /></el-icon>
          </el-tag>
        </h2>
      </div>

      <el-table
        :data="filteredKeys"
        v-loading="providerStore.keysLoading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="keyName" label="Name" min-width="120" />
        <el-table-column label="Provider" min-width="100">
          <template #default="{ row }">
            <span class="provider-badge" :class="row.providerType">
              {{ row.providerName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Key" min-width="140">
          <template #default="{ row }">
            <code class="key-display">{{ row.keyPrefix }}</code>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Weight" width="80" align="center">
          <template #default="{ row }">
            {{ row.weight }}
          </template>
        </el-table-column>
        <el-table-column label="Usage" min-width="120">
          <template #default="{ row }">
            <div class="usage-info">
              <span>{{ formatNumber(row.totalRequests) }} reqs</span>
              <span class="usage-tokens">{{ formatNumber(row.totalTokens) }} tokens</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Errors" width="80" align="center">
          <template #default="{ row }">
            <el-badge
              :value="row.consecutiveErrors"
              :hidden="row.consecutiveErrors === 0"
              :type="row.consecutiveErrors > 0 ? 'danger' : 'info'"
            >
              <span>{{ row.consecutiveErrors }}</span>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="180" fixed="right">
          <template #default="{ row }">
            <el-button-group size="small">
              <el-button
                v-if="row.status !== 'active'"
                type="success"
                @click="resetKey(row)"
              >
                Reset
              </el-button>
              <el-button type="primary" @click="editKey(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button type="danger" @click="confirmDeleteKey(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add API Key Dialog -->
    <el-dialog
      v-model="showAddKeyDialog"
      title="Add API Key"
      width="500px"
      @closed="resetKeyForm"
    >
      <el-form
        ref="keyFormRef"
        :model="keyForm"
        :rules="keyRules"
        label-position="top"
      >
        <el-form-item label="Provider" prop="providerId">
          <el-select v-model="keyForm.providerId" placeholder="Select provider" style="width: 100%">
            <el-option
              v-for="provider in providerStore.providers"
              :key="provider.id"
              :label="provider.name"
              :value="provider.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Key Name" prop="keyName">
          <el-input v-model="keyForm.keyName" placeholder="e.g., Production Key 1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="keyForm.apiKey"
            type="password"
            show-password
            placeholder="Enter your API key"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Weight" prop="weight">
              <el-input-number
                v-model="keyForm.weight"
                :min="1"
                :max="1000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Rate Limit (req/min)">
              <el-input-number
                v-model="keyForm.rateLimitPerMinute"
                :min="1"
                :max="10000"
                style="width: 100%"
                placeholder="Default"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Daily Quota (tokens)">
          <el-input-number
            v-model="keyForm.dailyQuota"
            :min="0"
            :step="100000"
            style="width: 100%"
            placeholder="Unlimited"
          />
        </el-form-item>
        <el-form-item label="Enabled">
          <el-switch v-model="keyForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddKeyDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveKey">
          {{ editingKey ? 'Update' : 'Add' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Edit Provider Dialog -->
    <el-dialog
      v-model="showEditProviderDialog"
      title="Edit Provider"
      width="500px"
    >
      <el-form
        ref="providerFormRef"
        :model="providerForm"
        :rules="providerRules"
        label-position="top"
      >
        <el-form-item label="Name" prop="name">
          <el-input v-model="providerForm.name" />
        </el-form-item>
        <el-form-item label="Type" prop="type">
          <el-input v-model="providerForm.type" disabled />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="providerForm.baseUrl" placeholder="Default API URL" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Priority">
              <el-input-number
                v-model="providerForm.priority"
                :min="1"
                :max="1000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Timeout (s)">
              <el-input-number
                v-model="providerForm.timeoutSeconds"
                :min="1"
                :max="300"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Retries">
              <el-input-number
                v-model="providerForm.retryCount"
                :min="0"
                :max="10"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Description">
          <el-input
            v-model="providerForm.description"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditProviderDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="savingProvider" @click="handleSaveProvider">
          Save
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useProviderStore } from '@/stores/modules/provider'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Provider, ProviderApiKey } from '@/types/provider'

const providerStore = useProviderStore()

const showAddKeyDialog = ref(false)
const showEditProviderDialog = ref(false)
const saving = ref(false)
const savingProvider = ref(false)
const reloading = ref(false)
const selectedProvider = ref<Provider | null>(null)
const editingKey = ref<ProviderApiKey | null>(null)
const editingProvider = ref<Provider | null>(null)

const keyFormRef = ref<FormInstance>()
const providerFormRef = ref<FormInstance>()

const keyForm = reactive({
  providerId: null as number | null,
  keyName: '',
  apiKey: '',
  enabled: true,
  weight: 100,
  rateLimitPerMinute: null as number | null,
  dailyQuota: null as number | null,
})

const providerForm = reactive({
  name: '',
  type: '',
  baseUrl: '',
  priority: 100,
  timeoutSeconds: 60,
  retryCount: 3,
  description: '',
})

const keyRules: FormRules = {
  providerId: [{ required: true, message: 'Please select a provider', trigger: 'change' }],
  keyName: [{ required: true, message: 'Please enter a key name', trigger: 'blur' }],
  apiKey: [{ required: true, message: 'Please enter the API key', trigger: 'blur' }],
}

const providerRules: FormRules = {
  name: [{ required: true, message: 'Please enter provider name', trigger: 'blur' }],
}

const filteredKeys = computed(() => {
  if (selectedProvider.value) {
    return providerStore.apiKeys.filter(k => k.providerId === selectedProvider.value?.id)
  }
  return providerStore.apiKeys
})

function getProviderInitial(type: string): string {
  const initials: Record<string, string> = {
    openai: 'O',
    anthropic: 'A',
    deepseek: 'D',
    moonshot: 'M',
    google: 'G',
  }
  return initials[type] || type.charAt(0).toUpperCase()
}

function getStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  const types: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    active: 'success',
    rate_limited: 'warning',
    error: 'danger',
    disabled: 'info',
  }
  return types[status] || 'info'
}

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function viewProviderKeys(provider: Provider) {
  selectedProvider.value = provider
}

function clearProviderFilter() {
  selectedProvider.value = null
}

async function toggleProvider(provider: Provider) {
  try {
    await providerStore.updateProvider(provider.id, {
      name: provider.name,
      type: provider.type,
      enabled: provider.enabled,
    })
    ElMessage.success(`Provider ${provider.enabled ? 'enabled' : 'disabled'}`)
  } catch (error: any) {
    provider.enabled = !provider.enabled // Revert
    ElMessage.error(error.message || 'Failed to update provider')
  }
}

function editProvider(provider: Provider) {
  editingProvider.value = provider
  Object.assign(providerForm, {
    name: provider.name,
    type: provider.type,
    baseUrl: provider.baseUrl || '',
    priority: provider.priority,
    timeoutSeconds: provider.timeoutSeconds,
    retryCount: provider.retryCount,
    description: provider.description || '',
  })
  showEditProviderDialog.value = true
}

async function handleSaveProvider() {
  const valid = await providerFormRef.value?.validate().catch(() => false)
  if (!valid || !editingProvider.value) return

  savingProvider.value = true
  try {
    await providerStore.updateProvider(editingProvider.value.id, {
      name: providerForm.name,
      type: providerForm.type,
      baseUrl: providerForm.baseUrl || undefined,
      priority: providerForm.priority,
      timeoutSeconds: providerForm.timeoutSeconds,
      retryCount: providerForm.retryCount,
      description: providerForm.description || undefined,
    })
    showEditProviderDialog.value = false
    ElMessage.success('Provider updated')
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to update provider')
  } finally {
    savingProvider.value = false
  }
}

function editKey(key: ProviderApiKey) {
  editingKey.value = key
  Object.assign(keyForm, {
    providerId: key.providerId,
    keyName: key.keyName,
    apiKey: '', // Don't show existing key
    enabled: key.enabled,
    weight: key.weight,
    rateLimitPerMinute: key.rateLimitPerMinute,
    dailyQuota: key.dailyQuota,
  })
  showAddKeyDialog.value = true
}

function resetKeyForm() {
  editingKey.value = null
  Object.assign(keyForm, {
    providerId: selectedProvider.value?.id || null,
    keyName: '',
    apiKey: '',
    enabled: true,
    weight: 100,
    rateLimitPerMinute: null,
    dailyQuota: null,
  })
  keyFormRef.value?.resetFields()
}

async function handleSaveKey() {
  const valid = await keyFormRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const data: any = {
      providerId: keyForm.providerId,
      keyName: keyForm.keyName,
      enabled: keyForm.enabled,
      weight: keyForm.weight,
    }

    if (keyForm.apiKey) {
      data.apiKey = keyForm.apiKey
    }
    if (keyForm.rateLimitPerMinute) {
      data.rateLimitPerMinute = keyForm.rateLimitPerMinute
    }
    if (keyForm.dailyQuota) {
      data.dailyQuota = keyForm.dailyQuota
    }

    if (editingKey.value) {
      await providerStore.updateApiKey(editingKey.value.id, data)
      ElMessage.success('API Key updated')
    } else {
      await providerStore.createApiKey(data)
      ElMessage.success('API Key added')
    }
    showAddKeyDialog.value = false
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to save API key')
  } finally {
    saving.value = false
  }
}

async function resetKey(key: ProviderApiKey) {
  try {
    await providerStore.resetApiKeyStatus(key.id)
    ElMessage.success('Key status reset')
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to reset key status')
  }
}

async function confirmDeleteKey(key: ProviderApiKey) {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete "${key.keyName}"? This action cannot be undone.`,
      'Delete API Key',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await providerStore.deleteApiKey(key.id)
    ElMessage.success('API Key deleted')
  } catch {
    // Cancelled
  }
}

async function handleReload() {
  reloading.value = true
  try {
    await providerStore.reloadKeyPool()
    ElMessage.success('Key pool reloaded')
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to reload key pool')
  } finally {
    reloading.value = false
  }
}

onMounted(() => {
  providerStore.fetchProviders()
  providerStore.fetchApiKeys()
})
</script>

<style scoped lang="scss">
.providers-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h1 {
      font-size: 24px;
      font-weight: 600;
      color: #1f2937;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .providers-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
    margin-bottom: 32px;
  }

  .provider-card {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    border: 1px solid #e5e7eb;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    &.disabled {
      opacity: 0.6;
    }

    .provider-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .provider-info {
        display: flex;
        align-items: center;
        gap: 12px;

        h3 {
          font-size: 16px;
          font-weight: 600;
          color: #1f2937;
          margin: 0;
        }

        .provider-type {
          font-size: 12px;
          color: #64748b;
        }
      }

      .provider-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 18px;
        color: #fff;

        &.openai { background: linear-gradient(135deg, #10a37f 0%, #0d8a6c 100%); }
        &.anthropic { background: linear-gradient(135deg, #d97706 0%, #b45309 100%); }
        &.deepseek { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); }
        &.moonshot { background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%); }
        &.google { background: linear-gradient(135deg, #ea4335 0%, #d93025 100%); }
      }
    }

    .provider-stats {
      display: flex;
      gap: 24px;
      margin-bottom: 16px;
      padding: 12px 0;
      border-top: 1px solid #f3f4f6;
      border-bottom: 1px solid #f3f4f6;

      .stat {
        text-align: center;

        .stat-value {
          display: block;
          font-size: 20px;
          font-weight: 600;
          color: #1f2937;
        }

        .stat-label {
          font-size: 12px;
          color: #64748b;
        }
      }
    }

    .provider-models {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 16px;
      min-height: 28px;
    }

    .provider-actions {
      display: flex;
      gap: 8px;
    }
  }

  .keys-section {
    background: #fff;
    border-radius: 12px;
    padding: 24px;

    .section-header {
      margin-bottom: 20px;

      h2 {
        font-size: 18px;
        font-weight: 600;
        color: #1f2937;
        display: flex;
        align-items: center;
        gap: 12px;

        .filter-tag {
          .close-icon {
            margin-left: 4px;
            cursor: pointer;
          }
        }
      }
    }

    .key-display {
      font-family: 'Fira Code', monospace;
      font-size: 13px;
      color: #64748b;
      background: #f1f5f9;
      padding: 4px 8px;
      border-radius: 4px;
    }

    .provider-badge {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;

      &.openai { background: #d1fae5; color: #065f46; }
      &.anthropic { background: #fef3c7; color: #92400e; }
      &.deepseek { background: #dbeafe; color: #1e40af; }
      &.moonshot { background: #ede9fe; color: #5b21b6; }
    }

    .usage-info {
      font-size: 13px;
      color: #64748b;

      .usage-tokens {
        display: block;
        font-size: 11px;
        color: #94a3b8;
      }
    }
  }
}
</style>
