<template>
  <div class="skus-page">
    <div class="page-header">
      <h1>Product SKUs</h1>
      <div class="header-actions">
        <el-select
          v-model="filterPlatform"
          placeholder="Filter by platform"
          clearable
          style="width: 150px; margin-right: 10px"
          @change="loadSkus"
        >
          <el-option
            v-for="platform in deliveryStore.platforms"
            :key="platform.platform"
            :label="platform.name"
            :value="platform.platform"
          />
        </el-select>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          Add SKU
        </el-button>
      </div>
    </div>

    <el-table
      :data="deliveryStore.skus"
      v-loading="deliveryStore.skusLoading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="sku" label="SKU Code" width="150" />
      <el-table-column prop="name" label="Name" width="180" />
      <el-table-column label="Platform" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.platform" size="small">{{ row.platform }}</el-tag>
          <el-tag v-else type="info" size="small">All</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Quota" width="150">
        <template #default="{ row }">
          <span v-if="row.quotaType === 'tokens'">
            {{ formatNumber(row.quotaTokens) }} tokens
          </span>
          <span v-else>
            {{ formatNumber(row.quotaRequests) }} requests
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="validityDays" label="Validity" width="100">
        <template #default="{ row }">
          {{ row.validityDays }} days
        </template>
      </el-table-column>
      <el-table-column prop="rateLimit" label="Rate Limit" width="120">
        <template #default="{ row }">
          {{ row.rateLimit ? `${row.rateLimit}/min` : 'Default' }}
        </template>
      </el-table-column>
      <el-table-column label="Enabled" width="100" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link @click="editSku(row)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button size="small" link type="danger" @click="deleteSku(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? 'Edit SKU' : 'Add SKU'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="140px"
      >
        <el-form-item label="SKU Code" prop="sku">
          <el-input v-model="formData.sku" :disabled="isEditing" placeholder="e.g., API-PRO-30D" />
        </el-form-item>
        <el-form-item label="Name" prop="name">
          <el-input v-model="formData.name" placeholder="Product name" />
        </el-form-item>
        <el-form-item label="Platform">
          <el-select v-model="formData.platform" clearable placeholder="All platforms">
            <el-option
              v-for="platform in deliveryStore.platforms"
              :key="platform.platform"
              :label="platform.name"
              :value="platform.platform"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="formData.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Quota Type">
          <el-radio-group v-model="formData.quotaType">
            <el-radio value="tokens">Tokens</el-radio>
            <el-radio value="requests">Requests</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.quotaType === 'tokens'" label="Token Quota">
          <el-input-number v-model="formData.quotaTokens" :min="0" :step="100000" />
        </el-form-item>
        <el-form-item v-else label="Request Quota">
          <el-input-number v-model="formData.quotaRequests" :min="0" :step="1000" />
        </el-form-item>
        <el-form-item label="Validity (Days)">
          <el-input-number v-model="formData.validityDays" :min="1" :max="365" />
        </el-form-item>
        <el-form-item label="Allowed Models">
          <el-input v-model="formData.allowedModels" placeholder="Comma-separated, empty for all" />
        </el-form-item>
        <el-form-item label="Rate Limit (/min)">
          <el-input-number v-model="formData.rateLimit" :min="0" :max="10000" />
        </el-form-item>
        <el-form-item label="Enabled">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveSku" :loading="saving">
          {{ isEditing ? 'Update' : 'Create' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { useDeliveryStore } from '@/stores/modules/delivery'
import { deliveryApi } from '@/api/delivery'
import type { ProductSku, CreateProductSkuRequest } from '@/types/delivery'

const deliveryStore = useDeliveryStore()

const filterPlatform = ref<string>('')
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const defaultFormData: CreateProductSkuRequest = {
  platform: undefined,
  sku: '',
  name: '',
  description: '',
  quotaType: 'tokens',
  quotaTokens: 1000000,
  quotaRequests: undefined,
  validityDays: 30,
  allowedModels: '',
  rateLimit: undefined,
  enabled: true
}

const formData = reactive<CreateProductSkuRequest>({ ...defaultFormData })

const formRules: FormRules = {
  sku: [
    { required: true, message: 'SKU code is required', trigger: 'blur' },
    { max: 100, message: 'Max 100 characters', trigger: 'blur' }
  ],
  name: [
    { required: true, message: 'Name is required', trigger: 'blur' },
    { max: 100, message: 'Max 100 characters', trigger: 'blur' }
  ]
}

onMounted(() => {
  deliveryStore.loadPlatforms()
  loadSkus()
})

function loadSkus() {
  deliveryStore.loadSkus(filterPlatform.value || undefined)
}

function showAddDialog() {
  isEditing.value = false
  editingId.value = null
  Object.assign(formData, defaultFormData)
  dialogVisible.value = true
}

function editSku(sku: ProductSku) {
  isEditing.value = true
  editingId.value = sku.id
  Object.assign(formData, {
    platform: sku.platform || undefined,
    sku: sku.sku,
    name: sku.name,
    description: sku.description || '',
    quotaType: sku.quotaType,
    quotaTokens: sku.quotaTokens,
    quotaRequests: sku.quotaRequests,
    validityDays: sku.validityDays,
    allowedModels: sku.allowedModels || '',
    rateLimit: sku.rateLimit,
    enabled: sku.enabled
  })
  dialogVisible.value = true
}

async function saveSku() {
  if (!formRef.value) return
  await formRef.value.validate()

  saving.value = true
  try {
    if (isEditing.value && editingId.value) {
      await deliveryApi.updateSku(editingId.value, formData)
      ElMessage.success('SKU updated successfully')
    } else {
      await deliveryApi.createSku(formData)
      ElMessage.success('SKU created successfully')
    }
    dialogVisible.value = false
    loadSkus()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to save SKU')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(sku: ProductSku) {
  try {
    await deliveryApi.updateSku(sku.id, {
      sku: sku.sku,
      name: sku.name,
      enabled: sku.enabled
    })
    ElMessage.success(`SKU ${sku.enabled ? 'enabled' : 'disabled'}`)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to update SKU')
    sku.enabled = !sku.enabled
  }
}

async function deleteSku(sku: ProductSku) {
  await ElMessageBox.confirm(
    `Are you sure you want to delete SKU "${sku.name}"?`,
    'Confirm Delete',
    { type: 'warning' }
  )
  try {
    await deliveryApi.deleteSku(sku.id)
    ElMessage.success('SKU deleted successfully')
    loadSkus()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to delete SKU')
  }
}

function formatNumber(num: number | null): string {
  if (num === null) return '-'
  if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}K`
  return String(num)
}
</script>

<style scoped>
.skus-page {
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

.header-actions {
  display: flex;
  align-items: center;
}
</style>
