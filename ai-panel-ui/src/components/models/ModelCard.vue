<template>
  <article class="model-card" data-testid="model-card">
    <header class="card-head">
      <div class="title-row">
        <span class="model-id" :title="model.id">{{ model.id }}</span>
        <button class="icon-btn" type="button" aria-label="Copy model id" @click="copyId">
          <el-icon><CopyDocument /></el-icon>
        </button>
      </div>
      <span class="provider-badge" :style="{ background: providerColor }">
        {{ providerInitial }}
      </span>
    </header>

    <p class="desc">{{ model.description || 'No description yet' }}</p>

    <div class="pricing">
      <div class="price-label">Dynamic pricing</div>
      <div class="price-row">
        <span>Input <strong>${{ formatSell(model.sellInputPricePerMillion, model.inputPrice) }}</strong> / {{ unitLabel }}</span>
        <span>Output <strong>${{ formatSell(model.sellOutputPricePerMillion, model.outputPrice) }}</strong> / {{ unitLabel }}</span>
      </div>
    </div>

    <div class="tags">
      <span v-if="model.groupName" class="tag group">{{ model.groupName }}</span>
      <span v-for="label in displayLabels" :key="label" class="tag">{{ label }}</span>
    </div>

    <div class="health" aria-label="Recent request health">
      <div class="sparkline">
        <i
          v-for="(s, i) in sparkline"
          :key="i"
          :class="['seg', s === 1 ? 'ok' : s === 0 ? 'err' : 'idle']"
        />
      </div>
      <div class="metrics">
        <span>{{ successText }}</span>
        <span>{{ latencyText }}</span>
        <span>{{ throughputText }}</span>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { CatalogModel, PriceUnit } from '@/types/model'
import { PROVIDER_META } from '@/data/catalogFallback'

const props = defineProps<{
  model: CatalogModel
  priceUnit: PriceUnit
}>()

const providerColor = computed(
  () => PROVIDER_META[props.model.provider]?.color || '#64748b',
)
const providerInitial = computed(() => (props.model.provider || '?').charAt(0).toUpperCase())
const unitLabel = computed(() => (props.priceUnit === '1M' ? '1M' : '1K'))

const displayLabels = computed(() =>
  (props.model.labels || []).filter((l) => l !== 'chat').slice(0, 3),
)

const sparkline = computed(() => {
  const recent = props.model.metrics?.recentStatuses
  if (recent && recent.length) return recent.slice(-24)
  return Array.from({ length: 24 }, () => -1)
})

const successText = computed(() => {
  const r = props.model.metrics?.successRate
  return r == null ? '—' : `${r.toFixed(2)}%`
})

const latencyText = computed(() => {
  const s = props.model.metrics?.avgLatencySeconds
  return s == null || s === 0 ? '—' : `${s.toFixed(2)}s`
})

const throughputText = computed(() => {
  const t = props.model.metrics?.throughputTokensPerSecond
  return t == null || t === 0 ? '—' : `${t.toFixed(1)}t/s`
})

function formatSell(perMillion: number | undefined, perThousand: number | undefined): string {
  if (props.priceUnit === '1M') {
    const v = perMillion ?? (perThousand ?? 0) * 1000
    return v >= 10 ? v.toFixed(1) : v.toFixed(2)
  }
  const v = perThousand ?? (perMillion ?? 0) / 1000
  return v.toFixed(4)
}

async function copyId() {
  await navigator.clipboard.writeText(props.model.id)
  ElMessage.success('Model ID copied')
}
</script>

<style scoped lang="scss">
.model-card {
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 220px;
  transition: box-shadow 0.2s, border-color 0.2s;

  &:hover {
    border-color: #cfd8e3;
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  }
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.model-id {
  font-family: 'JetBrains Mono', 'Fira Code', ui-monospace, monospace;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-btn {
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  padding: 2px;
  display: inline-flex;

  &:hover {
    color: #334155;
  }
}

.provider-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.desc {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  min-height: 18px;
}

.pricing {
  .price-label {
    font-size: 12px;
    color: #64748b;
    margin-bottom: 4px;
  }

  .price-row {
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 13px;
    color: #475569;

    strong {
      color: #0f172a;
      font-weight: 600;
    }
  }
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;

  &.group {
    background: #eef2ff;
    color: #4338ca;
  }
}

.health {
  margin-top: auto;
}

.sparkline {
  display: flex;
  gap: 2px;
  height: 10px;
  margin-bottom: 8px;
}

.seg {
  flex: 1;
  border-radius: 1px;

  &.ok {
    background: #22c55e;
  }
  &.err {
    background: #ef4444;
  }
  &.idle {
    background: #e2e8f0;
  }
}

.metrics {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: #64748b;
}
</style>
