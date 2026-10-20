// Ported from ai-panel-common/src/main/java/com/aipanel/common/enums/ErrorCode.java
// Keep this list in sync across all Worker services until the Java modules are retired.
export const ErrorCode = {
  SUCCESS: { code: 200, message: 'Success' },
  PARAM_ERROR: { code: 400, message: 'Invalid request parameters' },
  UNAUTHORIZED: { code: 401, message: 'Unauthorized access' },
  FORBIDDEN: { code: 403, message: 'Access forbidden' },
  NOT_FOUND: { code: 404, message: 'Resource not found' },
  METHOD_NOT_ALLOWED: { code: 405, message: 'Method not allowed' },
  CONFLICT: { code: 409, message: 'Resource conflict' },
  INTERNAL_ERROR: { code: 500, message: 'Internal server error' },
  SERVICE_UNAVAILABLE: { code: 503, message: 'Service temporarily unavailable' },

  MODEL_NOT_FOUND: { code: 4001, message: 'Model not found' },
  MODEL_UNAVAILABLE: { code: 4002, message: 'Model is currently unavailable' },
  MODEL_NOT_ALLOWED: { code: 4003, message: 'Access to this model is not allowed' },
  ALL_MODELS_UNAVAILABLE: { code: 4004, message: 'All models are currently unavailable' },
  MODEL_PROVIDER_ERROR: { code: 4005, message: 'Model provider returned an error' },
  MODEL_TIMEOUT: { code: 4006, message: 'Model request timeout' },
  MODEL_CONTEXT_LENGTH_EXCEEDED: { code: 4007, message: 'Context length exceeded' },
} as const

export type ErrorCodeEntry = (typeof ErrorCode)[keyof typeof ErrorCode]

export class BusinessError extends Error {
  constructor(
    public readonly errorCode: ErrorCodeEntry,
    message?: string,
  ) {
    super(message ?? errorCode.message)
  }
}
