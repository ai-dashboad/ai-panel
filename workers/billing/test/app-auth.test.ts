import { describe, it, expect } from 'vitest'
import { SignJWT } from 'jose'
import app from '../src/index'

const SECRET = 'unit-test-secret-that-is-at-least-32-bytes-long!!'
const INTERNAL = 'internal-test-token'
const env = {
  DB: {} as D1Database,
  DEFAULT_MONTHLY_QUOTA_TOKENS: '1000',
  JWT_SECRET: SECRET,
  INTERNAL_API_TOKEN: INTERNAL,
  ADMIN_USERNAMES: 'admin',
}

const token = (sub: string, username: string) =>
  new SignJWT({ username, type: 'access' })
    .setProtectedHeader({ alg: 'HS256' })
    .setSubject(sub)
    .setExpirationTime('1h')
    .sign(new TextEncoder().encode(SECRET))

async function call(path: string, init: RequestInit = {}) {
  const res = await app.request(path, init, env)
  return (await res.json()) as { code: number; data: unknown }
}

describe('billing worker auth', () => {
  it('protects internal endpoints with the internal token', async () => {
    expect((await call('/api/v1/billing/quota-check?userId=1')).code).toBe(403)
    expect((await call('/api/v1/billing/usage', { method: 'POST', body: '{}' })).code).toBe(403)
    const userJwt = await token('1', 'alice')
    expect((await call('/api/v1/billing/quota-check?userId=1', { headers: { Authorization: `Bearer ${userJwt}` } })).code).toBe(403)
    const ok = await call('/api/v1/billing/usage', {
      method: 'POST',
      body: '{}',
      headers: { 'X-Internal-Token': INTERNAL, 'Content-Type': 'application/json' },
    })
    expect([401, 403]).not.toContain(ok.code)
  })

  it('requires a JWT for user endpoints', async () => {
    expect((await call('/api/v1/billing/balance?userId=1')).code).toBe(401)
    expect((await call('/api/v1/billing/records?userId=1')).code).toBe(401)
  })

  it("scopes reads to the caller's own user id unless admin", async () => {
    const alice = await token('7', 'alice')
    const auth = { headers: { Authorization: `Bearer ${alice}` } }
    // /balance is a stub that never touches the DB, so it returns success directly
    expect(await call('/api/v1/billing/balance', auth)).toMatchObject({ code: 200, data: 0 })
    expect((await call('/api/v1/billing/balance?userId=7', auth)).code).toBe(200)
    expect((await call('/api/v1/billing/balance?userId=8', auth)).code).toBe(403)
    const admin = await token('1', 'admin')
    expect((await call('/api/v1/billing/balance?userId=8', { headers: { Authorization: `Bearer ${admin}` } })).code).toBe(200)
  })

  it('restricts credits to admins', async () => {
    const alice = await token('7', 'alice')
    expect((await call('/api/v1/billing/credits?userId=7&amount=5', { method: 'POST', headers: { Authorization: `Bearer ${alice}` } })).code).toBe(403)
    const admin = await token('1', 'admin')
    expect((await call('/api/v1/billing/credits?userId=7&amount=5', { method: 'POST', headers: { Authorization: `Bearer ${admin}` } })).code).toBe(200)
  })
})
