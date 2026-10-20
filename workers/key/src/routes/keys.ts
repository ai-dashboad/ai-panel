import { Hono } from 'hono'
import { and, count, desc, eq } from 'drizzle-orm'
import type { Db } from '../db/client'
import { apiKeys } from '../db/schema'
import { ok, fail, failRaw } from '../lib/response'
import { ErrorCode } from '../lib/error-code'
import { ApiKeyCache } from '../lib/cache'
import type { AuthContext } from '../lib/auth'
import { pageResultOf } from '../lib/page-result'
import {
  createApiKeySchema,
  updateApiKeySchema,
  validateApiKeySchema,
  toApiKeyResponse,
} from '../dto'
import { generateApiKey, hashApiKey, getDisplayPrefix, extractKeyId, isValidApiKeyFormat } from '../lib/api-key'

type Env = { Variables: { db: Db; cache: ApiKeyCache; auth: AuthContext } }

export const keyRoutes = new Hono<Env>()

const MAX_KEYS_PER_USER = 10
// The authenticated user id is set by the auth middleware in index.ts from the
// verified JWT (subject claim). Client-supplied X-User-ID headers are ignored.
function requireUserId(c: { get: (key: 'auth') => AuthContext | undefined }): number | null {
  return c.get('auth')?.userId ?? null
}

async function issueNewKey(
  db: Db,
  userId: number,
  fields: {
    name: string
    description?: string
    allowedModels?: string[]
    rateLimitPerMinute?: number
    expiresAt?: Date
  },
) {
  const fullKey = generateApiKey()
  const keyHash = await hashApiKey(fullKey)
  const keyId = extractKeyId(fullKey)
  const keyPrefix = getDisplayPrefix(fullKey)
  const now = new Date()

  await db.insert(apiKeys).values({
    userId,
    keyId,
    keyHash,
    keyPrefix,
    name: fields.name,
    description: fields.description,
    status: 1,
    allowedModels: fields.allowedModels ? JSON.stringify(fields.allowedModels) : undefined,
    rateLimitOverride: fields.rateLimitPerMinute,
    expiresAt: fields.expiresAt,
    createdAt: now,
    updatedAt: now,
  })

  const [row] = await db.select().from(apiKeys).where(eq(apiKeys.keyId, keyId)).limit(1)
  return { row, fullKey }
}

keyRoutes.post('/', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const parsed = createApiKeySchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, ErrorCode.PARAM_ERROR, parsed.error.issues[0]?.message)
  const input = parsed.data

  const [{ value: keyCount }] = await db
    .select({ value: count() })
    .from(apiKeys)
    .where(and(eq(apiKeys.userId, userId), eq(apiKeys.deleted, false)))
  if (keyCount >= MAX_KEYS_PER_USER) {
    return fail(c, ErrorCode.API_KEY_LIMIT_REACHED, `Maximum ${MAX_KEYS_PER_USER} API Keys allowed per user`)
  }

  const { row, fullKey } = await issueNewKey(db, userId, input)
  return ok(c, toApiKeyResponse(row, fullKey))
})

keyRoutes.get('/', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const pageNum = Math.max(1, Number(c.req.query('pageNum') ?? 1))
  const pageSize = Math.max(1, Number(c.req.query('pageSize') ?? 20))

  const [{ value: total }] = await db
    .select({ value: count() })
    .from(apiKeys)
    .where(and(eq(apiKeys.userId, userId), eq(apiKeys.deleted, false)))

  const rows = await db
    .select()
    .from(apiKeys)
    .where(and(eq(apiKeys.userId, userId), eq(apiKeys.deleted, false)))
    .orderBy(desc(apiKeys.createdAt))
    .limit(pageSize)
    .offset((pageNum - 1) * pageSize)

  return ok(c, pageResultOf(rows.map((row) => toApiKeyResponse(row)), pageNum, pageSize, total))
})

keyRoutes.get('/validate', async (c) => {
  // Registered before /:keyId so the literal path wins; see index.ts for validate-related routing notes.
  const db = c.get('db')
  const cache = c.get('cache')
  const key = c.req.query('key')
  const model = c.req.query('model') ?? undefined
  if (!key) return fail(c, ErrorCode.PARAM_ERROR, 'API Key is required')
  return ok(c, await validateKey(db, cache, key, model))
})

keyRoutes.post('/internal/validate', async (c) => {
  const db = c.get('db')
  const cache = c.get('cache')
  const parsed = validateApiKeySchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, ErrorCode.PARAM_ERROR, parsed.error.issues[0]?.message)
  return ok(c, await validateKey(db, cache, parsed.data.apiKey, parsed.data.model))
})

keyRoutes.post('/ensure-default', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const [existing] = await db
    .select()
    .from(apiKeys)
    .where(and(eq(apiKeys.userId, userId), eq(apiKeys.deleted, false)))
    .orderBy(apiKeys.createdAt)
    .limit(1)
  if (existing) return ok(c, toApiKeyResponse(existing))

  const { row, fullKey } = await issueNewKey(db, userId, {
    name: 'Default API Key',
    description: 'Auto-generated API key for quick start',
  })
  return ok(c, toApiKeyResponse(row, fullKey))
})

keyRoutes.get('/:keyId', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const keyId = c.req.param('keyId')
  const row = await findOwnedKey(db, userId, keyId)
  if (!row) return fail(c, ErrorCode.API_KEY_NOT_FOUND, `API Key ${keyId} not found`)
  return ok(c, toApiKeyResponse(row))
})

keyRoutes.put('/:keyId', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const cache = c.get('cache')
  const keyId = c.req.param('keyId')
  const existing = await findOwnedKey(db, userId, keyId)
  if (!existing) return fail(c, ErrorCode.API_KEY_NOT_FOUND, `API Key ${keyId} not found`)

  const parsed = updateApiKeySchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, ErrorCode.PARAM_ERROR, parsed.error.issues[0]?.message)
  const input = parsed.data

  await db
    .update(apiKeys)
    .set({
      name: input.name ?? existing.name,
      description: input.description ?? existing.description,
      allowedModels: input.allowedModels ? JSON.stringify(input.allowedModels) : existing.allowedModels,
      rateLimitOverride: input.rateLimitPerMinute ?? existing.rateLimitOverride,
      expiresAt: input.expiresAt === undefined ? existing.expiresAt : input.expiresAt,
      status: input.status ?? existing.status,
      updatedAt: new Date(),
    })
    .where(eq(apiKeys.keyId, keyId))

  await cache.invalidateByKeyId(keyId)

  const row = await findOwnedKey(db, userId, keyId)
  return ok(c, toApiKeyResponse(row!))
})

keyRoutes.delete('/:keyId', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const cache = c.get('cache')
  const keyId = c.req.param('keyId')
  const existing = await findOwnedKey(db, userId, keyId)
  if (!existing) return fail(c, ErrorCode.API_KEY_NOT_FOUND, `API Key ${keyId} not found`)

  await db.update(apiKeys).set({ deleted: true, updatedAt: new Date() }).where(eq(apiKeys.keyId, keyId))
  await cache.invalidateByKeyId(keyId)

  return ok(c, null)
})

keyRoutes.post('/:keyId/regenerate', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const cache = c.get('cache')
  const oldKeyId = c.req.param('keyId')
  const existing = await findOwnedKey(db, userId, oldKeyId)
  if (!existing) return fail(c, ErrorCode.API_KEY_NOT_FOUND, `API Key ${oldKeyId} not found`)

  const fullKey = generateApiKey()
  const keyHash = await hashApiKey(fullKey)
  const newKeyId = extractKeyId(fullKey)
  const keyPrefix = getDisplayPrefix(fullKey)

  await db
    .update(apiKeys)
    .set({ keyId: newKeyId, keyHash, keyPrefix, updatedAt: new Date() })
    .where(eq(apiKeys.id, existing.id))

  await cache.invalidateByKeyId(oldKeyId)

  const [row] = await db.select().from(apiKeys).where(eq(apiKeys.id, existing.id)).limit(1)
  return ok(c, toApiKeyResponse(row, fullKey))
})

keyRoutes.get('/:keyId/usage', async (c) => {
  const userId = requireUserId(c)
  if (userId === null) return failRaw(c, 401, 'Not authenticated')

  const db = c.get('db')
  const keyId = c.req.param('keyId')
  const existing = await findOwnedKey(db, userId, keyId)
  if (!existing) return fail(c, ErrorCode.API_KEY_NOT_FOUND, `API Key ${keyId} not found`)

  // Stubbed pending real usage stats from ai-panel-billing, matching the original
  // service's TODO ("Implement actual usage statistics from billing service").
  return ok(c, {
    keyId,
    requestsLimit: 10000,
    requestsUsed: 0,
    requestsRemaining: 10000,
    tokensLimit: 1000000,
    tokensUsed: 0,
    tokensRemaining: 1000000,
  })
})

async function findOwnedKey(db: Db, userId: number, keyId: string) {
  const [row] = await db
    .select()
    .from(apiKeys)
    .where(and(eq(apiKeys.keyId, keyId), eq(apiKeys.userId, userId), eq(apiKeys.deleted, false)))
    .limit(1)
  return row
}

async function validateKey(db: Db, cache: ApiKeyCache, apiKey: string, model?: string) {
  if (!isValidApiKeyFormat(apiKey)) {
    return { valid: false, errorCode: ErrorCode.API_KEY_INVALID.code, errorMessage: 'Invalid API Key format' }
  }

  const keyHash = await hashApiKey(apiKey)
  const cached = await cache.getValidation(keyHash)
  if (cached) return applyModelCheck(cached, model)

  const [row] = await db
    .select()
    .from(apiKeys)
    .where(and(eq(apiKeys.keyHash, keyHash), eq(apiKeys.deleted, false)))
    .limit(1)
  if (!row) {
    return { valid: false, errorCode: ErrorCode.API_KEY_INVALID.code, errorMessage: 'API Key not found' }
  }
  if (row.status !== 1) {
    return { valid: false, errorCode: ErrorCode.API_KEY_DISABLED.code, errorMessage: 'API Key is disabled' }
  }
  if (row.expiresAt && row.expiresAt.getTime() < Date.now()) {
    return { valid: false, errorCode: ErrorCode.API_KEY_EXPIRED.code, errorMessage: 'API Key has expired' }
  }

  const allowedModels = row.allowedModels ? (JSON.parse(row.allowedModels) as string[]) : null
  const validation = {
    valid: true,
    userId: row.userId,
    keyId: row.keyId,
    planType: 'FREE', // TODO: fetch actual plan from ai-panel-user once that call is wired up
    allowedModels,
    rateLimitPerMinute: row.rateLimitOverride,
  }

  await cache.cacheValidation(keyHash, validation)
  return applyModelCheck(validation, model)
}

function applyModelCheck<T extends { valid: boolean; allowedModels: string[] | null }>(
  validation: T,
  model?: string,
) {
  if (!model || !validation.valid) return validation
  const allowedModels = validation.allowedModels
  const modelAllowed = !allowedModels || allowedModels.length === 0 || allowedModels.includes(model)
  return { ...validation, modelAllowed }
}
