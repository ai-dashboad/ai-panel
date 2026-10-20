// Ported from ai-panel-common/src/main/java/com/aipanel/common/util/ApiKeyGenerator.java
// Java's MessageDigest("SHA-256") becomes Workers' crypto.subtle.digest.
const KEY_PREFIX = 'sk-'
const KEY_LENGTH = 32
const DISPLAY_PREFIX_LENGTH = 7
const DISPLAY_SUFFIX_LENGTH = 4
const ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'

export function generateApiKey(): string {
  const randomBytes = new Uint8Array(KEY_LENGTH)
  crypto.getRandomValues(randomBytes)

  let key = KEY_PREFIX
  for (let i = 0; i < KEY_LENGTH; i++) {
    key += ALPHABET[randomBytes[i] % ALPHABET.length]
  }
  return key
}

export async function hashApiKey(apiKey: string): Promise<string> {
  if (!apiKey) throw new Error('API Key cannot be null or empty')

  const data = new TextEncoder().encode(apiKey)
  const digest = await crypto.subtle.digest('SHA-256', data)
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('')
}

export function getDisplayPrefix(apiKey: string): string {
  if (!apiKey || apiKey.length < 12) return '***'
  return apiKey.slice(0, DISPLAY_PREFIX_LENGTH) + '...' + apiKey.slice(-DISPLAY_SUFFIX_LENGTH)
}

export function isValidApiKeyFormat(apiKey: string | null | undefined): apiKey is string {
  if (!apiKey || !apiKey.startsWith(KEY_PREFIX)) return false
  const keyPart = apiKey.slice(KEY_PREFIX.length)
  if (keyPart.length !== KEY_LENGTH) return false
  return [...keyPart].every((ch) => ALPHABET.includes(ch))
}

export function extractKeyId(apiKey: string): string {
  const keyPart = apiKey.slice(KEY_PREFIX.length)
  return keyPart.length >= 8 ? keyPart.slice(0, 8) : keyPart
}
