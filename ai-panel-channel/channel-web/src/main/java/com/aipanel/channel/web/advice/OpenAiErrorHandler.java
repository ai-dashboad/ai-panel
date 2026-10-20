package com.aipanel.channel.web.advice;

import com.aipanel.channel.core.service.ChannelApiException;
import com.aipanel.channel.core.upstream.UpstreamException;
import com.aipanel.channel.web.controller.OpenAICompatibleController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders errors of the OpenAI-compatible API in OpenAI's format:
 * {@code {"error": {"message": ..., "type": ..., "code": ..., "param": null}}}.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = OpenAICompatibleController.class)
public class OpenAiErrorHandler {

    @ExceptionHandler(ChannelApiException.class)
    public ResponseEntity<Map<String, Object>> handle(ChannelApiException e) {
        return error(e.getStatus(), e.getType(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<Map<String, Object>> handle(UpstreamException e) {
        String type = e.getStatus() == 404 ? "invalid_request_error"
            : e.getStatus() >= 500 ? "api_error"
            : e.getStatus() == 429 ? "rate_limit_error" : "invalid_request_error";
        return error(e.getStatus(), type, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handle(MissingRequestHeaderException e) {
        return error(401, "invalid_request_error", "invalid_api_key", "Missing Authorization header");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handle(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .findFirst().orElse("Invalid request");
        return error(400, "invalid_request_error", "invalid_request", msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handle(HttpMessageNotReadableException e) {
        return error(400, "invalid_request_error", "invalid_json", "Request body is not valid JSON for this endpoint");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception e) {
        log.error("Unhandled error in OpenAI-compatible API", e);
        return error(500, "api_error", "internal_error", "Internal server error");
    }

    public static Map<String, Object> body(String type, String code, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("message", message);
        err.put("type", type);
        err.put("code", code);
        err.put("param", null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", err);
        return body;
    }

    private static ResponseEntity<Map<String, Object>> error(int status, String type, String code, String message) {
        HttpStatus s = HttpStatus.resolve(status);
        return ResponseEntity.status(s != null ? s : HttpStatus.BAD_GATEWAY)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body(type, code, message));
    }
}
