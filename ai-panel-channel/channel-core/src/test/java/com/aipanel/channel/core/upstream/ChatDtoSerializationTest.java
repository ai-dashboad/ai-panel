package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatDtoSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void requestRoundTripKeepsEverythingTheClientSent() throws Exception {
        JsonNode original = mapper.readTree(TestRequests.TOOL_CONVERSATION);
        ChatCompletionRequest req = mapper.readValue(TestRequests.TOOL_CONVERSATION, ChatCompletionRequest.class);
        JsonNode back = mapper.valueToTree(req);

        // explicit null content is dropped (equivalent for every OpenAI-compatible server)
        ((ObjectNode) original.at("/messages/2")).remove("content");
        assertThat(back).isEqualTo(original);
        assertThat(back.has("extra")).isFalse();
        assertThat(back.has("temperature")).isFalse();
    }

    @Test
    void responseRoundTripKeepsUnknownFieldsAndOmitsNulls() throws Exception {
        String json = """
            {"id":"x","object":"chat.completion","created":1,"model":"m",
             "choices":[{"index":0,"message":{"role":"assistant","content":"hi","annotations":[]},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2},"service_tier":"default"}
            """;
        ChatCompletionResponse resp = mapper.readValue(json, ChatCompletionResponse.class);
        JsonNode back = mapper.valueToTree(resp);
        assertThat(back.get("service_tier").asText()).isEqualTo("default");
        assertThat(back.at("/choices/0/message/annotations").isArray()).isTrue();
        assertThat(back.has("extra")).isFalse();
        assertThat(back.has("system_fingerprint")).isFalse();
        assertThat(back.at("/choices/0").has("delta")).isFalse();
    }
}
