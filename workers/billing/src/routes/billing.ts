import { Hono } from 'hono'
import { count, desc, eq } from 'drizzle-orm'
import type { Db } from '../db/client'
import { usageRecords } from '../db/schema'
import { ok, fail, failRaw } from '../lib/response'
import { isAdmin, type AuthContext } from '../lib/auth'
import { ErrorCode } from '../lib/error-code'
import { pageResultOf } from '../lib/page-result'
import { recordUsageSchema, toBillingRecordResponse } from '../dto'
import { calculateUsageCost } from '../lib/cost'
import { getSummaryStats, getModelStats, getDailyStats } from '../lib/stats'
import { checkQuota } from '../lib/quota'

type Env = {
  Bindings: { DB: D1Database; DEFAULT_MONTHLY_QUOTA_TOKENS: string; ADMIN_USERNAMES: string }
  Variables: { db: Db; auth: AuthContext | null }
}

export const billingRoutes = new Hono<Env>()

type UserCtx = {
  get: (key: 'auth') => AuthContext | null
  env: { ADMIN_USERNAMES: string }
  req: { query: (name: string) => string | undefined }
}

/**
 * Resolves which user's data a signed-in caller may read. Defaults to the caller's own
 * id (from the verified JWT); an explicit ?userId= for someone else requires admin.
 * Returns null when the caller is not allowed.
 */
function resolveTargetUserId(c: UserCtx): number | null {
  const auth = c.get('auth')
  if (!auth) return null
  const raw = c.req.query('userId')
  if (raw === undefined || raw === '') return auth.userId
  const requested = Number(raw)
  if (!Number.isSafeInteger(requested)) return null
  if (requested === auth.userId) return requested
  return isAdmin(auth.username, c.env.ADMIN_USERNAMES) ? requested : null
}

billingRoutes.get('/quota-check', async (c) => {
  const userId = Number(c.req.query('userId'))
  if (!Number.isFinite(userId)) return fail(c, ErrorCode.PARAM_ERROR, 'userId is required')

  const quotaLimit = Number(c.env.DEFAULT_MONTHLY_QUOTA_TOKENS)
  const result = await checkQuota(c.env.DB, userId, quotaLimit, new Date())
  return ok(c, result)
})

const DAY_MS = 24 * 60 * 60 * 1000

function roundCost(value: number | null | undefined): number {
  return Math.round((value ?? 0) * 1e6) / 1e6
}

function parseDateRange(c: { req: { query: (name: string) => string | undefined } }) {
  const now = Date.now()
  const endDate = c.req.query('endDate')
  const startDate = c.req.query('startDate')
  const endMs = endDate ? new Date(endDate).getTime() + DAY_MS - 1 : now
  const startMs = startDate ? new Date(startDate).getTime() : now - 30 * DAY_MS
  return { startMs, endMs }
}

billingRoutes.post('/usage', async (c) => {
  const db = c.get('db')
  const parsed = recordUsageSchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, ErrorCode.PARAM_ERROR, parsed.error.issues[0]?.message)
  const input = parsed.data

  const { inputCost, outputCost, totalCost } = calculateUsageCost(input.inputTokens, input.outputTokens)

  await db.insert(usageRecords).values({
    userId: input.userId,
    apiKeyId: input.apiKeyId,
    requestId: input.requestId,
    model: input.model,
    provider: input.provider,
    inputTokens: input.inputTokens,
    outputTokens: input.outputTokens,
    totalTokens: input.inputTokens + input.outputTokens,
    inputCost,
    outputCost,
    totalCost,
    latencyMs: input.latencyMs,
    status: input.status ?? 'success',
    createTime: new Date(),
  })

  return ok(c, null)
})

billingRoutes.get('/usage/summary', async (c) => {
  const userId = resolveTargetUserId(c)
  if (userId === null) return failRaw(c, 403, 'Not allowed to view this user')

  const { startMs, endMs } = parseDateRange(c)
  const d1 = c.env.DB

  const [summary, modelStats, dailyStats] = await Promise.all([
    getSummaryStats(d1, userId, startMs, endMs),
    getModelStats(d1, userId, startMs, endMs),
    getDailyStats(d1, userId, startMs, endMs),
  ])

  const usageByModel: Record<string, unknown> = {}
  for (const row of modelStats) {
    usageByModel[row.model] = {
      requestCount: row.request_count,
      inputTokens: row.input_tokens ?? 0,
      outputTokens: row.output_tokens ?? 0,
      cost: roundCost(row.total_cost),
    }
  }

  const usageByDay = dailyStats.map((row) => ({
    date: row.date,
    requestCount: row.request_count,
    totalTokens: row.total_tokens ?? 0,
    cost: roundCost(row.total_cost),
  }))

  return ok(c, {
    userId,
    totalRequests: summary?.total_requests ?? 0,
    successfulRequests: summary?.successful_requests ?? 0,
    failedRequests: summary?.failed_requests ?? 0,
    totalInputTokens: summary?.total_input_tokens ?? 0,
    totalOutputTokens: summary?.total_output_tokens ?? 0,
    totalTokens: summary?.total_tokens ?? 0,
    totalCost: roundCost(summary?.total_cost),
    averageLatencyMs: summary?.avg_latency ?? 0,
    usageByModel,
    usageByDay,
  })
})

billingRoutes.get('/records', async (c) => {
  const userId = resolveTargetUserId(c)
  if (userId === null) return failRaw(c, 403, 'Not allowed to view this user')

  const db = c.get('db')
  const pageNum = Math.max(1, Number(c.req.query('page') ?? 1))
  const pageSize = Math.max(1, Number(c.req.query('size') ?? 20))

  const [{ value: total }] = await db.select({ value: count() }).from(usageRecords).where(eq(usageRecords.userId, userId))

  const rows = await db
    .select()
    .from(usageRecords)
    .where(eq(usageRecords.userId, userId))
    .orderBy(desc(usageRecords.createTime))
    .limit(pageSize)
    .offset((pageNum - 1) * pageSize)

  return ok(c, pageResultOf(rows.map(toBillingRecordResponse), pageNum, pageSize, total))
})

// getBalance/addCredits are ported as stubs, matching BillingServiceImpl's current
// behavior exactly: balance is always reported as 0, and credits are never persisted.
// t_user_balance/t_balance_transaction are unused until real balance tracking lands.
billingRoutes.get('/balance', async (c) => {
  const userId = resolveTargetUserId(c)
  if (userId === null) return failRaw(c, 403, 'Not allowed to view this user')
  return ok(c, 0)
})

billingRoutes.post('/credits', async (c) => {
  const auth = c.get('auth')
  if (!auth || !isAdmin(auth.username, c.env.ADMIN_USERNAMES)) return failRaw(c, 403, 'Admin privileges required')
  const userId = Number(c.req.query('userId'))
  const amount = Number(c.req.query('amount'))
  if (!Number.isFinite(userId) || !Number.isFinite(amount)) {
    return fail(c, ErrorCode.PARAM_ERROR, 'userId and amount are required')
  }
  return ok(c, null)
})
