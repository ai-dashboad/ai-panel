package com.aipanel.common.exception;

import com.aipanel.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Exception related to API Key operations
 */
@Getter
public class ApiKeyException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final String keyId;

    public ApiKeyException(ErrorCode errorCode) {
        super(errorCode);
        this.keyId = null;
    }

    public ApiKeyException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.keyId = null;
    }

    public ApiKeyException(ErrorCode errorCode, String message, String keyId) {
        super(errorCode, message);
        this.keyId = keyId;
    }

    /**
     * Invalid API Key exception
     */
    public static ApiKeyException invalid() {
        return new ApiKeyException(ErrorCode.API_KEY_INVALID);
    }

    /**
     * Invalid API Key exception with key ID
     */
    public static ApiKeyException invalid(String keyId) {
        return new ApiKeyException(ErrorCode.API_KEY_INVALID,
            "Invalid API Key: " + maskKeyId(keyId), keyId);
    }

    /**
     * Expired API Key exception
     */
    public static ApiKeyException expired(String keyId) {
        return new ApiKeyException(ErrorCode.API_KEY_EXPIRED,
            "API Key has expired: " + maskKeyId(keyId), keyId);
    }

    /**
     * Disabled API Key exception
     */
    public static ApiKeyException disabled(String keyId) {
        return new ApiKeyException(ErrorCode.API_KEY_DISABLED,
            "API Key is disabled: " + maskKeyId(keyId), keyId);
    }

    /**
     * Quota exceeded exception
     */
    public static ApiKeyException quotaExceeded(String keyId) {
        return new ApiKeyException(ErrorCode.API_KEY_QUOTA_EXCEEDED,
            "API Key quota exceeded: " + maskKeyId(keyId), keyId);
    }

    /**
     * Not found exception
     */
    public static ApiKeyException notFound(String keyId) {
        return new ApiKeyException(ErrorCode.API_KEY_NOT_FOUND,
            "API Key not found: " + maskKeyId(keyId), keyId);
    }

    /**
     * Mask API Key ID for logging
     */
    private static String maskKeyId(String keyId) {
        if (keyId == null || keyId.length() < 8) {
            return "***";
        }
        return keyId.substring(0, 7) + "..." + keyId.substring(keyId.length() - 4);
    }
}
