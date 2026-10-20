<template>
  <div class="models-ops" data-testid="models-ops">
    <aside class="ops-sidebar">
      <section class="filter-block">
        <h3>Groups</h3>
        <button
          type="button"
          class="filter-item"
          :class="{ active: !filterGroup }"
          @click="filterGroup = ''"
        >
          <span>All groups</span>
          <span class="count">{{ groups.length }}</span>
        </button>
        <button
          v-for="g in groups"
          :key="g.code"
          type="button"
          class="filter-item"
          :class="{ active: filterGroup === g.code }"
          @click="filterGroup = g.code"
        >
          <span>{{ g.name }} <em>x{{ formatMult(g.priceMultiplier) }}</em></span>
          <span class="count">{{ g.modelCount ?? countByGroup(g.code) }}</span>
        </button>
      </section>

      <section class="filter-block">
        <h3>Providers</h3>
        <button
          type="button"
          class="filter-item"
          :class="{ active: !filterProvider }"
          @click="filterProvider = ''"
        >
          <span>All providers</span>
          <span class="count">{{ models.length }}</span>
        </button>
        <button
          v-for="p in providerCounts"
          :key="p.code"
          type="button"
          class="filter-item"
          :class="{ active: filterProvider === p.code }"
          @click="filterProvider = p.code"
        >
          <span>{{ p.name }}</span>
          <span class="count">{{ p.count }}</span>
        </button>
      </section>

      <section class="filter-block">
        <h3>Pricing type</h3>
        <button
          type="button"
          class="filter-item"
          :class="{ active: !filterPricing }"
          @click="filterPricing = ''"
        >
          <span>All</span>
          <span class="count">{{ models.length }}</span>
        </button>
        <button
          type="button"
          class="filter-item"
          :class="{ active: filterPricing === 'usage' }"
          @click="filterPricing = 'usage'"
        >
          <span>Pay-as-you-go</span>
          <span class="count">{{ usageCount }}</span>
        </button>
      </section>

      <section class="filter-block">
        <h3>Endpoint type</h3>
        <button type="button" class="filter-item active">
          <span>Chat</span>
          <span class="count">{{ models.length }}</span>
        </button>
      </section>
    </aside>

    <main class="ops-main">
      <header class="ops-toolbar">
        <div class="toolbar-left">
          <h1>Models</h1>
          <span class="result-count" data-testid="result-count">
            {{ filteredModels.length }} / {{ models.length }}
          </span>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="searchQuery"
            placeholder="Search models..."
            clearable
            style="width: 220px"
            data-testid="model-search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-radio-group v-model="priceUnit" size="small">
            <el-radio-button value="1M">/1M</el-radio-button>
            <el-radio-button value="1K">/1K</el-radio-button>
          </el-radio-group>
          <el-select v-model="sortBy" size="small" style="width: 130px">
            <el-option label="Name" value="name" />
            <el-option label="Provider" value="provider" />
            <el-option label="Price" value="price" />
          </el-select>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="grid">Grid</el-radio-button>
            <el-radio-button value="list">List</el-radio-button>
          </el-radio-group>
        </div>
      </header>

      <el-skeleton v-if="loading" :rows="6" animated />

      <div v-else :class="['card-grid', viewMode]">
        <ModelCard
          v-for="model in filteredModels"
          :key="model.id"
          :model="model"
          :price-unit="priceUnit"
        />
      </div>

      <el-empty v-if="!loading && !filteredModels.length" description="No models match filters" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ModelCard from '@/components/models/ModelCard.vue'
import { modelApi } from '@/api/model'
import { FALLBACK_GROUPS, FALLBACK_MODELS, PROVIDER_META } from '@/data/catalogFallback'
import type { CatalogModel, ModelGroup, PriceUnit, ViewMode } from '@/types/model'

const loading = ref(true)
const models = ref<CatalogModel[]>([])
const groups = ref<ModelGroup[]>([])
const searchQuery = ref('')
const filterGroup = ref('')
const filterProvider = ref('')
const filterPricing = ref('')
const priceUnit = ref<PriceUnit>('1M')
const sortBy = ref<'name' | 'provider' | 'price'>('name')
const viewMode = ref<ViewMode>('grid')

const usageCount = computed(
  () => models.value.filter((m) => (m.pricingType || 'usage') === 'usage').length,
)

const providerCounts = computed(() => {
  const map = new Map<string, number>()
  for (const m of models.value) {
    map.set(m.provider, (map.get(m.provider) || 0) + 1)
  }
  return [...map.entries()]
    .map(([code, count]) => ({
      code,
      name: PROVIDER_META[code]?.name || code,
      count,
    }))
    .sort((a, b) => b.count - a.count)
})

const filteredModels = computed(() => {
  let list = models.value.filter((model) => {
    const q = searchQuery.value.trim().toLowerCase()
    if (
      q &&
      !model.displayName.toLowerCase().includes(q) &&
      !model.id.toLowerCase().includes(q)
    ) {
      return false
    }
    if (filterGroup.value && model.groupCode !== filterGroup.value) return false
    if (filterProvider.value && model.provider !== filterProvider.value) return false
    if (filterPricing.value && (model.pricingType || 'usage') !== filterPricing.value) return false
    return true
  })

  list = [...list].sort((a, b) => {
    if (sortBy.value === 'provider') return a.provider.localeCompare(b.provider)
    if (sortBy.value === 'price') {
      return (a.sellInputPricePerMillion || 0) - (b.sellInputPricePerMillion || 0)
    }
    return a.id.localeCompare(b.id)
  })
  return list
})

function formatMult(v: number) {
  return Number.isInteger(v) ? String(v) : String(v)
}

function countByGroup(code: string) {
  return models.value.filter((m) => m.groupCode === code).length
}

async function loadCatalog() {
  loading.value = true
  try {
    const [modelsRes, groupsRes, healthRes] = await Promise.allSettled([
      modelApi.list(),
      modelApi.listGroups(true),
      modelApi.healthMetrics(24, 24),
    ])

    const apiModels =
      modelsRes.status === 'fulfilled' && Array.isArray(modelsRes.value.data)
        ? modelsRes.value.data
        : []

    models.value = apiModels.length ? apiModels : FALLBACK_MODELS

    const apiGroups =
      groupsRes.status === 'fulfilled' && Array.isArray(groupsRes.value.data)
        ? groupsRes.value.data
        : []
    groups.value = apiGroups.length ? apiGroups : FALLBACK_GROUPS

    if (healthRes.status === 'fulfilled' && healthRes.value.data) {
      const health = healthRes.value.data
      models.value = models.value.map((m) => ({
        ...m,
        metrics: health[m.id] || m.metrics,
      }))
    }

    // Enrich sell price if API omitted group pricing
    models.value = models.value.map((m) => {
      if (m.sellInputPricePerMillion != null) return m
      const g = groups.value.find((x) => x.code === m.groupCode)
      const mult = g?.priceMultiplier ?? m.priceMultiplier ?? 1
      return {
        ...m,
        priceMultiplier: mult,
        groupName: m.groupName || g?.name,
        sellInputPricePerMillion: +((m.inputPrice || 0) * mult * 1000).toFixed(4),
        sellOutputPricePerMillion: +((m.outputPrice || 0) * mult * 1000).toFixed(4),
        pricingType: m.pricingType || 'usage',
        labels: m.labels?.length ? m.labels : ['chat', m.provider],
      }
    })
  } finally {
    loading.value = false
  }
}

onMounted(loadCatalog)
</script>

<style scoped lang="scss">
.models-ops {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
  min-height: calc(100vh - 140px);
  align-items: start;
}

.ops-sidebar {
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  padding: 16px 12px;
  position: sticky;
  top: 0;
  max-height: calc(100vh - 120px);
  overflow: auto;
}

.filter-block {
  margin-bottom: 18px;

  h3 {
    margin: 0 0 8px;
    font-size: 12px;
    font-weight: 600;
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }
}

.filter-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #334155;
  text-align: left;

  em {
    font-style: normal;
    color: #6366f1;
    margin-left: 4px;
    font-size: 12px;
  }

  .count {
    color: #94a3b8;
    font-variant-numeric: tabular-nums;
  }

  &:hover {
    background: #f8fafc;
  }

  &.active {
    background: #eef2ff;
    color: #312e81;
    font-weight: 600;
  }
}

.ops-main {
  min-width: 0;
}

.ops-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: baseline;
  gap: 10px;

  h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
  }

  .result-count {
    font-size: 13px;
    color: #64748b;
  }
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-grid {
  display: grid;
  gap: 14px;

  &.grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }

  &.list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .models-ops {
    grid-template-columns: 1fr;
  }

  .ops-sidebar {
    position: static;
    max-height: none;
  }
}
</style>
