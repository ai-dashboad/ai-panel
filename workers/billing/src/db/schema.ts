import { sqliteTable, text, integer, real, index } from 'drizzle-orm/sqlite-core'

// Ported from ai-panel-billing/billing-data/.../po/UsageRecordPO.java
// The original Java service noted this table as "Sharded by user_id and month" via
// ShardingSphere, but that sharding config was never wired into the running service
// (confirmed: no ShardingSphere dependency in billing's actual pom/config) — dropped here.
export const usageRecords = sqliteTable(
  't_usage_record',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    userId: integer('user_id').notNull(),
    apiKeyId: integer('api_key_id'),
    requestId: text('request_id').notNull(),
    model: text('model').notNull(),
    provider: text('provider').notNull(),
    inputTokens: integer('input_tokens').notNull(),
    outputTokens: integer('output_tokens').notNull(),
    totalTokens: integer('total_tokens').notNull(),
    inputCost: real('input_cost').notNull(),
    outputCost: real('output_cost').notNull(),
    totalCost: real('total_cost').notNull(),
    latencyMs: integer('latency_ms'),
    status: text('status').notNull(),
    createTime: integer('create_time', { mode: 'timestamp_ms' }).notNull(),
  },
  (table) => [
    index('idx_usage_record_user_id').on(table.userId),
    index('idx_usage_record_create_time').on(table.createTime),
  ],
)

// Ported from UserBalancePO.java — not yet wired up to any route (see BillingServiceImpl
// stubs); kept for schema parity when real balance tracking is implemented later.
export const userBalances = sqliteTable('t_user_balance', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  userId: integer('user_id').notNull().unique(),
  balance: real('balance').notNull().default(0),
  totalCharged: real('total_charged').notNull().default(0),
  totalUsed: real('total_used').notNull().default(0),
  version: integer('version').notNull().default(0),
  createTime: integer('create_time', { mode: 'timestamp_ms' }).notNull(),
  updateTime: integer('update_time', { mode: 'timestamp_ms' }).notNull(),
})

// Ported from BalanceTransactionPO.java — same not-yet-wired-up status as userBalances.
export const balanceTransactions = sqliteTable('t_balance_transaction', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  userId: integer('user_id').notNull(),
  transactionType: text('transaction_type').notNull(), // CHARGE, DEDUCT, REFUND
  amount: real('amount').notNull(),
  balanceAfter: real('balance_after').notNull(),
  referenceId: text('reference_id'),
  description: text('description'),
  createTime: integer('create_time', { mode: 'timestamp_ms' }).notNull(),
})

export type UsageRecordRow = typeof usageRecords.$inferSelect
export type NewUsageRecordRow = typeof usageRecords.$inferInsert
