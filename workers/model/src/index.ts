import { Hono } from 'hono'
import { createDb, type Db } from './db/client'
import { modelRoutes } from './routes/models'
import { providerRoutes } from './routes/providers'
import { groupRoutes } from './routes/groups'
import { statsRoutes } from './routes/stats'
import { fail, failRaw } from './lib/response'
import { ErrorCode } from './lib/error-code'
import { verifyAccessToken, isAdmin, AuthError, type AuthContext } from './lib/auth'

export interface Bindings {
  DB: D1Database
  /** Shared with ai-panel-user (signs the console JWTs). Set via `wrangler secret put JWT_SECRET`. */
  JWT_SECRET: string
  /** Comma-separated usernames allowed to modify the model catalog. */
  ADMIN_USERNAMES: string
}

type Env = { Bindings: Bindings; Variables: { db: Db; requestId: string; auth: AuthContext } }

const app = new Hono<Env>()

app.use('*', async (c, next) => {
  c.set('requestId', crypto.randomUUID())
  c.set('db', createDb(c.env.DB))
  await next()
})

// Authentication: all routes require a valid user JWT (same token ai-panel-user issues).
// Authorization: read-only requests are open to any signed-in user; anything that
// changes the catalog (POST/PUT/PATCH/DELETE) is limited to ADMIN_USERNAMES.
app.use('*', async (c, next) => {
  let auth: AuthContext
  try {
    auth = await verifyAccessToken(c.req.header('Authorization'), c.env.JWT_SECRET)
  } catch (e) {
    return failRaw(c, 401, e instanceof AuthError ? e.message : 'Not authenticated')
  }
  c.set('auth', auth)
  const method = c.req.method.toUpperCase()
  const readOnly = method === 'GET' || method === 'HEAD' || method === 'OPTIONS'
  if (!readOnly && !isAdmin(auth, c.env.ADMIN_USERNAMES)) {
    return failRaw(c, 403, 'Admin privileges required')
  }
  return next()
})

app.onError((err, c) => {
  console.error(err)
  return fail(c, ErrorCode.INTERNAL_ERROR, 'Internal server error')
})

// Order matters: providers must be mounted before the /:modelId catch-all in modelRoutes.
app.route('/api/v1/models/providers', providerRoutes)
app.route('/api/v1/models', modelRoutes)
app.route('/api/v1/model-groups', groupRoutes)
app.route('/api/v1/stats', statsRoutes)

export default app
