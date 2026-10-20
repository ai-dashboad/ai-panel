package com.aipanel.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Unified error codes for the platform
 */
@Getter
@Schema(description = "Business Error Codes")
public enum ErrorCode {

    // General errors 1xx/2xx
    SUCCESS(200, "Success"),
    PARAM_ERROR(400, "Invalid request parameters"),
    UNAUTHORIZED(401, "Unauthorized access"),
    FORBIDDEN(403, "Access forbidden"),
    NOT_FOUND(404, "Resource not found"),
    METHOD_NOT_ALLOWED(405, "Method not allowed"),
    CONFLICT(409, "Resource conflict"),
    INTERNAL_ERROR(500, "Internal server error"),
    SERVICE_UNAVAILABLE(503, "Service temporarily unavailable"),

    // API Key related 2xxx
    API_KEY_INVALID(2001, "Invalid API Key"),
    API_KEY_EXPIRED(2002, "API Key has expired"),
    API_KEY_DISABLED(2003, "API Key is disabled"),
    API_KEY_QUOTA_EXCEEDED(2004, "API Key quota exceeded"),
    API_KEY_NOT_FOUND(2005, "API Key not found"),
    API_KEY_LIMIT_REACHED(2006, "Maximum API Keys limit reached"),

    // Rate limiting related 3xxx
    RATE_LIMIT_EXCEEDED(3001, "Rate limit exceeded"),
    GLOBAL_RATE_LIMIT(3002, "System is busy, please try again later"),
    USER_RATE_LIMIT(3003, "User rate limit exceeded"),
    MODEL_RATE_LIMIT(3004, "Model rate limit exceeded"),

    // Model related 4xxx
    MODEL_NOT_FOUND(4001, "Model not found"),
    MODEL_UNAVAILABLE(4002, "Model is currently unavailable"),
    MODEL_NOT_ALLOWED(4003, "Access to this model is not allowed"),
    ALL_MODELS_UNAVAILABLE(4004, "All models are currently unavailable"),
    MODEL_PROVIDER_ERROR(4005, "Model provider returned an error"),
    MODEL_TIMEOUT(4006, "Model request timeout"),
    MODEL_CONTEXT_LENGTH_EXCEEDED(4007, "Context length exceeded"),

    // User related 5xxx
    USER_NOT_FOUND(5001, "User not found"),
    USER_DISABLED(5002, "User account is disabled"),
    USER_EXISTS(5003, "User already exists"),
    INVALID_PASSWORD(5004, "Invalid password"),
    PLAN_EXPIRED(5005, "User plan has expired"),
    PLAN_NOT_FOUND(5006, "Plan not found"),
    EMAIL_EXISTS(5007, "Email already registered"),
    USERNAME_EXISTS(5008, "Username already taken"),

    // Authentication related 6xxx
    TOKEN_INVALID(6001, "Invalid token"),
    TOKEN_EXPIRED(6002, "Token has expired"),
    REFRESH_TOKEN_INVALID(6003, "Invalid refresh token"),
    LOGIN_FAILED(6004, "Login failed"),

    // Billing related 7xxx
    INSUFFICIENT_BALANCE(7001, "Insufficient balance"),
    PAYMENT_FAILED(7002, "Payment failed"),
    INVOICE_NOT_FOUND(7003, "Invoice not found");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Get ErrorCode by code value
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
