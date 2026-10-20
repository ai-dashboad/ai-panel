// Ported from ai-panel-common/src/main/java/com/aipanel/common/enums/ErrorCode.java
export const ErrorCode = {
  SUCCESS: { code: 200, message: 'Success' },
  PARAM_ERROR: { code: 400, message: 'Invalid request parameters' },
  UNAUTHORIZED: { code: 401, message: 'Unauthorized access' },
  INTERNAL_ERROR: { code: 500, message: 'Internal server error' },

  INSUFFICIENT_BALANCE: { code: 7001, message: 'Insufficient balance' },
  PAYMENT_FAILED: { code: 7002, message: 'Payment failed' },
  INVOICE_NOT_FOUND: { code: 7003, message: 'Invoice not found' },
} as const

export type ErrorCodeEntry = (typeof ErrorCode)[keyof typeof ErrorCode]
