import { Hono } from 'hono'
import { and, asc, count, eq } from 'drizzle-orm'
import type { Db } from '../db/client'
import { modelGroupMembers, modelGroups } from '../db/schema'
import { ok, fail } from '../lib/response'
import { ErrorCode } from '../lib/error-code'

type Env = { Variables: { db: Db } }

export const groupRoutes = new Hono<Env>()

groupRoutes.get('/', async (c) => {
  const db = c.get('db')
  const enabledOnly = c.req.query('enabledOnly') !== 'false'

  const conditions = [eq(modelGroups.deleted, false)]
  if (enabledOnly) conditions.push(eq(modelGroups.enabled, true))

  const rows = await db
    .select()
    .from(modelGroups)
    .where(and(...conditions))
    .orderBy(asc(modelGroups.sortOrder))

  const results = await Promise.all(
    rows.map(async (row) => {
      const [{ value: modelCount }] = await db
        .select({ value: count() })
        .from(modelGroupMembers)
        .where(eq(modelGroupMembers.groupId, row.id))
      return {
        id: row.id,
        code: row.groupCode,
        name: row.groupName,
        priceMultiplier: row.priceMultiplier,
        description: row.description,
        enabled: row.enabled,
        sortOrder: row.sortOrder,
        modelCount,
      }
    }),
  )

  return ok(c, results)
})

groupRoutes.get('/:code', async (c) => {
  const db = c.get('db')
  const code = c.req.param('code')
  const [row] = await db
    .select()
    .from(modelGroups)
    .where(and(eq(modelGroups.groupCode, code), eq(modelGroups.deleted, false)))
    .limit(1)
  if (!row) return fail(c, ErrorCode.MODEL_NOT_FOUND, `Group ${code} not found`)

  const members = await db
    .select({ modelId: modelGroupMembers.modelId })
    .from(modelGroupMembers)
    .where(eq(modelGroupMembers.groupId, row.id))

  return ok(c, {
    id: row.id,
    code: row.groupCode,
    name: row.groupName,
    priceMultiplier: row.priceMultiplier,
    description: row.description,
    enabled: row.enabled,
    sortOrder: row.sortOrder,
    modelCount: members.length,
    modelIds: members.map((m) => m.modelId),
  })
})
