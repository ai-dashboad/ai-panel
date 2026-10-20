import { Hono } from 'hono'
import { desc, eq } from 'drizzle-orm'
import type { Db } from '../db/client'
import { subscriptionRequests } from '../db/schema'
import { ok, fail } from '../lib/response'
import { verifyAccessToken, isAdmin, AuthError, type AuthContext } from '../lib/auth'
import { createRequestSchema, reviewRequestSchema, toResponse } from '../dto'

type Bindings = { DB: D1Database; JWT_SECRET: string; ADMIN_USERNAMES: string }
type Env = { Bindings: Bindings; Variables: { db: Db; auth: AuthContext } }

export const requestRoutes = new Hono<Env>()

requestRoutes.use('*', async (c, next) => {
  try {
    const auth = await verifyAccessToken(c.req.header('Authorization'), c.env.JWT_SECRET)
    c.set('auth', auth)
  } catch (e) {
    return fail(c, 401, e instanceof AuthError ? e.message : 'Not authenticated')
  }
  await next()
})

requestRoutes.get('/whoami', async (c) => {
  const auth = c.get('auth')
  return ok(c, { userId: auth.userId, username: auth.username, isAdmin: requireAdmin(c) })
})

requestRoutes.post('/', async (c) => {
  const db = c.get('db')
  const auth = c.get('auth')
  const parsed = createRequestSchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, 400, parsed.error.issues[0]?.message ?? 'Invalid request')
  const input = parsed.data
  const now = new Date()

  await db.insert(subscriptionRequests).values({
    userId: auth.userId,
    username: auth.username,
    email: input.email,
    service: input.service,
    note: input.note,
    status: 'pending',
    createdAt: now,
    updatedAt: now,
  })

  const [row] = await db
    .select()
    .from(subscriptionRequests)
    .where(eq(subscriptionRequests.userId, auth.userId))
    .orderBy(desc(subscriptionRequests.id))
    .limit(1)
  return ok(c, toResponse(row))
})

requestRoutes.get('/mine', async (c) => {
  const db = c.get('db')
  const auth = c.get('auth')
  const rows = await db
    .select()
    .from(subscriptionRequests)
    .where(eq(subscriptionRequests.userId, auth.userId))
    .orderBy(desc(subscriptionRequests.createdAt))
  return ok(c, rows.map(toResponse))
})

requestRoutes.delete('/:id', async (c) => {
  const db = c.get('db')
  const auth = c.get('auth')
  const id = Number(c.req.param('id'))

  const [existing] = await db.select().from(subscriptionRequests).where(eq(subscriptionRequests.id, id)).limit(1)
  if (!existing || existing.userId !== auth.userId) return fail(c, 404, 'Request not found')
  if (existing.status !== 'pending') return fail(c, 409, 'Only pending requests can be cancelled')

  await db
    .update(subscriptionRequests)
    .set({ status: 'cancelled', updatedAt: new Date() })
    .where(eq(subscriptionRequests.id, id))
  return ok(c, null)
})

function requireAdmin(c: { get: (key: 'auth') => AuthContext; env: Bindings }) {
  return isAdmin(c.get('auth').username, c.env.ADMIN_USERNAMES)
}

requestRoutes.get('/', async (c) => {
  if (!requireAdmin(c)) return fail(c, 403, 'Admin access required')

  const db = c.get('db')
  const status = c.req.query('status')
  const rows = status
    ? await db
        .select()
        .from(subscriptionRequests)
        .where(eq(subscriptionRequests.status, status))
        .orderBy(desc(subscriptionRequests.createdAt))
    : await db.select().from(subscriptionRequests).orderBy(desc(subscriptionRequests.createdAt))

  return ok(c, rows.map(toResponse))
})

requestRoutes.put('/:id/review', async (c) => {
  if (!requireAdmin(c)) return fail(c, 403, 'Admin access required')

  const db = c.get('db')
  const auth = c.get('auth')
  const id = Number(c.req.param('id'))
  const parsed = reviewRequestSchema.safeParse(await c.req.json())
  if (!parsed.success) return fail(c, 400, parsed.error.issues[0]?.message ?? 'Invalid request')

  const [existing] = await db.select().from(subscriptionRequests).where(eq(subscriptionRequests.id, id)).limit(1)
  if (!existing) return fail(c, 404, 'Request not found')

  await db
    .update(subscriptionRequests)
    .set({
      status: parsed.data.status,
      adminNote: parsed.data.adminNote,
      reviewedBy: auth.userId,
      reviewedAt: new Date(),
      updatedAt: new Date(),
    })
    .where(eq(subscriptionRequests.id, id))

  const [row] = await db.select().from(subscriptionRequests).where(eq(subscriptionRequests.id, id)).limit(1)
  return ok(c, toResponse(row))
})
