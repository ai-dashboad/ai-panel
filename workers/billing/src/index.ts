import { Hono } from 'hono'
import { createDb, type Db } from './db/client'
import { billingRoutes } from './routes/billing'
import { fail, failRaw } from './lib/response'
import { ErrorCode } from './lib/error-code'
import {
  verifyAccessToken,
  isValidInternalToken,
  AuthError,
  INTERNAL_TOKEN_HEADER,
  type AuthContext,
} from './lib/auth'

export interface Bindings {
  DB: D1Database
  DEFAULT_MONTHLY_QUOTA_TOKENS: string
  /** Shared with ai-panel-user (signs the console JWTs). Set via `wrangler secret put JWT_SECRET`. */
  JWT_SECRET: string
  /** Shared secret for service-to-service calls (dispatch -> quota check / usage recording). */
  INTERNAL_API_TOKEN: string
  /** Comma-separated usernames allowed to view other users' usage and add credits. */
  ADMIN_USERNAMES: string
}

type Env = { Bindings: Bindings; Variables: { db: Db; requestId: string; auth: AuthContext | null } }

const app = new Hono<Env>()

// Called by ai-panel-dispatch (BillingQuotaService) and other backend services,
// never by end users: protected by the internal token instead of a user JWT.
const INTERNAL_ROUTES = new Set(['GET /api/v1/billing/quota-check', 'POST /api/v1/billing/usage'])

app.use('*', async (c, next) => {
  c.set('requestId', crypto.randomUUID())
  c.set('db', createDb(c.env.DB))
  c.set('auth', null)
  await next()
})

app.use('*', async (c, next) => {
  const route = `${c.req.method.toUpperCase()} ${c.req.path.replace(/\/+$/, '')}`
  if (INTERNAL_ROUTES.has(route)) {
    if (!isValidInternalToken(c.req.header(INTERNAL_TOKEN_HEADER), c.env.INTERNAL_API_TOKEN)) {
      return failRaw(c, 403, 'Forbidden')
    }
    return next()
  }
  try {
    c.set('auth', await verifyAccessToken(c.req.header('Authorization'), c.env.JWT_SECRET))
  } catch (e) {
    return failRaw(c, 401, e instanceof AuthError ? e.message : 'Not authenticated')
  }
  return next()
})

app.onError((err, c) => {
  console.error(err)
  return fail(c, ErrorCode.INTERNAL_ERROR, 'Internal server error')
})

app.route('/api/v1/billing', billingRoutes)

export default app
