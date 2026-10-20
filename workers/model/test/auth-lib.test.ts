import { describe, it, expect } from 'vitest'
import { SignJWT } from 'jose'
import { verifyAccessToken, isValidInternalToken, isAdmin, AuthError } from '../src/lib/auth'

const SECRET = 'unit-test-secret-that-is-at-least-32-bytes-long!!'

async function sign(claims: Record<string, unknown>, opts: { secret?: string; alg?: string; sub?: string; exp?: string | number } = {}) {
  const jwt = new SignJWT(claims)
    .setProtectedHeader({ alg: opts.alg ?? 'HS256' })
    .setIssuedAt()
    .setExpirationTime(opts.exp ?? '1h')
  if (opts.sub !== undefined) jwt.setSubject(opts.sub)
  return jwt.sign(new TextEncoder().encode(opts.secret ?? SECRET))
}

describe('verifyAccessToken', () => {
  it('accepts an access token shaped like ai-panel-user issues (HS256/384/512)', async () => {
    for (const alg of ['HS256', 'HS384', 'HS512']) {
      const token = await sign({ username: 'alice', type: 'access' }, { sub: '42', alg })
      await expect(verifyAccessToken(`Bearer ${token}`, SECRET)).resolves.toEqual({ userId: 42, username: 'alice' })
    }
  })

  it('rejects a missing/malformed header', async () => {
    await expect(verifyAccessToken(undefined, SECRET)).rejects.toBeInstanceOf(AuthError)
    await expect(verifyAccessToken('Basic abc', SECRET)).rejects.toBeInstanceOf(AuthError)
  })

  it('rejects a token signed with another secret', async () => {
    const token = await sign({ type: 'access' }, { sub: '1', secret: 'some-other-secret-that-is-32-bytes-long!!' })
    await expect(verifyAccessToken(`Bearer ${token}`, SECRET)).rejects.toThrow('Invalid or expired token')
  })

  it('rejects refresh tokens and tokens without a numeric subject', async () => {
    const refresh = await sign({ type: 'refresh' }, { sub: '1' })
    await expect(verifyAccessToken(`Bearer ${refresh}`, SECRET)).rejects.toThrow('Not an access token')
    const noSub = await sign({ type: 'access' })
    await expect(verifyAccessToken(`Bearer ${noSub}`, SECRET)).rejects.toThrow('Token missing subject')
  })

  it('rejects expired tokens', async () => {
    const token = await sign({ type: 'access' }, { sub: '1', exp: Math.floor(Date.now() / 1000) - 60 })
    await expect(verifyAccessToken(`Bearer ${token}`, SECRET)).rejects.toThrow('Invalid or expired token')
  })

  it('rejects unsigned (alg=none) tokens', async () => {
    const b64 = (o: object) => Buffer.from(JSON.stringify(o)).toString('base64url')
    const token = `${b64({ alg: 'none' })}.${b64({ sub: '1', type: 'access' })}.`
    await expect(verifyAccessToken(`Bearer ${token}`, SECRET)).rejects.toBeInstanceOf(AuthError)
  })

  it('fails closed when JWT_SECRET is not configured', async () => {
    const token = await sign({ type: 'access' }, { sub: '1' })
    await expect(verifyAccessToken(`Bearer ${token}`, undefined)).rejects.toThrow(/JWT_SECRET/)
    await expect(verifyAccessToken(`Bearer ${token}`, '')).rejects.toThrow(/JWT_SECRET/)
  })
})

describe('isValidInternalToken', () => {
  it('matches only the exact configured token and fails closed', () => {
    expect(isValidInternalToken('s3cret', 's3cret')).toBe(true)
    expect(isValidInternalToken('s3cre', 's3cret')).toBe(false)
    expect(isValidInternalToken('s3cretX', 's3cret')).toBe(false)
    expect(isValidInternalToken(undefined, 's3cret')).toBe(false)
    expect(isValidInternalToken('anything', undefined)).toBe(false)
    expect(isValidInternalToken('', '')).toBe(false)
  })
})

describe('isAdmin', () => {
  it('is case-insensitive and tolerates whitespace', () => {
    expect(isAdmin('Admin', ' admin , ops ')).toBe(true)
    expect(isAdmin('bob', 'admin,ops')).toBe(false)
    expect(isAdmin(null, 'admin')).toBe(false)
    expect(isAdmin('admin', undefined)).toBe(false)
  })
})
