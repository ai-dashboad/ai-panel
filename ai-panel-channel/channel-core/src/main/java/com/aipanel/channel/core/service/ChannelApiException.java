package com.aipanel.channel.core.service;

import lombok.Getter;

/**
 * Error returned to clients of the OpenAI-compatible API, rendered as
 * {@code {"error": {"message", "type", "code"}}} with the given HTTP status.
 */
@Getter
public class ChannelApiException extends RuntimeException {

    private final int status;
    private final String type;
    private final String code;

    public ChannelApiException(int status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }
}
