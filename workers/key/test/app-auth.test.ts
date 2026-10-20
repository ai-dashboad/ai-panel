import { describe, it, expect } from 'vitest'
import { SignJWT } from 'jose'
import app from '../src/index'

const SECRET = 'unit-test-secret-that-is-at-least-32-bytes-long!!'
const INTERNAL = 'internal-test-token'
// DB/KV are never reached by the rejected requests below; requests that pass auth hit
// these stubs and fail with a 500-style envelope, which proves auth let them through.
const env = { DB: {} as D1Database, KEY_CACHE: {} as KVNamespace, JWT_SECRET: SECRET, INTERNAL_API_TOKEN: INTERNAL }

const token = (sub = '7') =>
  new SignJWT({ username: 'alice', type: 'access' })
    .setProtectedHeader({ alg: 'HS256' })
    .setSubject(sub)
    .setExpirationTime('1h')
    .sign(new TextEncoder().encode(SECRET))

async function call(path: string, init: RequestInit = {}) {
  const res = await app.request(path, init, env)
  return (await res.json()) as { code: number; message: string }
}

describe('key worker auth', () => {
  it('rejects user endpoints without a JWT, even with a spoofed X-User-ID header', async () => {
    expect((await call('/api/v1/keys')).code).toBe(401)
    expect((await call('/api/v1/keys', { headers: { 'X-User-ID': '1' } })).code).toBe(401)
    expect((await call('/api/v1/keys/abc', { method: 'DELETE', headers: { 'X-User-ID': '1' } })).code).toBe(401)
    expect((await call('/api/v1/keys/ensure-default', { method: 'POST' })).code).toBe(401)
  })

  it('rejects a JWT signed with the wrong secret', async () => {
    const bad = await new SignJWT({ type: 'access' })
      .setProtectedHeader({ alg: 'HS256' })
      .setSubject('1')
      .sign(new TextEncoder().encode('wrong-secret-wrong-secret-wrong-secret!!'))
    expect((await call('/api/v1/keys', { headers: { Authorization: `Bearer ${bad}` } })).code).toBe(401)
  })

  it('lets a valid JWT through to the handler', async () => {
    const res = await call('/api/v1/keys', { headers: { Authorization: `Bearer ${await token()}` } })
    expect([401, 403]).not.toContain(res.code)
  })

  it('protects the validation endpoints with the internal token', async () => {
    const body = JSON.stringify({ apiKey: 'sk-test' })
    const json = { 'Content-Type': 'application/json' }
    expect((await call('/api/v1/keys/internal/validate', { method: 'POST', body, headers: json })).code).toBe(403)
    expect((await call('/api/v1/keys/validate?key=sk-test')).code).toBe(403)
    // a user JWT is not enough for internal endpoints
    expect(
      (await call('/api/v1/keys/validate?key=sk-test', { headers: { Authorization: `Bearer ${await token()}` } })).code,
    ).toBe(403)
    const ok = await call('/api/v1/keys/validate?key=sk-test', { headers: { 'X-Internal-Token': INTERNAL } })
    expect([401, 403]).not.toContain(ok.code)
  })

  it('fails closed when secrets are not configured', async () => {
    const res = await app.request('/api/v1/keys', { headers: { Authorization: `Bearer ${await token()}` } }, { ...env, JWT_SECRET: '' })
    expect(((await res.json()) as { code: number }).code).toBe(401)
    const res2 = await app.request('/api/v1/keys/validate?key=x', { headers: { 'X-Internal-Token': '' } }, { ...env, INTERNAL_API_TOKEN: '' })
    expect(((await res2.json()) as { code: number }).code).toBe(403)
  })
})
