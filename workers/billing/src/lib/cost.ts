// Ported from BillingServiceImpl.calculateCost — same default per-1000-token prices.
const DEFAULT_INPUT_PRICE = 0.01
const DEFAULT_OUTPUT_PRICE = 0.03

function calculateCost(tokens: number, pricePerThousand: number): number {
  if (!tokens || tokens <= 0) return 0
  return Math.round((pricePerThousand * tokens) / 1000 * 1e6) / 1e6
}

export function calculateUsageCost(inputTokens: number, outputTokens: number) {
  const inputCost = calculateCost(inputTokens, DEFAULT_INPUT_PRICE)
  const outputCost = calculateCost(outputTokens, DEFAULT_OUTPUT_PRICE)
  return { inputCost, outputCost, totalCost: inputCost + outputCost }
}
