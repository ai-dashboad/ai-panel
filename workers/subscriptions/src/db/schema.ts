import { sqliteTable, text, integer, index } from 'drizzle-orm/sqlite-core'

export const subscriptionRequests = sqliteTable(
  'subscription_request',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    userId: integer('user_id').notNull(),
    username: text('username'),
    email: text('email').notNull(),
    service: text('service').notNull(), // 'claude' | 'chatgpt' | 'other'
    note: text('note'),
    status: text('status').notNull().default('pending'), // pending | approved | rejected | cancelled
    adminNote: text('admin_note'),
    reviewedBy: integer('reviewed_by'),
    reviewedAt: integer('reviewed_at', { mode: 'timestamp_ms' }),
    createdAt: integer('created_at', { mode: 'timestamp_ms' }).notNull(),
    updatedAt: integer('updated_at', { mode: 'timestamp_ms' }).notNull(),
  },
  (table) => [index('idx_subscription_request_user_id').on(table.userId), index('idx_subscription_request_status').on(table.status)],
)

export type SubscriptionRequestRow = typeof subscriptionRequests.$inferSelect
export type NewSubscriptionRequestRow = typeof subscriptionRequests.$inferInsert
