package com.aipanel.channel.web.advice;

import com.aipanel.channel.web.controller.ChannelController;
import com.aipanel.channel.web.controller.ProviderConfigController;
import com.aipanel.common.domain.R;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps validation / lookup errors of the admin APIs to 4xx instead of 500.
 */
@RestControllerAdvice(assignableTypes = {ProviderConfigController.class, ChannelController.class})
public class AdminApiErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(400, e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<R<Void>> upstream(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(R.fail(502, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .findFirst().orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(400, msg));
    }
}
