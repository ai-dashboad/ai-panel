import { Hono } from 'hono'
import { createDb, type Db } from './db/client'
import { ApiKeyCache } from './lib/cache'
import { keyRoutes } from './routes/keys'
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
  KEY_CACHE: KVNamespace
  /** Shared with ai-panel-user (signs the console JWTs). Set via `wrangler secret put JWT_SECRET`. */
  JWT_SECRET: string
  /** Shared secret for service-to-service calls (gateway/dispatch -> key validation). */
  INTERNAL_API_TOKEN: string
}

export type Env = {
  Bindings: Bindings
  Variables: { db: Db; cache: ApiKeyCache; requestId: string; auth: AuthContext }
}

const app = new Hono<Env>()

// Endpoints used by other services to validate a client's API key. They are not
// called by end users, so they require the internal token instead of a user JWT.
const INTERNAL_PATHS = new Set(['/api/v1/keys/validate', '/api/v1/keys/internal/validate'])

app.use('*', async (c, next) => {
  c.set('requestId', crypto.randomUUID())
  c.set('db', createDb(c.env.DB))
  c.set('cache', new ApiKeyCache(c.env.KEY_CACHE))
  await next()
})

// Authentication: every route requires either the internal token or a valid user JWT.
// The user id always comes from the verified token — never from a client-supplied header.
app.use('*', async (c, next) => {
  const path = c.req.path.replace(/\/+$/, '')
  if (INTERNAL_PATHS.has(path)) {
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

// Note: /validate and /internal/validate are registered inside keyRoutes ahead of
// the /:keyId catch-all, mirroring ApiKeyController's route order.
app.route('/api/v1/keys', keyRoutes)

export default app
