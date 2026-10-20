import type { Context } from 'hono'
import { ErrorCode, type ErrorCodeEntry } from './error-code'

// Mirrors ai-panel-common's R<T> envelope so ai-panel-ui's axios interceptor
// (which checks code === 0 || code === 200) keeps working unchanged.
export interface Envelope<T> {
  code: number
  message: string
  data: T | null
  timestamp: number
  requestId?: string
}

export function ok<T>(c: Context, data: T, message: string = ErrorCode.SUCCESS.message) {
  const body: Envelope<T> = {
    code: ErrorCode.SUCCESS.code,
    message,
    data,
    timestamp: Date.now(),
    requestId: c.get('requestId'),
  }
  return c.json(body, 200)
}

export function fail(c: Context, errorCode: ErrorCodeEntry, message?: string) {
  const body: Envelope<null> = {
    code: errorCode.code,
    message: message ?? errorCode.message,
    data: null,
    timestamp: Date.now(),
    requestId: c.get('requestId'),
  }
  // Business errors travel with HTTP 200 + a business code in the body,
  // matching the existing Spring services' convention that ai-panel-ui relies on.
  return c.json(body, 200)
}

export function failRaw(c: Context, code: number, message: string) {
  const body: Envelope<null> = {
    code,
    message,
    data: null,
    timestamp: Date.now(),
    requestId: c.get('requestId'),
  }
  return c.json(body, 200)
}
