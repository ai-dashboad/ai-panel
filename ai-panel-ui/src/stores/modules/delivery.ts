import { defineStore } from 'pinia'
import { ref } from 'vue'
import { deliveryApi } from '@/api/delivery'
import type {
  PlatformConfig,
  ProductSku,
  ExternalOrder,
  DeliveryLog,
  OrderPageResult
} from '@/types/delivery'

export const useDeliveryStore = defineStore('delivery', () => {
  // Platform configs
  const platforms = ref<PlatformConfig[]>([])
  const platformsLoading = ref(false)

  // Product SKUs
  const skus = ref<ProductSku[]>([])
  const skusLoading = ref(false)

  // Orders
  const orders = ref<OrderPageResult | null>(null)
  const ordersLoading = ref(false)
  const currentOrder = ref<ExternalOrder | null>(null)
  const orderLogs = ref<DeliveryLog[]>([])

  // Load platforms
  async function loadPlatforms() {
    platformsLoading.value = true
    try {
      const response = await deliveryApi.listPlatforms()
      platforms.value = response.data
    } finally {
      platformsLoading.value = false
    }
  }

  // Load SKUs
  async function loadSkus(platform?: string) {
    skusLoading.value = true
    try {
      const response = await deliveryApi.listSkus(platform)
      skus.value = response.data
    } finally {
      skusLoading.value = false
    }
  }

  // Load orders
  async function loadOrders(params: {
    pageNum?: number
    pageSize?: number
    platform?: string
    status?: string
    keyword?: string
  } = {}) {
    ordersLoading.value = true
    try {
      const response = await deliveryApi.listOrders({
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 20,
        ...params
      })
      orders.value = response.data
    } finally {
      ordersLoading.value = false
    }
  }

  // Load order details
  async function loadOrderDetails(id: number) {
    const orderResponse = await deliveryApi.getOrder(id)
    currentOrder.value = orderResponse.data
    const logsResponse = await deliveryApi.getOrderLogs(id)
    orderLogs.value = logsResponse.data
  }

  return {
    platforms,
    platformsLoading,
    skus,
    skusLoading,
    orders,
    ordersLoading,
    currentOrder,
    orderLogs,
    loadPlatforms,
    loadSkus,
    loadOrders,
    loadOrderDetails
  }
})
