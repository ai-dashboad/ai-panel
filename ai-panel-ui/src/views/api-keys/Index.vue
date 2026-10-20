<template>
  <div class="api-keys-page">
    <div class="page-header">
      <h1>API Keys</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        Create API Key
      </el-button>
    </div>

    <div class="page-container">
      <el-table :data="apiKeyStore.keys" v-loading="apiKeyStore.loading" stripe>
        <el-table-column prop="name" label="Name" min-width="150" />
        <el-table-column label="API Key" min-width="250">
          <template #default="{ row }">
            <code class="api-key-display">{{ row.keyPrefix }}</code>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Rate Limit" width="120">
          <template #default="{ row }">
            {{ row.rateLimitPerMinute || 'Default' }} /min
          </template>
        </el-table-column>
        <el-table-column prop="lastUsedAt" label="Last Used" width="180">
          <template #default="{ row }">
            {{ row.lastUsedAt ? formatDate(row.lastUsedAt) : 'Never' }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editKey(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="danger" @click="confirmDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Create API Key Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="Create API Key"
      width="500px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Key Name" prop="name">
          <el-input v-model="form.name" placeholder="e.g., Production API Key" />
        </el-form-item>
        <el-form-item label="Description" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="Optional description"
            :rows="2"
          />
        </el-form-item>
        <el-form-item label="Rate Limit (per minute)" prop="rateLimitPerMinute">
          <el-input-number
            v-model="form.rateLimitPerMinute"
            :min="1"
            :max="10000"
            placeholder="Leave empty for default"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Expiration">
          <el-date-picker
            v-model="form.expiresAt"
            type="datetime"
            placeholder="Never expires"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">
          Create
        </el-button>
      </template>
    </el-dialog>

    <!-- Show New API Key Dialog -->
    <el-dialog
      v-model="showKeyDialog"
      title="API Key Created"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert
        title="Save this API key - you won't be able to see it again!"
        type="warning"
        show-icon
        :closable="false"
        class="mb-4"
      />
      <div class="new-key-display">
        <code>{{ newApiKey }}</code>
        <el-button type="primary" link @click="copyKey">
          <el-icon><CopyDocument /></el-icon>
          Copy
        </el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="showKeyDialog = false">Done</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useApiKeyStore } from '@/stores/modules/apiKeys'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ApiKey } from '@/types/apiKey'
import dayjs from 'dayjs'

const apiKeyStore = useApiKeyStore()

const showCreateDialog = ref(false)
const showKeyDialog = ref(false)
const creating = ref(false)
const newApiKey = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  description: '',
  rateLimitPerMinute: null as number | null,
  expiresAt: null as Date | null,
})

const rules: FormRules = {
  name: [
    { required: true, message: 'Please enter a key name', trigger: 'blur' },
    { min: 1, max: 100, message: 'Name must be 1-100 characters', trigger: 'blur' },
  ],
}

function disabledDate(date: Date): boolean {
  return date.getTime() <= Date.now()
}

function formatDate(date: string): string {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const requestData: any = {
      name: form.name,
      description: form.description || undefined,
    }
    if (form.rateLimitPerMinute) {
      requestData.rateLimitPerMinute = form.rateLimitPerMinute
    }
    if (form.expiresAt) {
      // Format as ISO datetime string
      requestData.expiresAt = form.expiresAt.toISOString().slice(0, 19)
    }
    const result = await apiKeyStore.createKey(requestData)
    newApiKey.value = result.fullKey
    showCreateDialog.value = false
    showKeyDialog.value = true
    ElMessage.success('API Key created successfully')
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to create API key')
  } finally {
    creating.value = false
  }
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.rateLimitPerMinute = null
  form.expiresAt = null
  formRef.value?.resetFields()
}

function editKey(_key: ApiKey) {
  ElMessage.info('Edit functionality coming soon')
}

async function confirmDelete(key: ApiKey) {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete "${key.name}"? This action cannot be undone.`,
      'Delete API Key',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await apiKeyStore.deleteKey(key.id)
    ElMessage.success('API Key deleted')
  } catch {
    // Cancelled
  }
}

function copyKey() {
  navigator.clipboard.writeText(newApiKey.value)
  ElMessage.success('API Key copied to clipboard')
}

onMounted(() => {
  apiKeyStore.fetchKeys()
})
</script>

<style scoped lang="scss">
.api-keys-page {
  .usage-text {
    font-size: 12px;
    color: #64748b;
    margin-top: 4px;
  }

  .new-key-display {
    display: flex;
    align-items: center;
    gap: 12px;
    background: #1e293b;
    padding: 16px;
    border-radius: 8px;
    margin-top: 16px;

    code {
      flex: 1;
      color: #10b981;
      font-family: 'Fira Code', monospace;
      font-size: 14px;
      word-break: break-all;
    }
  }

  .mb-4 {
    margin-bottom: 16px;
  }
}
</style>
