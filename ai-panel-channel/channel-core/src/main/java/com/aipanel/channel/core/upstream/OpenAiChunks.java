package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers to build OpenAI {@code chat.completion.chunk} payloads.
 */
public final class OpenAiChunks {

    private OpenAiChunks() {
    }

    public static ObjectNode chunk(String id, String model, long created, ObjectNode delta, String finishReason) {
        ObjectNode chunk = UpstreamHttp.MAPPER.createObjectNode();
        chunk.put("id", id);
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", created);
        chunk.put("model", model);
        ArrayNode choices = chunk.putArray("choices");
        ObjectNode choice = choices.addObject();
        choice.put("index", 0);
        choice.set("delta", delta == null ? UpstreamHttp.MAPPER.createObjectNode() : delta);
        if (finishReason == null) {
            choice.putNull("finish_reason");
        } else {
            choice.put("finish_reason", finishReason);
        }
        return chunk;
    }

    public static ObjectNode usageChunk(String id, String model, long created, ChatCompletionResponse.Usage usage) {
        ObjectNode chunk = UpstreamHttp.MAPPER.createObjectNode();
        chunk.put("id", id);
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", created);
        chunk.put("model", model);
        chunk.putArray("choices");
        chunk.set("usage", UpstreamHttp.MAPPER.valueToTree(usage));
        return chunk;
    }

    /**
     * Turn a complete (non-streaming) response into equivalent stream chunks. Used when an
     * upstream ignores {@code stream: true} and answers with plain JSON.
     */
    public static List<String> fromResponse(ChatCompletionResponse response, boolean includeUsage) {
        List<String> out = new ArrayList<>();
        String id = response.getId() == null ? "chatcmpl-aipanel" : response.getId();
        long created = response.getCreated() == null ? System.currentTimeMillis() / 1000 : response.getCreated();
        String finish = "stop";
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatCompletionResponse.Choice c = response.getChoices().get(0);
            finish = c.getFinishReason() == null ? "stop" : c.getFinishReason();
            ChatCompletionResponse.Message m = c.getMessage();
            ObjectNode delta = UpstreamHttp.MAPPER.createObjectNode();
            delta.put("role", "assistant");
            if (m != null) {
                if (m.getContent() != null) {
                    delta.put("content", m.getContent());
                }
                if (m.getReasoningContent() != null) {
                    delta.put("reasoning_content", m.getReasoningContent());
                }
                if (m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                    ArrayNode tcs = delta.putArray("tool_calls");
                    for (int i = 0; i < m.getToolCalls().size(); i++) {
                        ChatCompletionResponse.ToolCall tc = m.getToolCalls().get(i);
                        ObjectNode t = UpstreamHttp.MAPPER.valueToTree(tc);
                        t.put("index", i);
                        tcs.add(t);
                    }
                }
            }
            out.add(chunk(id, response.getModel(), created, delta, null).toString());
        }
        out.add(chunk(id, response.getModel(), created, null, finish).toString());
        if (includeUsage && response.getUsage() != null) {
            out.add(usageChunk(id, response.getModel(), created, response.getUsage()).toString());
        }
        return out;
    }
}
