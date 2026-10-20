import { jwtVerify } from 'jose'

// Mirrors ai-panel-user's JwtServiceImpl / ai-panel-gateway's JwtAuthFilter:
// - HMAC-signed JWT, secret shared across services via JWT_SECRET
// - subject = userId, claim "username", claim "type" must be "access"
// jjwt's Keys.hmacShaKeyFor picks HS256/384/512 from the secret length, so all three
// HMAC variants are accepted (and nothing else — "none"/RSA tokens are rejected).
const HMAC_ALGS = ['HS256', 'HS384', 'HS512']

export interface AuthContext {
  userId: number
  username: string | null
}

export class AuthError extends Error {}

export async function verifyAccessToken(authHeader: string | undefined, jwtSecret: string | undefined): Promise<AuthContext> {
  if (!jwtSecret) {
    // Fail closed: never accept tokens when the worker has no secret configured.
    throw new AuthError('Server misconfigured: JWT_SECRET is not set')
  }
  if (!authHeader?.startsWith('Bearer ')) {
    throw new AuthError('Missing bearer token')
  }
  const token = authHeader.slice('Bearer '.length)

  let payload
  try {
    const key = new TextEncoder().encode(jwtSecret)
    ;({ payload } = await jwtVerify(token, key, { algorithms: HMAC_ALGS }))
  } catch {
    throw new AuthError('Invalid or expired token')
  }

  if (payload.type !== 'access') {
    throw new AuthError('Not an access token')
  }
  const userId = Number(payload.sub)
  if (!payload.sub || !Number.isSafeInteger(userId)) {
    throw new AuthError('Token missing subject')
  }

  return { userId, username: typeof payload.username === 'string' ? payload.username : null }
}

export function isAdmin(username: string | null, adminUsernamesCsv: string | undefined): boolean {
  if (!username || !adminUsernamesCsv) return false
  const admins = adminUsernamesCsv
    .split(',')
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean)
  return admins.includes(username.toLowerCase())
}

export const INTERNAL_TOKEN_HEADER = 'X-Internal-Token'

/**
 * Service-to-service check for internal endpoints (key validation, usage recording,
 * quota checks). Compares the X-Internal-Token header with the INTERNAL_API_TOKEN
 * secret in constant time. Fails closed when the secret is not configured.
 */
export function isValidInternalToken(provided: string | undefined, expected: string | undefined): boolean {
  if (!expected || !provided) return false
  const a = new TextEncoder().encode(provided)
  const b = new TextEncoder().encode(expected)
  let diff = a.length ^ b.length
  const len = Math.max(a.length, b.length)
  for (let i = 0; i < len; i++) {
    diff |= (a[i] ?? 0) ^ (b[i] ?? 0)
  }
  return diff === 0
}
