import { sqliteTable, text, integer, index } from 'drizzle-orm/sqlite-core'

// Ported from ai-panel-key/key-data/.../po/ApiKeyPO.java
export const apiKeys = sqliteTable(
  't_api_key',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    userId: integer('user_id').notNull(),
    keyId: text('key_id').notNull().unique(),
    keyHash: text('key_hash').notNull().unique(),
    keyPrefix: text('key_prefix').notNull(),
    name: text('name').notNull(),
    description: text('description'),
    status: integer('status').notNull().default(1), // 1-active, 0-disabled
    allowedModels: text('allowed_models'), // JSON string array, null = all models
    rateLimitOverride: integer('rate_limit_override'),
    expiresAt: integer('expires_at', { mode: 'timestamp_ms' }),
    lastUsedAt: integer('last_used_at', { mode: 'timestamp_ms' }),
    createdAt: integer('created_at', { mode: 'timestamp_ms' }).notNull(),
    updatedAt: integer('updated_at', { mode: 'timestamp_ms' }).notNull(),
    deleted: integer('deleted', { mode: 'boolean' }).notNull().default(false),
  },
  (table) => [index('idx_api_key_user_id').on(table.userId)],
)

export type ApiKeyRow = typeof apiKeys.$inferSelect
export type NewApiKeyRow = typeof apiKeys.$inferInsert
