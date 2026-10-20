package com.aipanel.common.domain;

import com.aipanel.common.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Unified API Response wrapper
 *
 * @param <T> Response data type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unified API Response")
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Response code", example = "200")
    private int code;

    @Schema(description = "Response message", example = "success")
    private String message;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Timestamp in milliseconds", example = "1704067200000")
    private long timestamp;

    @Schema(description = "Request ID for tracing", example = "req_abc123")
    private String requestId;

    /**
     * Create successful response with data
     */
    public static <T> R<T> ok(T data) {
        R<T> response = new R<>();
        response.setCode(ErrorCode.SUCCESS.getCode());
        response.setMessage(ErrorCode.SUCCESS.getMessage());
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * Create successful response without data
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * Create successful response with custom message
     */
    public static <T> R<T> ok(T data, String message) {
        R<T> response = ok(data);
        response.setMessage(message);
        return response;
    }

    /**
     * Create error response with message
     */
    public static <T> R<T> fail(String message) {
        R<T> response = new R<>();
        response.setCode(ErrorCode.INTERNAL_ERROR.getCode());
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * Create error response with code and message
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> response = new R<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * Create error response with ErrorCode
     */
    public static <T> R<T> fail(ErrorCode errorCode) {
        R<T> response = new R<>();
        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * Create error response with ErrorCode and custom message
     */
    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        R<T> response = new R<>();
        response.setCode(errorCode.getCode());
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * Set request ID for tracing
     */
    public R<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Check if response is successful
     */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }

    /**
     * Check if response is failed
     */
    public boolean isFailed() {
        return !isSuccess();
    }
}
