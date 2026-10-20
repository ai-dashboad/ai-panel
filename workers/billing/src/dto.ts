import { z } from 'zod'
import type { UsageRecordRow } from './db/schema'

// Ported from RecordUsageRequest.java
export const recordUsageSchema = z.object({
  userId: z.number().int(),
  apiKeyId: z.number().int().optional(),
  requestId: z.string().min(1, 'Request ID is required'),
  model: z.string().min(1, 'Model is required'),
  provider: z.string().min(1, 'Provider is required'),
  inputTokens: z.number().int().min(0),
  outputTokens: z.number().int().min(0),
  latencyMs: z.number().int().optional(),
  status: z.string().optional(),
})
export type RecordUsageInput = z.infer<typeof recordUsageSchema>

// Mirrors BillingRecordResponse.java
export function toBillingRecordResponse(row: UsageRecordRow) {
  return {
    id: row.id,
    requestId: row.requestId,
    model: row.model,
    provider: row.provider,
    inputTokens: row.inputTokens,
    outputTokens: row.outputTokens,
    totalTokens: row.totalTokens,
    inputCost: row.inputCost,
    outputCost: row.outputCost,
    totalCost: row.totalCost,
    latencyMs: row.latencyMs,
    status: row.status,
    createTime: row.createTime,
  }
}
