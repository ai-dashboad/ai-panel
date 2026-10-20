import { sqliteTable, text, integer, real, index } from 'drizzle-orm/sqlite-core'

// Ported from ai-panel-model/model-data/.../po/ModelPO.java
export const models = sqliteTable(
  't_model',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    modelId: text('model_id').notNull().unique(),
    displayName: text('display_name').notNull(),
    providerCode: text('provider_code').notNull(),
    modelType: text('model_type').notNull(),
    contextWindow: integer('context_window'),
    maxOutputTokens: integer('max_output_tokens'),
    inputPrice: real('input_price').notNull(),
    outputPrice: real('output_price').notNull(),
    features: text('features').notNull().default('[]'), // JSON string array
    fallbackChain: text('fallback_chain').notNull().default('[]'), // JSON string array
    enabled: integer('enabled', { mode: 'boolean' }).notNull().default(true),
    deprecated: integer('deprecated', { mode: 'boolean' }).notNull().default(false),
    description: text('description'),
    createTime: integer('create_time', { mode: 'timestamp_ms' }).notNull(),
    updateTime: integer('update_time', { mode: 'timestamp_ms' }).notNull(),
    deleted: integer('deleted', { mode: 'boolean' }).notNull().default(false),
  },
  (table) => [
    index('idx_model_provider_code').on(table.providerCode),
    index('idx_model_type').on(table.modelType),
  ],
)

// Ported from ai-panel-model/model-data/.../po/ProviderPO.java
export const providers = sqliteTable('t_provider', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  providerCode: text('provider_code').notNull().unique(),
  providerName: text('provider_name').notNull(),
  baseUrl: text('base_url').notNull(),
  apiKey: text('api_key'), // never exposed via the API response
  priority: integer('priority').notNull().default(0),
  rateLimitPerMinute: integer('rate_limit_per_minute'),
  timeout: integer('timeout'),
  enabled: integer('enabled', { mode: 'boolean' }).notNull().default(true),
  healthStatus: integer('health_status').notNull().default(0),
  lastHealthCheck: integer('last_health_check', { mode: 'timestamp_ms' }),
  config: text('config'),
  createTime: integer('create_time', { mode: 'timestamp_ms' }).notNull(),
  updateTime: integer('update_time', { mode: 'timestamp_ms' }).notNull(),
  deleted: integer('deleted', { mode: 'boolean' }).notNull().default(false),
})

export const modelGroups = sqliteTable('t_model_group', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  groupCode: text('group_code').notNull().unique(),
  groupName: text('group_name').notNull(),
  priceMultiplier: real('price_multiplier').notNull().default(1),
  description: text('description'),
  enabled: integer('enabled', { mode: 'boolean' }).notNull().default(true),
  sortOrder: integer('sort_order').notNull().default(100),
  createTime: integer('create_time', { mode: 'timestamp_ms' }).notNull(),
  updateTime: integer('update_time', { mode: 'timestamp_ms' }).notNull(),
  deleted: integer('deleted', { mode: 'boolean' }).notNull().default(false),
})

export const modelGroupMembers = sqliteTable(
  't_model_group_member',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    groupId: integer('group_id').notNull(),
    modelId: text('model_id').notNull(),
    createTime: integer('create_time', { mode: 'timestamp_ms' }).notNull(),
  },
  (table) => [index('idx_group_member_model').on(table.modelId)],
)

export type ModelRow = typeof models.$inferSelect
export type NewModelRow = typeof models.$inferInsert
export type ProviderRow = typeof providers.$inferSelect
export type NewProviderRow = typeof providers.$inferInsert
export type ModelGroupRow = typeof modelGroups.$inferSelect
