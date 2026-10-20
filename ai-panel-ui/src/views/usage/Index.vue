<template>
  <div class="usage-page">
    <div class="page-header">
      <h1>Usage & Billing</h1>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="to"
        start-placeholder="Start date"
        end-placeholder="End date"
        :shortcuts="dateShortcuts"
      />
    </div>

    <!-- Summary Cards -->
    <el-row :gutter="20" class="summary-row">
      <el-col :xs="24" :sm="8">
        <div class="summary-card">
          <div class="summary-value">{{ formatNumber(summary.totalRequests) }}</div>
          <div class="summary-label">Total Requests</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="summary-card">
          <div class="summary-value">{{ formatNumber(summary.totalTokens) }}</div>
          <div class="summary-label">Total Tokens</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="summary-card">
          <div class="summary-value">${{ summary.totalCost.toFixed(2) }}</div>
          <div class="summary-label">Total Cost</div>
        </div>
      </el-col>
    </el-row>

    <!-- Usage Chart -->
    <div class="chart-card">
      <h3>Daily Usage</h3>
      <div ref="usageChartRef" class="chart-container"></div>
    </div>

    <!-- Usage Records -->
    <div class="records-card">
      <h3>Usage Records</h3>
      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="date" label="Date" width="120" />
        <el-table-column prop="model" label="Model" min-width="150" />
        <el-table-column prop="requests" label="Requests" width="100" />
        <el-table-column prop="inputTokens" label="Input Tokens" width="120">
          <template #default="{ row }">{{ formatNumber(row.inputTokens) }}</template>
        </el-table-column>
        <el-table-column prop="outputTokens" label="Output Tokens" width="120">
          <template #default="{ row }">{{ formatNumber(row.outputTokens) }}</template>
        </el-table-column>
        <el-table-column prop="cost" label="Cost" width="100">
          <template #default="{ row }">${{ row.cost.toFixed(4) }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="totalRecords"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { loadUsageMock } from '@/api/usageMock'

const usageChartRef = ref<HTMLElement>()
const loading = ref(true)
const dateRange = ref<[Date, Date]>([
  dayjs().subtract(7, 'day').toDate(),
  dayjs().toDate(),
])

const dateShortcuts = [
  { text: 'Last 7 days', value: () => [dayjs().subtract(7, 'day').toDate(), dayjs().toDate()] },
  { text: 'Last 30 days', value: () => [dayjs().subtract(30, 'day').toDate(), dayjs().toDate()] },
  { text: 'This month', value: () => [dayjs().startOf('month').toDate(), dayjs().toDate()] },
]

const currentPage = ref(1)
const pageSize = ref(20)
const totalRecords = ref(0)

const summary = reactive({
  totalRequests: 0,
  totalTokens: 0,
  totalCost: 0,
})

const records = ref<
  Array<{
    date: string
    model: string
    requests: number
    inputTokens: number
    outputTokens: number
    cost: number
  }>
>([])

let dailyLabels: string[] = []
let dailyRequests: number[] = []
let dailyTokensK: number[] = []
let dailyCost: number[] = []

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function initChart() {
  if (!usageChartRef.value) return

  const chart = echarts.init(usageChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: { data: ['Requests', 'Tokens (K)', 'Cost ($)'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dailyLabels,
    },
    yAxis: [
      { type: 'value', name: 'Requests' },
      { type: 'value', name: 'Cost ($)', position: 'right' },
    ],
    series: [
      {
        name: 'Requests',
        type: 'bar',
        data: dailyRequests,
        itemStyle: { color: '#3b82f6' },
      },
      {
        name: 'Tokens (K)',
        type: 'line',
        data: dailyTokensK,
        itemStyle: { color: '#10b981' },
      },
      {
        name: 'Cost ($)',
        type: 'line',
        yAxisIndex: 1,
        data: dailyCost,
        itemStyle: { color: '#f59e0b' },
      },
    ],
  }
  chart.setOption(option)
}

onMounted(async () => {
  try {
    const mock = await loadUsageMock()
    Object.assign(summary, mock.summary)
    records.value = mock.records
    totalRecords.value = mock.records.length
    dailyLabels = mock.daily.labels
    dailyRequests = mock.daily.requests
    dailyTokensK = mock.daily.tokensK
    dailyCost = mock.daily.cost
  } finally {
    loading.value = false
    initChart()
  }
})
</script>

<style scoped lang="scss">
.usage-page {
  .summary-row {
    margin-bottom: 20px;
  }

  .summary-card {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    text-align: center;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    .summary-value {
      font-size: 32px;
      font-weight: 700;
      color: #1f2937;
    }

    .summary-label {
      font-size: 14px;
      color: #64748b;
      margin-top: 4px;
    }
  }

  .chart-card,
  .records-card {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
      margin-bottom: 16px;
    }
  }

  .chart-container {
    height: 350px;
  }

  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}
</style>
