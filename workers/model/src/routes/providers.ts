import { Hono } from 'hono'
import { and, count, eq } from 'drizzle-orm'
import type { Db } from '../db/client'
import { models, providers } from '../db/schema'
import { ok, fail } from '../lib/response'
import { ErrorCode } from '../lib/error-code'
import { toProviderResponse } from '../dto'

type Env = { Variables: { db: Db } }

export const providerRoutes = new Hono<Env>()

async function modelCountFor(db: Db, providerCode: string) {
  const [row] = await db
    .select({ value: count() })
    .from(models)
    .where(and(eq(models.providerCode, providerCode), eq(models.deleted, false)))
  return row?.value ?? 0
}

providerRoutes.get('/', async (c) => {
  const db = c.get('db')
  const rows = await db.select().from(providers).where(eq(providers.deleted, false))
  const results = await Promise.all(rows.map(async (row) => toProviderResponse(row, await modelCountFor(db, row.providerCode))))
  return ok(c, results)
})

providerRoutes.get('/:providerCode', async (c) => {
  const db = c.get('db')
  const providerCode = c.req.param('providerCode')

  const [row] = await db
    .select()
    .from(providers)
    .where(and(eq(providers.providerCode, providerCode), eq(providers.deleted, false)))
    .limit(1)
  if (!row) return fail(c, ErrorCode.NOT_FOUND, `Provider ${providerCode} not found`)

  return ok(c, toProviderResponse(row, await modelCountFor(db, row.providerCode)))
})

providerRoutes.post('/:providerCode/sync', async (c) => {
  // Placeholder: syncing model catalogs from live provider APIs is a follow-up task,
  // matching the original service's stubbed syncModelsFromProvider behavior.
  const providerCode = c.req.param('providerCode')
  return ok(c, 'Sync started', `Sync started for ${providerCode}`)
})
