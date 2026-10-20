<template>
  <div class="subscription-requests-page">
    <div class="page-header">
      <h1>Subscription Requests</h1>
      <el-button type="primary" @click="createDialogVisible = true">
        <el-icon><Plus /></el-icon>
        New Request
      </el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="My Requests" name="mine">
        <el-table :data="myRequests" v-loading="myLoading" stripe style="width: 100%">
          <el-table-column prop="email" label="Email" min-width="200" />
          <el-table-column label="Service" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.service }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="note" label="Note" min-width="200">
            <template #default="{ row }">{{ row.note || '-' }}</template>
          </el-table-column>
          <el-table-column label="Status" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="adminNote" label="Admin Note" min-width="200">
            <template #default="{ row }">{{ row.adminNote || '-' }}</template>
          </el-table-column>
          <el-table-column label="Submitted" width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Actions" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'pending'"
                size="small"
                link
                type="danger"
                @click="handleCancel(row)"
              >
                Cancel
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="whoami?.isAdmin" label="Admin Review" name="admin">
        <div class="filters">
          <el-select v-model="adminStatusFilter" placeholder="Status" clearable style="width: 150px" @change="loadAllRequests">
            <el-option label="Pending" value="pending" />
            <el-option label="Approved" value="approved" />
            <el-option label="Rejected" value="rejected" />
            <el-option label="Cancelled" value="cancelled" />
          </el-select>
        </div>
        <el-table :data="allRequests" v-loading="adminLoading" stripe style="width: 100%">
          <el-table-column prop="username" label="Employee" width="140">
            <template #default="{ row }">{{ row.username || `user #${row.userId}` }}</template>
          </el-table-column>
          <el-table-column prop="email" label="Email" min-width="200" />
          <el-table-column label="Service" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.service }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="note" label="Note" min-width="200">
            <template #default="{ row }">{{ row.note || '-' }}</template>
          </el-table-column>
          <el-table-column label="Status" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Submitted" width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Actions" width="160" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <el-button size="small" link type="success" @click="openReview(row, 'approved')">Approve</el-button>
                <el-button size="small" link type="danger" @click="openReview(row, 'rejected')">Reject</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Request Dialog -->
    <el-dialog v-model="createDialogVisible" title="Request a Subscription" width="480px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="Email" required>
          <el-input v-model="createForm.email" placeholder="you@company.com" />
        </el-form-item>
        <el-form-item label="Service" required>
          <el-select v-model="createForm.service" style="width: 100%">
            <el-option label="Claude" value="claude" />
            <el-option label="ChatGPT" value="chatgpt" />
            <el-option label="Other" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="Note">
          <el-input v-model="createForm.note" type="textarea" :rows="3" placeholder="Optional context for the admin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">Submit</el-button>
      </template>
    </el-dialog>

    <!-- Review Dialog -->
    <el-dialog v-model="reviewDialogVisible" :title="reviewForm.status === 'approved' ? 'Approve Request' : 'Reject Request'" width="480px">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="Admin Note">
          <el-input v-model="reviewForm.adminNote" type="textarea" :rows="3" placeholder="Optional note visible to the employee" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReview">Confirm</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { subscriptionRequestApi } from '@/api/subscription-request'
import type { SubscriptionRequest, SubscriptionRequestStatus, WhoAmI } from '@/types/subscription-request'

const activeTab = ref('mine')
const whoami = ref<WhoAmI | null>(null)

const myRequests = ref<SubscriptionRequest[]>([])
const myLoading = ref(false)

const allRequests = ref<SubscriptionRequest[]>([])
const adminLoading = ref(false)
const adminStatusFilter = ref('')

const createDialogVisible = ref(false)
const submitting = ref(false)
const createForm = reactive({
  email: '',
  service: 'claude' as 'claude' | 'chatgpt' | 'other',
  note: '',
})

const reviewDialogVisible = ref(false)
const reviewing = ref(false)
const reviewForm = reactive({
  requestId: 0,
  status: 'approved' as 'approved' | 'rejected',
  adminNote: '',
})

onMounted(async () => {
  await loadWhoAmI()
  await loadMyRequests()
  if (whoami.value?.isAdmin) {
    await loadAllRequests()
  }
})

async function loadWhoAmI() {
  try {
    const res = await subscriptionRequestApi.whoami()
    whoami.value = res.data
  } catch {
    whoami.value = null
  }
}

async function loadMyRequests() {
  myLoading.value = true
  try {
    const res = await subscriptionRequestApi.listMine()
    myRequests.value = res.data
  } finally {
    myLoading.value = false
  }
}

async function loadAllRequests() {
  adminLoading.value = true
  try {
    const res = await subscriptionRequestApi.listAll(adminStatusFilter.value || undefined)
    allRequests.value = res.data
  } finally {
    adminLoading.value = false
  }
}

async function handleCreate() {
  if (!createForm.email || !createForm.service) {
    ElMessage.error('Email and service are required')
    return
  }
  submitting.value = true
  try {
    await subscriptionRequestApi.create({
      email: createForm.email,
      service: createForm.service,
      note: createForm.note || undefined,
    })
    ElMessage.success('Request submitted')
    createDialogVisible.value = false
    createForm.email = ''
    createForm.note = ''
    await loadMyRequests()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to submit request')
  } finally {
    submitting.value = false
  }
}

async function handleCancel(row: SubscriptionRequest) {
  await ElMessageBox.confirm(`Cancel your ${row.service} request?`, 'Confirm Cancel', { type: 'warning' })
  try {
    await subscriptionRequestApi.cancel(row.id)
    ElMessage.success('Request cancelled')
    await loadMyRequests()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to cancel request')
  }
}

function openReview(row: SubscriptionRequest, status: 'approved' | 'rejected') {
  reviewForm.requestId = row.id
  reviewForm.status = status
  reviewForm.adminNote = ''
  reviewDialogVisible.value = true
}

async function handleReview() {
  reviewing.value = true
  try {
    await subscriptionRequestApi.review(reviewForm.requestId, {
      status: reviewForm.status,
      adminNote: reviewForm.adminNote || undefined,
    })
    ElMessage.success(reviewForm.status === 'approved' ? 'Request approved' : 'Request rejected')
    reviewDialogVisible.value = false
    await loadAllRequests()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to review request')
  } finally {
    reviewing.value = false
  }
}

function statusType(status: SubscriptionRequestStatus): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const types: Record<SubscriptionRequestStatus, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger',
    cancelled: 'info',
  }
  return types[status] || 'primary'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.subscription-requests-page {
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

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
