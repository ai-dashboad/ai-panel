// Ported from ai-panel-common/src/main/java/com/aipanel/common/enums/ErrorCode.java
// Keep this list in sync across all Worker services until the Java modules are retired.
export const ErrorCode = {
  SUCCESS: { code: 200, message: 'Success' },
  PARAM_ERROR: { code: 400, message: 'Invalid request parameters' },
  UNAUTHORIZED: { code: 401, message: 'Unauthorized access' },
  FORBIDDEN: { code: 403, message: 'Access forbidden' },
  NOT_FOUND: { code: 404, message: 'Resource not found' },
  CONFLICT: { code: 409, message: 'Resource conflict' },
  INTERNAL_ERROR: { code: 500, message: 'Internal server error' },

  API_KEY_INVALID: { code: 2001, message: 'Invalid API Key' },
  API_KEY_EXPIRED: { code: 2002, message: 'API Key has expired' },
  API_KEY_DISABLED: { code: 2003, message: 'API Key is disabled' },
  API_KEY_QUOTA_EXCEEDED: { code: 2004, message: 'API Key quota exceeded' },
  API_KEY_NOT_FOUND: { code: 2005, message: 'API Key not found' },
  API_KEY_LIMIT_REACHED: { code: 2006, message: 'Maximum API Keys limit reached' },
} as const

export type ErrorCodeEntry = (typeof ErrorCode)[keyof typeof ErrorCode]
