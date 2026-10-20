import { Hono } from 'hono'
import { createDb, type Db } from './db/client'
import { requestRoutes } from './routes/requests'
import { fail } from './lib/response'

export interface Bindings {
  DB: D1Database
  JWT_SECRET: string
  ADMIN_USERNAMES: string
}

type Env = { Bindings: Bindings; Variables: { db: Db; requestId: string } }

const app = new Hono<Env>()

app.use('*', async (c, next) => {
  c.set('requestId', crypto.randomUUID())
  c.set('db', createDb(c.env.DB))
  await next()
})

app.onError((err, c) => {
  console.error(err)
  return fail(c, 500, err.message)
})

app.route('/api/v1/subscription-requests', requestRoutes)

export default app
