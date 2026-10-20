package com.aipanel.common.exception;

import com.aipanel.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Exception related to AI model operations
 */
@Getter
public class ModelException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final String modelId;
    private final String provider;

    public ModelException(ErrorCode errorCode) {
        super(errorCode);
        this.modelId = null;
        this.provider = null;
    }

    public ModelException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.modelId = null;
        this.provider = null;
    }

    public ModelException(ErrorCode errorCode, String modelId, String provider) {
        super(errorCode);
        this.modelId = modelId;
        this.provider = provider;
    }

    public ModelException(ErrorCode errorCode, String message, String modelId, String provider) {
        super(errorCode, message);
        this.modelId = modelId;
        this.provider = provider;
    }

    public ModelException(ErrorCode errorCode, String message, String modelId, String provider, Throwable cause) {
        super(errorCode.getCode(), message, cause);
        this.modelId = modelId;
        this.provider = provider;
    }

    /**
     * Model not found
     */
    public static ModelException notFound(String modelId) {
        return new ModelException(ErrorCode.MODEL_NOT_FOUND,
            "Model not found: " + modelId, modelId, null);
    }

    /**
     * Model unavailable
     */
    public static ModelException unavailable(String modelId, String provider) {
        return new ModelException(ErrorCode.MODEL_UNAVAILABLE,
            String.format("Model %s is unavailable on %s", modelId, provider), modelId, provider);
    }

    /**
     * Model not allowed for user
     */
    public static ModelException notAllowed(String modelId) {
        return new ModelException(ErrorCode.MODEL_NOT_ALLOWED,
            "Access to model " + modelId + " is not allowed", modelId, null);
    }

    /**
     * All models unavailable
     */
    public static ModelException allUnavailable() {
        return new ModelException(ErrorCode.ALL_MODELS_UNAVAILABLE);
    }

    /**
     * Provider error
     */
    public static ModelException providerError(String modelId, String provider, String error) {
        return new ModelException(ErrorCode.MODEL_PROVIDER_ERROR,
            String.format("Provider %s error for model %s: %s", provider, modelId, error),
            modelId, provider);
    }

    /**
     * Provider error with cause
     */
    public static ModelException providerError(String modelId, String provider, Throwable cause) {
        return new ModelException(ErrorCode.MODEL_PROVIDER_ERROR,
            String.format("Provider %s error for model %s: %s", provider, modelId, cause.getMessage()),
            modelId, provider, cause);
    }

    /**
     * Timeout error
     */
    public static ModelException timeout(String modelId, String provider) {
        return new ModelException(ErrorCode.MODEL_TIMEOUT,
            String.format("Request to model %s on %s timed out", modelId, provider),
            modelId, provider);
    }

    /**
     * Context length exceeded
     */
    public static ModelException contextLengthExceeded(String modelId, int maxLength, int actualLength) {
        return new ModelException(ErrorCode.MODEL_CONTEXT_LENGTH_EXCEEDED,
            String.format("Context length %d exceeds model %s limit of %d", actualLength, modelId, maxLength),
            modelId, null);
    }
}
