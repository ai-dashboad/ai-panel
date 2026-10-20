package com.aipanel.channel.core.upstream;

import java.io.IOException;

/**
 * Receives OpenAI-format {@code chat.completion.chunk} JSON payloads (without the {@code data: } prefix).
 */
@FunctionalInterface
public interface StreamSink {
    void send(String chunkJson) throws IOException;
}
