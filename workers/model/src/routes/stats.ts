import { Hono } from 'hono'
import { eq } from 'drizzle-orm'
import type { Db } from '../db/client'
import { models } from '../db/schema'
import { ok } from '../lib/response'

type Env = { Variables: { db: Db } }

export const statsRoutes = new Hono<Env>()

/** Deterministic demo health metrics derived from model id (no request log store in worker). */
function hashSeed(input: string): number {
  let h = 0
  for (let i = 0; i < input.length; i++) h = (h * 31 + input.charCodeAt(i)) >>> 0
  return h
}

statsRoutes.get('/models/health', async (c) => {
  const db = c.get('db')
  const sparkline = Math.min(48, Math.max(1, Number(c.req.query('sparkline') ?? 24)))

  const rows = await db.select({ modelId: models.modelId }).from(models).where(eq(models.deleted, false))

  const health: Record<
    string,
    {
      modelId: string
      successRate: number
      avgLatencySeconds: number
      throughputTokensPerSecond: number
      requestCount: number
      recentStatuses: number[]
    }
  > = {}

  for (const row of rows) {
    const seed = hashSeed(row.modelId)
    const successRate = 0.92 + ((seed % 80) / 1000)
    const avgLatencySeconds = 0.4 + ((seed % 120) / 100)
    const throughput = 40 + (seed % 90)
    const requestCount = 200 + (seed % 1800)
    const recentStatuses = Array.from({ length: sparkline }, (_, i) =>
      (seed + i * 17) % 17 === 0 ? 0 : 1,
    )
    health[row.modelId] = {
      modelId: row.modelId,
      successRate: +successRate.toFixed(4),
      avgLatencySeconds: +avgLatencySeconds.toFixed(2),
      throughputTokensPerSecond: throughput,
      requestCount,
      recentStatuses,
    }
  }

  return ok(c, health)
})
