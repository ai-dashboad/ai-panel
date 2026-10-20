import { Hono } from 'hono'
import { and, eq } from 'drizzle-orm'
import type { Db } from '../db/client'
import { modelGroupMembers, modelGroups, models, providers } from '../db/schema'
import { ok, fail } from '../lib/response'
import { ErrorCode } from '../lib/error-code'
import {
  createModelSchema,
  updateModelSchema,
  toModelDetailResponse,
  toCatalogModelResponse,
  type GroupPricing,
} from '../dto'

type Env = { Variables: { db: Db } }

export const modelRoutes = new Hono<Env>()

async function providerNameFor(db: Db, providerCode: string) {
  const [provider] = await db
    .select({ providerName: providers.providerName })
    .from(providers)
    .where(and(eq(providers.providerCode, providerCode), eq(providers.deleted, false)))
    .limit(1)
  return provider?.providerName
}

async function loadPrimaryGroupMap(db: Db): Promise<Map<string, GroupPricing>> {
  try {
    const members = await db
      .select({
        modelId: modelGroupMembers.modelId,
        groupCode: modelGroups.groupCode,
        groupName: modelGroups.groupName,
        priceMultiplier: modelGroups.priceMultiplier,
        sortOrder: modelGroups.sortOrder,
      })
      .from(modelGroupMembers)
      .innerJoin(modelGroups, eq(modelGroupMembers.groupId, modelGroups.id))
      .where(and(eq(modelGroups.deleted, false), eq(modelGroups.enabled, true)))

    const map = new Map<string, GroupPricing & { sortOrder: number }>()
    for (const m of members) {
      const existing = map.get(m.modelId)
      if (!existing || m.sortOrder < existing.sortOrder) {
        map.set(m.modelId, {
          groupCode: m.groupCode,
          groupName: m.groupName,
          priceMultiplier: m.priceMultiplier,
          sortOrder: m.sortOrder,
        })
      }
    }
    return map
  } catch {
    return new Map()
  }
}

modelRoutes.get('/', async (c) => {
  const db = c.get('db')
  const provider = c.req.query('provider')
  const type = c.req.query('type')

  const conditions = [eq(models.deleted, false)]
  if (provider) conditions.push(eq(models.providerCode, provider))
  if (type) conditions.push(eq(models.modelType, type))

  const rows = await db
    .select()
    .from(models)
    .where(and(...conditions))

  const groupMap = await loadPrimaryGroupMap(db)
  const results = rows.map((row) => toCatalogModelResponse(row, groupMap.get(row.modelId) ?? null))
  return ok(c, results)
})

modelRoutes.get('/:modelId', async (c) => {
  const db = c.get('db')
  const modelId = c.req.param('modelId')

  const [row] = await db
    .select()
    .from(models)
    .where(and(eq(models.modelId, modelId), eq(models.deleted, false)))
    .limit(1)

  if (!row) return fail(c, ErrorCode.MODEL_NOT_FOUND)
  return ok(c, toModelDetailResponse(row, await providerNameFor(db, row.providerCode)))
})

modelRoutes.post('/', async (c) => {
  const db = c.get('db')
  const parsed = createModelSchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, ErrorCode.PARAM_ERROR, parsed.error.issues[0]?.message)

  const input = parsed.data
  const [existing] = await db
    .select({ id: models.id })
    .from(models)
    .where(and(eq(models.modelId, input.modelId), eq(models.deleted, false)))
    .limit(1)
  if (existing) return fail(c, ErrorCode.CONFLICT, `Model ${input.modelId} already exists`)

  const now = new Date()
  await db.insert(models).values({
    modelId: input.modelId,
    displayName: input.displayName,
    providerCode: input.providerCode,
    modelType: input.modelType,
    contextWindow: input.contextWindow,
    maxOutputTokens: input.maxOutputTokens,
    inputPrice: input.inputPrice,
    outputPrice: input.outputPrice,
    features: JSON.stringify(input.features ?? []),
    fallbackChain: JSON.stringify(input.fallbackChain ?? []),
    description: input.description,
    createTime: now,
    updateTime: now,
  })

  const [row] = await db
    .select()
    .from(models)
    .where(eq(models.modelId, input.modelId))
    .limit(1)
  return ok(c, toModelDetailResponse(row, await providerNameFor(db, row.providerCode)))
})

modelRoutes.put('/:modelId', async (c) => {
  const db = c.get('db')
  const modelId = c.req.param('modelId')
  const parsed = updateModelSchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, ErrorCode.PARAM_ERROR, parsed.error.issues[0]?.message)

  const [existing] = await db
    .select()
    .from(models)
    .where(and(eq(models.modelId, modelId), eq(models.deleted, false)))
    .limit(1)
  if (!existing) return fail(c, ErrorCode.MODEL_NOT_FOUND)

  const input = parsed.data
  await db
    .update(models)
    .set({
      displayName: input.displayName ?? existing.displayName,
      contextWindow: input.contextWindow ?? existing.contextWindow,
      maxOutputTokens: input.maxOutputTokens ?? existing.maxOutputTokens,
      inputPrice: input.inputPrice ?? existing.inputPrice,
      outputPrice: input.outputPrice ?? existing.outputPrice,
      features: input.features ? JSON.stringify(input.features) : existing.features,
      fallbackChain: input.fallbackChain ? JSON.stringify(input.fallbackChain) : existing.fallbackChain,
      description: input.description ?? existing.description,
      enabled: input.enabled ?? existing.enabled,
      deprecated: input.deprecated ?? existing.deprecated,
      updateTime: new Date(),
    })
    .where(eq(models.modelId, modelId))

  const [row] = await db.select().from(models).where(eq(models.modelId, modelId)).limit(1)
  return ok(c, toModelDetailResponse(row, await providerNameFor(db, row.providerCode)))
})

modelRoutes.delete('/:modelId', async (c) => {
  const db = c.get('db')
  const modelId = c.req.param('modelId')

  const [existing] = await db
    .select({ id: models.id })
    .from(models)
    .where(and(eq(models.modelId, modelId), eq(models.deleted, false)))
    .limit(1)
  if (!existing) return fail(c, ErrorCode.MODEL_NOT_FOUND)

  await db.update(models).set({ deleted: true, updateTime: new Date() }).where(eq(models.modelId, modelId))
  return ok(c, null)
})
