export interface UsageMockData {
  summary: {
    totalRequests: number
    totalTokens: number
    totalCost: number
  }
  daily: {
    labels: string[]
    requests: number[]
    tokensK: number[]
    cost: number[]
  }
  records: Array<{
    date: string
    model: string
    requests: number
    inputTokens: number
    outputTokens: number
    cost: number
  }>
  dashboard: {
    apiKeys: number
    requests: number
    tokens: number
    balance: number
    modelDistribution: Array<{ name: string; value: number }>
    recentCalls: Array<{
      time: string
      model: string
      tokens: number
      latency: number
      status: string
    }>
  }
}

let cached: UsageMockData | null = null

/** Load usage/dashboard metrics from public mock JSON (offline demos). */
export async function loadUsageMock(): Promise<UsageMockData> {
  if (cached) return cached
  const res = await fetch('/mocks/usage.json')
  if (!res.ok) {
    throw new Error(`Failed to load usage mock: ${res.status}`)
  }
  cached = (await res.json()) as UsageMockData
  return cached
}
