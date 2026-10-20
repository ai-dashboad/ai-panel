<template>
  <div class="dashboard">
    <div class="page-header">
      <h1>Dashboard</h1>
    </div>

    <!-- Stats Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <div class="stat-icon">
            <el-icon :size="32"><Key /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.apiKeys }}</div>
            <div class="stat-label">API Keys</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%)">
          <div class="stat-icon">
            <el-icon :size="32"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(stats.requests) }}</div>
            <div class="stat-label">Total Requests</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #ee0979 0%, #ff6a00 100%)">
          <div class="stat-icon">
            <el-icon :size="32"><Coin /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(stats.tokens) }}</div>
            <div class="stat-label">Tokens Used</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%)">
          <div class="stat-icon">
            <el-icon :size="32"><Wallet /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">${{ stats.balance.toFixed(2) }}</div>
            <div class="stat-label">Balance</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <div class="chart-card">
          <div class="chart-header">
            <h3>Usage Trend</h3>
            <el-radio-group v-model="chartPeriod" size="small">
              <el-radio-button label="7d">7 Days</el-radio-button>
              <el-radio-button label="30d">30 Days</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="usageChartRef" class="chart-container"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="chart-card">
          <div class="chart-header">
            <h3>Model Distribution</h3>
          </div>
          <div ref="modelChartRef" class="chart-container"></div>
        </div>
      </el-col>
    </el-row>

    <!-- Recent Activity -->
    <div class="activity-card">
      <div class="card-header">
        <h3>Recent API Calls</h3>
        <router-link to="/usage">View All</router-link>
      </div>
      <el-table :data="recentCalls" stripe>
        <el-table-column prop="time" label="Time" width="180" />
        <el-table-column prop="model" label="Model" />
        <el-table-column prop="tokens" label="Tokens" width="100" />
        <el-table-column prop="latency" label="Latency" width="100">
          <template #default="{ row }">{{ row.latency }}ms</template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import { loadUsageMock } from '@/api/usageMock'

const usageChartRef = ref<HTMLElement>()
const modelChartRef = ref<HTMLElement>()
const chartPeriod = ref('7d')

const stats = reactive({
  apiKeys: 0,
  requests: 0,
  tokens: 0,
  balance: 0,
})

const recentCalls = ref<
  Array<{ time: string; model: string; tokens: number; latency: number; status: string }>
>([])

let dailyLabels: string[] = []
let dailyRequests: number[] = []
let modelDistribution: Array<{ name: string; value: number }> = []

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function initUsageChart() {
  if (!usageChartRef.value) return

  const chart = echarts.init(usageChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dailyLabels,
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: 'Requests',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        data: dailyRequests,
        itemStyle: { color: '#3b82f6' },
      },
    ],
  }
  chart.setOption(option)
}

function initModelChart() {
  if (!modelChartRef.value) return

  const chart = echarts.init(modelChartRef.value)
  const option = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        data: modelDistribution,
      },
    ],
  }
  chart.setOption(option)
}

onMounted(async () => {
  try {
    const mock = await loadUsageMock()
    stats.apiKeys = mock.dashboard.apiKeys
    stats.requests = mock.dashboard.requests
    stats.tokens = mock.dashboard.tokens
    stats.balance = mock.dashboard.balance
    recentCalls.value = mock.dashboard.recentCalls
    dailyLabels = mock.daily.labels
    dailyRequests = mock.daily.requests
    modelDistribution = mock.dashboard.modelDistribution
  } finally {
    initUsageChart()
    initModelChart()
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  .stats-row {
    margin-bottom: 20px;
  }

  .stat-card {
    border-radius: 12px;
    padding: 20px;
    color: #fff;
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .stat-icon {
      background: rgba(255, 255, 255, 0.2);
      border-radius: 12px;
      padding: 12px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
    }

    .stat-label {
      font-size: 14px;
      opacity: 0.9;
    }
  }

  .chart-card,
  .activity-card {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  }

  .chart-header,
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }

    a {
      color: #3b82f6;
      text-decoration: none;
      font-size: 14px;
    }
  }

  .chart-container {
    height: 300px;
  }
}
</style>
