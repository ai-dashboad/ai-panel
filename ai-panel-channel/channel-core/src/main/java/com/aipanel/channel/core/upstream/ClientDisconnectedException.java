package com.aipanel.channel.core.upstream;

/**
 * The downstream client went away while a stream was being written. Never retried.
 */
public class ClientDisconnectedException extends RuntimeException {
    public ClientDisconnectedException(Throwable cause) {
        super("Client disconnected", cause);
    }
}
