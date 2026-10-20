<template>
  <div class="platforms-page">
    <div class="page-header">
      <h1>Platform Configuration</h1>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        Add Platform
      </el-button>
    </div>

    <el-table
      :data="deliveryStore.platforms"
      v-loading="deliveryStore.platformsLoading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="platform" label="Platform" width="120" />
      <el-table-column prop="name" label="Name" width="150" />
      <el-table-column label="Webhook URL" min-width="300">
        <template #default="{ row }">
          <div class="webhook-url">
            <code>{{ row.webhookUrl }}</code>
            <el-button size="small" link @click="copyWebhookUrl(row.webhookUrl)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Auto Create User" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="row.autoCreateUser ? 'success' : 'info'" size="small">
            {{ row.autoCreateUser ? 'Yes' : 'No' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Auto Delivery" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.autoDelivery ? 'success' : 'info'" size="small">
            {{ row.autoDelivery ? 'Yes' : 'No' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Enabled" width="100" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link @click="editPlatform(row)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button size="small" link type="danger" @click="deletePlatform(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? 'Edit Platform' : 'Add Platform'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="140px"
      >
        <el-form-item label="Platform ID" prop="platform">
          <el-input v-model="formData.platform" :disabled="isEditing" placeholder="e.g., taobao" />
        </el-form-item>
        <el-form-item label="Name" prop="name">
          <el-input v-model="formData.name" placeholder="Display name" />
        </el-form-item>
        <el-form-item label="App Key">
          <el-input v-model="formData.appKey" placeholder="Platform app key (optional)" />
        </el-form-item>
        <el-form-item label="App Secret">
          <el-input v-model="formData.appSecret" type="password" placeholder="Platform app secret (optional)" show-password />
        </el-form-item>
        <el-form-item label="Webhook Secret">
          <el-input v-model="formData.webhookSecret" type="password" placeholder="For signature verification" show-password />
        </el-form-item>
        <el-form-item label="Delivery Template">
          <el-input
            v-model="formData.deliveryTemplate"
            type="textarea"
            :rows="6"
            placeholder="Use ${apiKey}, ${productName}, ${expiresAt}, etc."
          />
        </el-form-item>
        <el-form-item label="Auto Create User">
          <el-switch v-model="formData.autoCreateUser" />
        </el-form-item>
        <el-form-item label="Auto Delivery">
          <el-switch v-model="formData.autoDelivery" />
        </el-form-item>
        <el-form-item label="Enabled">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="savePlatform" :loading="saving">
          {{ isEditing ? 'Update' : 'Create' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Edit, Delete, CopyDocument } from '@element-plus/icons-vue'
import { useDeliveryStore } from '@/stores/modules/delivery'
import { deliveryApi } from '@/api/delivery'
import type { PlatformConfig, CreatePlatformConfigRequest } from '@/types/delivery'

const deliveryStore = useDeliveryStore()

const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const defaultFormData: CreatePlatformConfigRequest = {
  platform: '',
  name: '',
  appKey: '',
  appSecret: '',
  webhookSecret: '',
  deliveryTemplate: '',
  autoCreateUser: true,
  autoDelivery: true,
  enabled: true
}

const formData = reactive<CreatePlatformConfigRequest>({ ...defaultFormData })

const formRules: FormRules = {
  platform: [
    { required: true, message: 'Platform ID is required', trigger: 'blur' },
    { max: 30, message: 'Max 30 characters', trigger: 'blur' }
  ],
  name: [
    { required: true, message: 'Name is required', trigger: 'blur' },
    { max: 50, message: 'Max 50 characters', trigger: 'blur' }
  ]
}

onMounted(() => {
  deliveryStore.loadPlatforms()
})

function showAddDialog() {
  isEditing.value = false
  editingId.value = null
  Object.assign(formData, defaultFormData)
  dialogVisible.value = true
}

function editPlatform(platform: PlatformConfig) {
  isEditing.value = true
  editingId.value = platform.id
  Object.assign(formData, {
    platform: platform.platform,
    name: platform.name,
    appKey: '',
    appSecret: '',
    webhookSecret: '',
    deliveryTemplate: platform.deliveryTemplate || '',
    autoCreateUser: platform.autoCreateUser,
    autoDelivery: platform.autoDelivery,
    enabled: platform.enabled
  })
  dialogVisible.value = true
}

async function savePlatform() {
  if (!formRef.value) return
  await formRef.value.validate()

  saving.value = true
  try {
    if (isEditing.value && editingId.value) {
      await deliveryApi.updatePlatform(editingId.value, formData)
      ElMessage.success('Platform updated successfully')
    } else {
      await deliveryApi.createPlatform(formData)
      ElMessage.success('Platform created successfully')
    }
    dialogVisible.value = false
    deliveryStore.loadPlatforms()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to save platform')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(platform: PlatformConfig) {
  try {
    await deliveryApi.updatePlatform(platform.id, {
      platform: platform.platform,
      name: platform.name,
      enabled: platform.enabled
    })
    ElMessage.success(`Platform ${platform.enabled ? 'enabled' : 'disabled'}`)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to update platform')
    platform.enabled = !platform.enabled
  }
}

async function deletePlatform(platform: PlatformConfig) {
  await ElMessageBox.confirm(
    `Are you sure you want to delete platform "${platform.name}"?`,
    'Confirm Delete',
    { type: 'warning' }
  )
  try {
    await deliveryApi.deletePlatform(platform.id)
    ElMessage.success('Platform deleted successfully')
    deliveryStore.loadPlatforms()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to delete platform')
  }
}

function copyWebhookUrl(url: string) {
  navigator.clipboard.writeText(url)
  ElMessage.success('Webhook URL copied to clipboard')
}
</script>

<style scoped>
.platforms-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
}

.webhook-url {
  display: flex;
  align-items: center;
  gap: 8px;
}

.webhook-url code {
  font-size: 12px;
  background: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
  word-break: break-all;
}
</style>
