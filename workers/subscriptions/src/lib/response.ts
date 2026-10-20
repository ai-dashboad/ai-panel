import type { Context } from 'hono'

// Mirrors ai-panel-common's R<T> envelope so ai-panel-ui's axios interceptor
// (which checks code === 0 || code === 200) keeps working unchanged.
export interface Envelope<T> {
  code: number
  message: string
  data: T | null
  timestamp: number
  requestId?: string
}

export function ok<T>(c: Context, data: T, message = 'Success') {
  const body: Envelope<T> = {
    code: 200,
    message,
    data,
    timestamp: Date.now(),
    requestId: c.get('requestId'),
  }
  return c.json(body, 200)
}

export function fail(c: Context, code: number, message: string) {
  const body: Envelope<null> = {
    code,
    message,
    data: null,
    timestamp: Date.now(),
    requestId: c.get('requestId'),
  }
  return c.json(body, 200)
}
