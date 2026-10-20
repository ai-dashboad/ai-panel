package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicAdapterTest {

    private MockUpstream upstream;
    private final AnthropicAdapter adapter = new AnthropicAdapter();

    @BeforeEach
    void setUp() throws Exception {
        upstream = new MockUpstream();
    }

    @AfterEach
    void tearDown() {
        upstream.close();
    }

    private UpstreamTarget target(String baseUrl) {
        return new UpstreamTarget(new UpstreamProvider(7L, "Anthropic", "anthropic", baseUrl, 10, 1, null), "sk-ant-test", 3L);
    }

    @Test
    void mapsToolConversationToAnthropic() {
        ChatCompletionRequest req = TestRequests.parse(TestRequests.TOOL_CONVERSATION);
        req.setModel("claude-sonnet-4-5");
        JsonNode body = adapter.buildBody(req, false);

        assertThat(body.get("system").asText()).isEqualTo("You are a weather bot.");
        assertThat(body.get("max_tokens").asInt()).isEqualTo(AnthropicAdapter.DEFAULT_MAX_TOKENS);
        assertThat(body.has("temperature")).isFalse();
        assertThat(body.has("top_p")).isFalse();
        assertThat(body.get("top_k").asInt()).isEqualTo(20);

        // tools -> input_schema
        assertThat(body.at("/tools/0/name").asText()).isEqualTo("get_weather");
        assertThat(body.at("/tools/0/input_schema/properties/city/type").asText()).isEqualTo("string");
        // tool_choice required -> any, parallel_tool_calls=false -> disable_parallel_tool_use
        assertThat(body.at("/tool_choice/type").asText()).isEqualTo("any");
        assertThat(body.at("/tool_choice/disable_parallel_tool_use").asBoolean()).isTrue();

        JsonNode msgs = body.get("messages");
        assertThat(msgs).hasSize(3);
        // user: text + base64 image
        assertThat(msgs.at("/0/role").asText()).isEqualTo("user");
        assertThat(msgs.at("/0/content/0/type").asText()).isEqualTo("text");
        assertThat(msgs.at("/0/content/1/type").asText()).isEqualTo("image");
        assertThat(msgs.at("/0/content/1/source/type").asText()).isEqualTo("base64");
        assertThat(msgs.at("/0/content/1/source/media_type").asText()).isEqualTo("image/png");
        assertThat(msgs.at("/0/content/1/source/data").asText()).isEqualTo("iVBORw0KGgo=");
        // assistant tool_calls -> tool_use with parsed input
        assertThat(msgs.at("/1/role").asText()).isEqualTo("assistant");
        assertThat(msgs.at("/1/content/0/type").asText()).isEqualTo("tool_use");
        assertThat(msgs.at("/1/content/0/id").asText()).isEqualTo("call_1");
        assertThat(msgs.at("/1/content/0/input/city").asText()).isEqualTo("Paris");
        assertThat(msgs.at("/1/content/1/input/city").asText()).isEqualTo("Tokyo");
        // both tool results merged into one user message
        assertThat(msgs.at("/2/role").asText()).isEqualTo("user");
        assertThat(msgs.at("/2/content/0/type").asText()).isEqualTo("tool_result");
        assertThat(msgs.at("/2/content/0/tool_use_id").asText()).isEqualTo("call_1");
        assertThat(msgs.at("/2/content/1/tool_use_id").asText()).isEqualTo("call_2");
        assertThat(msgs.at("/2/content/1/content").asText()).isEqualTo("22C rain");
    }

    @Test
    void mapsToolChoiceVariantsAndHonorsClientParams() {
        assertThat(AnthropicAdapter.mapToolChoice("auto").get("type").asText()).isEqualTo("auto");
        assertThat(AnthropicAdapter.mapToolChoice("none").get("type").asText()).isEqualTo("none");
        JsonNode named = AnthropicAdapter.mapToolChoice(Map.of("type", "function", "function", Map.of("name", "lookup")));
        assertThat(named.get("type").asText()).isEqualTo("tool");
        assertThat(named.get("name").asText()).isEqualTo("lookup");

        ChatCompletionRequest req = TestRequests.parse("""
            {"model":"claude-x","messages":[{"role":"user","content":"hi"}],
             "max_tokens":100,"temperature":0.3,"stop":["X","Y"]}
            """);
        JsonNode body = adapter.buildBody(req, true);
        assertThat(body.get("max_tokens").asInt()).isEqualTo(100);
        assertThat(body.get("temperature").asDouble()).isEqualTo(0.3);
        assertThat(body.get("stop_sequences")).hasSize(2);
        assertThat(body.get("stream").asBoolean()).isTrue();
        assertThat(body.has("tools")).isFalse();
        assertThat(body.has("tool_choice")).isFalse();
    }

    @Test
    void nonStreamingToolUseResponseAndCustomBaseUrl() {
        upstream.json("/proxy/v1/messages", 200, """
            {"id":"msg_1","type":"message","role":"assistant","model":"claude-sonnet-4-5",
             "content":[{"type":"text","text":"Let me check."},
                        {"type":"tool_use","id":"toolu_1","name":"get_weather","input":{"city":"Paris"}}],
             "stop_reason":"tool_use","usage":{"input_tokens":40,"output_tokens":15}}
            """);
        ChatCompletionRequest req = TestRequests.parse(TestRequests.TOOL_CONVERSATION);
        req.setModel("claude-sonnet-4-5");

        ChatCompletionResponse resp = adapter.complete(target(upstream.baseUrl() + "/proxy"), req);

        MockUpstream.Recorded r = upstream.last();
        assertThat(r.path()).isEqualTo("/proxy/v1/messages");
        assertThat(r.header("x-api-key")).isEqualTo("sk-ant-test");
        assertThat(r.header("anthropic-version")).isEqualTo(AnthropicAdapter.API_VERSION);
        ChatCompletionResponse.Choice c = resp.getChoices().get(0);
        assertThat(c.getFinishReason()).isEqualTo("tool_calls");
        assertThat(c.getMessage().getContent()).isEqualTo("Let me check.");
        assertThat(c.getMessage().getToolCalls().get(0).getId()).isEqualTo("toolu_1");
        assertThat(c.getMessage().getToolCalls().get(0).getFunction().getArguments()).isEqualTo("{\"city\":\"Paris\"}");
        assertThat(resp.getUsage().getTotalTokens()).isEqualTo(55);
    }

    @Test
    void mapsStopReasons() {
        assertThat(AnthropicAdapter.mapStopReason("end_turn")).isEqualTo("stop");
        assertThat(AnthropicAdapter.mapStopReason("max_tokens")).isEqualTo("length");
        assertThat(AnthropicAdapter.mapStopReason("tool_use")).isEqualTo("tool_calls");
        assertThat(AnthropicAdapter.mapStopReason("stop_sequence")).isEqualTo("stop");
    }

    @Test
    void streamsToolUseAsOpenAiToolCallDeltas() {
        upstream.sse("/v1/messages", List.of(
            "event: message_start\ndata: {\"type\":\"message_start\",\"message\":{\"id\":\"msg_s\",\"model\":\"claude-sonnet-4-5\",\"usage\":{\"input_tokens\":25,\"output_tokens\":1}}}\n\n",
            "event: content_block_start\ndata: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}\n\n",
            "event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Checking\"}}\n\n",
            "event: content_block_stop\ndata: {\"type\":\"content_block_stop\",\"index\":0}\n\n",
            "event: ping\ndata: {\"type\":\"ping\"}\n\n",
            "event: content_block_start\ndata: {\"type\":\"content_block_start\",\"index\":1,\"content_block\":{\"type\":\"tool_use\",\"id\":\"toolu_9\",\"name\":\"get_weather\",\"input\":{}}}\n\n",
            "event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"index\":1,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"city\\\": \"}}\n\n",
            "event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"index\":1,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"\\\"Paris\\\"}\"}}\n\n",
            "event: content_block_stop\ndata: {\"type\":\"content_block_stop\",\"index\":1}\n\n",
            "event: message_delta\ndata: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"tool_use\"},\"usage\":{\"output_tokens\":20}}\n\n",
            "event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n"));
        ChatCompletionRequest req = TestRequests.parse(TestRequests.TOOL_CONVERSATION);
        req.setModel("claude-sonnet-4-5");
        req.setStream(true);
        req.setStreamOptions(Map.of("include_usage", true));

        List<String> chunks = new ArrayList<>();
        StreamResult result = adapter.stream(target(upstream.baseUrl()), req, chunks::add);

        assertThat(upstream.last().json().get("stream").asBoolean()).isTrue();
        List<JsonNode> nodes = chunks.stream().map(c -> {
            try {
                return UpstreamHttp.MAPPER.readTree(c);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
        // role, text, tool start, 2 arg deltas, finish, usage
        assertThat(nodes).hasSize(7);
        assertThat(nodes.get(0).at("/choices/0/delta/role").asText()).isEqualTo("assistant");
        assertThat(nodes.get(1).at("/choices/0/delta/content").asText()).isEqualTo("Checking");
        JsonNode start = nodes.get(2).at("/choices/0/delta/tool_calls/0");
        assertThat(start.get("index").asInt()).isEqualTo(0);
        assertThat(start.get("id").asText()).isEqualTo("toolu_9");
        assertThat(start.at("/function/name").asText()).isEqualTo("get_weather");
        assertThat(nodes.get(3).at("/choices/0/delta/tool_calls/0/function/arguments").asText()).isEqualTo("{\"city\": ");
        assertThat(nodes.get(4).at("/choices/0/delta/tool_calls/0/index").asInt()).isEqualTo(0);
        assertThat(nodes.get(5).at("/choices/0/finish_reason").asText()).isEqualTo("tool_calls");
        assertThat(nodes.get(6).at("/usage/total_tokens").asInt()).isEqualTo(45);
        assertThat(nodes.get(6).get("choices")).isEmpty();
        assertThat(nodes).allMatch(n -> "chat.completion.chunk".equals(n.get("object").asText()));
        assertThat(result.getFinishReason()).isEqualTo("tool_calls");
        assertThat(result.getUsage().getPromptTokens()).isEqualTo(25);
    }
}
