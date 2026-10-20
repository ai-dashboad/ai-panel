import { describe, it, expect } from 'vitest'
import { SignJWT } from 'jose'
import app from '../src/index'

const SECRET = 'unit-test-secret-that-is-at-least-32-bytes-long!!'
const env = { DB: {} as D1Database, JWT_SECRET: SECRET, ADMIN_USERNAMES: 'admin' }

const token = (username: string) =>
  new SignJWT({ username, type: 'access' })
    .setProtectedHeader({ alg: 'HS256' })
    .setSubject('7')
    .setExpirationTime('1h')
    .sign(new TextEncoder().encode(SECRET))

async function call(path: string, init: RequestInit = {}) {
  const res = await app.request(path, init, env)
  return (await res.json()) as { code: number }
}

describe('model worker auth', () => {
  it('requires a JWT for reads', async () => {
    expect((await call('/api/v1/models')).code).toBe(401)
    expect((await call('/api/v1/model-groups')).code).toBe(401)
    expect((await call('/api/v1/stats/models/health')).code).toBe(401)
    expect((await call('/api/v1/models/providers')).code).toBe(401)
  })

  it('allows reads for any signed-in user', async () => {
    const res = await call('/api/v1/models', { headers: { Authorization: `Bearer ${await token('bob')}` } })
    expect([401, 403]).not.toContain(res.code)
  })

  it('restricts catalog writes to ADMIN_USERNAMES', async () => {
    const init = (t: string, method: string) => ({
      method,
      headers: { Authorization: `Bearer ${t}`, 'Content-Type': 'application/json' },
      body: method === 'DELETE' ? undefined : '{}',
    })
    const bob = await token('bob')
    expect((await call('/api/v1/models', init(bob, 'POST'))).code).toBe(403)
    expect((await call('/api/v1/models/gpt-4o', init(bob, 'PUT'))).code).toBe(403)
    expect((await call('/api/v1/models/gpt-4o', init(bob, 'DELETE'))).code).toBe(403)
    expect((await call('/api/v1/models/providers/openai/sync', init(bob, 'POST'))).code).toBe(403)
    const admin = await token('admin')
    expect([401, 403]).not.toContain((await call('/api/v1/models/providers/openai/sync', init(admin, 'POST'))).code)
  })
})
