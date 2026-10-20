// Uniform per-employee quota enforcement: everyone gets the same starting monthly
// token allowance (DEFAULT_MONTHLY_QUOTA_TOKENS), reset on a calendar-month boundary.
// There is no per-user override yet — that's the next increment if it's ever needed.
export interface QuotaCheckResult {
  allowed: boolean
  used: number
  limit: number
  remaining: number
  periodStart: number
  periodEnd: number
}

export function currentCalendarMonthRange(now: Date): { start: number; end: number } {
  const start = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1)
  const end = Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 1) - 1
  return { start, end }
}

export async function checkQuota(db: D1Database, userId: number, quotaLimit: number, now: Date): Promise<QuotaCheckResult> {
  const { start, end } = currentCalendarMonthRange(now)

  const row = await db
    .prepare(
      `SELECT COALESCE(SUM(total_tokens), 0) as used
       FROM t_usage_record
       WHERE user_id = ? AND create_time >= ? AND create_time <= ?`,
    )
    .bind(userId, start, end)
    .first<{ used: number }>()

  const used = row?.used ?? 0
  const remaining = Math.max(0, quotaLimit - used)

  return {
    allowed: used < quotaLimit,
    used,
    limit: quotaLimit,
    remaining,
    periodStart: start,
    periodEnd: end,
  }
}
