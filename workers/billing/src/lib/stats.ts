// Ported from UsageRecordMapper.java's raw @Select aggregation queries.
// create_time is stored as a unix-ms INTEGER (Drizzle's timestamp_ms mode), so SQLite's
// DATE() needs the /1000 + 'unixepoch' modifier to read it as a date.

export interface SummaryStatsRow {
  total_requests: number
  successful_requests: number
  failed_requests: number
  total_input_tokens: number | null
  total_output_tokens: number | null
  total_tokens: number | null
  total_cost: number | null
  avg_latency: number | null
}

export interface ModelStatsRow {
  model: string
  request_count: number
  input_tokens: number | null
  output_tokens: number | null
  total_cost: number | null
}

export interface DailyStatsRow {
  date: string
  request_count: number
  total_tokens: number | null
  total_cost: number | null
}

export async function getSummaryStats(db: D1Database, userId: number, startMs: number, endMs: number) {
  const row = await db
    .prepare(
      `SELECT COUNT(*) as total_requests,
              SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_requests,
              SUM(CASE WHEN status = 'error' THEN 1 ELSE 0 END) as failed_requests,
              SUM(input_tokens) as total_input_tokens,
              SUM(output_tokens) as total_output_tokens,
              SUM(total_tokens) as total_tokens,
              SUM(total_cost) as total_cost,
              AVG(latency_ms) as avg_latency
       FROM t_usage_record
       WHERE user_id = ? AND create_time >= ? AND create_time <= ?`,
    )
    .bind(userId, startMs, endMs)
    .first<SummaryStatsRow>()
  return row
}

export async function getModelStats(db: D1Database, userId: number, startMs: number, endMs: number) {
  const { results } = await db
    .prepare(
      `SELECT model,
              COUNT(*) as request_count,
              SUM(input_tokens) as input_tokens,
              SUM(output_tokens) as output_tokens,
              SUM(total_cost) as total_cost
       FROM t_usage_record
       WHERE user_id = ? AND create_time >= ? AND create_time <= ?
       GROUP BY model`,
    )
    .bind(userId, startMs, endMs)
    .all<ModelStatsRow>()
  return results
}

export async function getDailyStats(db: D1Database, userId: number, startMs: number, endMs: number) {
  const { results } = await db
    .prepare(
      `SELECT DATE(create_time / 1000, 'unixepoch') as date,
              COUNT(*) as request_count,
              SUM(total_tokens) as total_tokens,
              SUM(total_cost) as total_cost
       FROM t_usage_record
       WHERE user_id = ? AND create_time >= ? AND create_time <= ?
       GROUP BY DATE(create_time / 1000, 'unixepoch')
       ORDER BY date`,
    )
    .bind(userId, startMs, endMs)
    .all<DailyStatsRow>()
  return results
}
