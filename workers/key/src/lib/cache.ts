// Replaces ai-panel-key/key-data/.../redis/ApiKeyCacheService.java
//
// The original invalidate(keyId) was a documented no-op ("In production, you'd also
// maintain a keyId -> hash mapping" — comment in the Java source): the cache was keyed
// only by hash, so updating/deleting a key by keyId couldn't evict its cached validation
// until the 5-minute TTL expired. Since this is a fresh rewrite, we maintain that mapping
// so invalidation actually works.
const VALIDATION_TTL_SECONDS = 300 // matches CommonConstants.CACHE_TTL_API_KEY
const VALIDATION_PREFIX = 'api-key:validation:'
const KEY_ID_INDEX_PREFIX = 'api-key:keyid-to-hash:'

export interface CachedValidation {
  valid: boolean
  userId: number
  keyId: string
  planType: string
  allowedModels: string[] | null
  rateLimitPerMinute: number | null
}

export class ApiKeyCache {
  constructor(private readonly kv: KVNamespace) {}

  async getValidation(keyHash: string): Promise<CachedValidation | null> {
    const json = await this.kv.get(VALIDATION_PREFIX + keyHash)
    return json ? (JSON.parse(json) as CachedValidation) : null
  }

  async cacheValidation(keyHash: string, value: CachedValidation): Promise<void> {
    await Promise.all([
      this.kv.put(VALIDATION_PREFIX + keyHash, JSON.stringify(value), {
        expirationTtl: VALIDATION_TTL_SECONDS,
      }),
      this.kv.put(KEY_ID_INDEX_PREFIX + value.keyId, keyHash, {
        expirationTtl: VALIDATION_TTL_SECONDS,
      }),
    ])
  }

  async invalidateByKeyId(keyId: string): Promise<void> {
    const keyHash = await this.kv.get(KEY_ID_INDEX_PREFIX + keyId)
    if (!keyHash) return
    await Promise.all([this.kv.delete(VALIDATION_PREFIX + keyHash), this.kv.delete(KEY_ID_INDEX_PREFIX + keyId)])
  }
}
