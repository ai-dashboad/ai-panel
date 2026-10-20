<template>
  <div class="orders-page">
    <div class="page-header">
      <h1>External Orders</h1>
      <div class="header-actions">
        <el-button @click="handleRetryAll" :loading="retryingAll">
          <el-icon><Refresh /></el-icon>
          Retry Failed
        </el-button>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters">
      <el-select
        v-model="filters.platform"
        placeholder="Platform"
        clearable
        style="width: 150px"
        @change="loadOrders"
      >
        <el-option
          v-for="platform in deliveryStore.platforms"
          :key="platform.platform"
          :label="platform.name"
          :value="platform.platform"
        />
      </el-select>
      <el-select
        v-model="filters.status"
        placeholder="Status"
        clearable
        style="width: 130px"
        @change="loadOrders"
      >
        <el-option label="Pending" value="pending" />
        <el-option label="Processing" value="processing" />
        <el-option label="Delivered" value="delivered" />
        <el-option label="Failed" value="failed" />
        <el-option label="Cancelled" value="cancelled" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="Search order ID, buyer..."
        style="width: 250px"
        clearable
        @keyup.enter="loadOrders"
        @clear="loadOrders"
      >
        <template #append>
          <el-button @click="loadOrders">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>

    <el-table
      :data="deliveryStore.orders?.list || []"
      v-loading="deliveryStore.ordersLoading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="externalOrderId" label="Order ID" width="150" />
      <el-table-column prop="platform" label="Platform" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.platform }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sku" label="SKU" width="130" />
      <el-table-column prop="buyerNick" label="Buyer" width="120" />
      <el-table-column prop="amount" label="Amount" width="100">
        <template #default="{ row }">
          {{ row.amount ? `¥${row.amount}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="Status" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="retryCount" label="Retries" width="80" align="center" />
      <el-table-column label="Delivery Time" width="170">
        <template #default="{ row }">
          {{ row.deliveryTime ? formatDate(row.deliveryTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="Created" width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link @click="viewOrderDetail(row)">
            <el-icon><View /></el-icon>
          </el-button>
          <el-button
            v-if="row.status === 'failed'"
            size="small"
            link
            type="primary"
            @click="retryOrder(row)"
          >
            <el-icon><Refresh /></el-icon>
            Retry
          </el-button>
          <el-button
            v-if="row.status !== 'delivered' && row.status !== 'cancelled'"
            size="small"
            link
            type="danger"
            @click="cancelOrder(row)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="deliveryStore.orders?.total || 0"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadOrders"
        @current-change="loadOrders"
      />
    </div>

    <!-- Order Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="Order Detail" width="700px">
      <template v-if="deliveryStore.currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Order ID">
            {{ deliveryStore.currentOrder.externalOrderId }}
          </el-descriptions-item>
          <el-descriptions-item label="Platform">
            {{ deliveryStore.currentOrder.platform }}
          </el-descriptions-item>
          <el-descriptions-item label="SKU">
            {{ deliveryStore.currentOrder.sku }}
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusType(deliveryStore.currentOrder.status)">
              {{ deliveryStore.currentOrder.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Buyer">
            {{ deliveryStore.currentOrder.buyerNick }}
          </el-descriptions-item>
          <el-descriptions-item label="Amount">
            {{ deliveryStore.currentOrder.amount ? `¥${deliveryStore.currentOrder.amount}` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Phone">
            {{ deliveryStore.currentOrder.buyerPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Email">
            {{ deliveryStore.currentOrder.buyerEmail || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Buyer Message" :span="2">
            {{ deliveryStore.currentOrder.buyerMessage || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Delivery Method">
            {{ deliveryStore.currentOrder.deliveryMethod || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Delivery Time">
            {{ deliveryStore.currentOrder.deliveryTime ? formatDate(deliveryStore.currentOrder.deliveryTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="deliveryStore.currentOrder.failReason" label="Fail Reason" :span="2">
            <el-text type="danger">{{ deliveryStore.currentOrder.failReason }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item v-if="deliveryStore.currentOrder.deliveryContent" label="Delivery Content" :span="2">
            <pre class="delivery-content">{{ deliveryStore.currentOrder.deliveryContent }}</pre>
          </el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px;">Delivery Logs</h4>
        <el-timeline>
          <el-timeline-item
            v-for="log in deliveryStore.orderLogs"
            :key="log.id"
            :timestamp="formatDate(log.createdAt)"
            :type="log.status === 'success' ? 'success' : 'danger'"
          >
            <strong>{{ log.action }}</strong>
            <p>{{ log.message }}</p>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, View, Close } from '@element-plus/icons-vue'
import { useDeliveryStore } from '@/stores/modules/delivery'
import { deliveryApi } from '@/api/delivery'
import type { ExternalOrder } from '@/types/delivery'

const deliveryStore = useDeliveryStore()

const filters = reactive({
  platform: '',
  status: '',
  keyword: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20
})

const detailDialogVisible = ref(false)
const retryingAll = ref(false)

onMounted(() => {
  deliveryStore.loadPlatforms()
  loadOrders()
})

function loadOrders() {
  deliveryStore.loadOrders({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    platform: filters.platform || undefined,
    status: filters.status || undefined,
    keyword: filters.keyword || undefined
  })
}

async function viewOrderDetail(order: ExternalOrder) {
  await deliveryStore.loadOrderDetails(order.id)
  detailDialogVisible.value = true
}

async function retryOrder(order: ExternalOrder) {
  try {
    await deliveryApi.retryOrder(order.id)
    ElMessage.success('Order retry initiated')
    loadOrders()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to retry order')
  }
}

async function cancelOrder(order: ExternalOrder) {
  await ElMessageBox.confirm(
    `Are you sure you want to cancel order "${order.externalOrderId}"?`,
    'Confirm Cancel',
    { type: 'warning' }
  )
  try {
    await deliveryApi.cancelOrder(order.id)
    ElMessage.success('Order cancelled')
    loadOrders()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to cancel order')
  }
}

async function handleRetryAll() {
  await ElMessageBox.confirm(
    'This will retry all failed orders. Continue?',
    'Confirm Retry All',
    { type: 'warning' }
  )
  retryingAll.value = true
  try {
    const count = await deliveryApi.retryFailedOrders(3)
    ElMessage.success(`Retried ${count} orders`)
    loadOrders()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || 'Failed to retry orders')
  } finally {
    retryingAll.value = false
  }
}

function getStatusType(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const types: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending: 'warning',
    processing: 'primary',
    delivered: 'success',
    failed: 'danger',
    cancelled: 'info'
  }
  return types[status] || 'primary'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.orders-page {
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
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.delivery-content {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
}
</style>
