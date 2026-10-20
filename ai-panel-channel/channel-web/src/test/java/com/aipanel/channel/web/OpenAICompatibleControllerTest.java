package com.aipanel.channel.web;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.core.service.ChannelApiException;
import com.aipanel.channel.core.service.ChatCompletionService;
import com.aipanel.channel.core.upstream.StreamResult;
import com.aipanel.channel.core.upstream.StreamSink;
import com.aipanel.channel.core.upstream.UpstreamException;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.aipanel.channel.web.advice.OpenAiErrorHandler;
import com.aipanel.channel.web.controller.OpenAICompatibleController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenAICompatibleControllerTest {

    private static final String BODY = """
        {"model":"Qwen/Qwen3-32B","messages":[{"role":"user","content":"weather in Paris?"}],
         "tools":[{"type":"function","function":{"name":"get_weather","parameters":{"type":"object"}}}],
         "stream": %s}
        """;

    private ChatCompletionService service;
    private MockMvc mvc;
    private final ChannelApiKeyPO key = new ChannelApiKeyPO();

    @BeforeEach
    void setUp() {
        service = mock(ChatCompletionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new OpenAICompatibleController(service, new ObjectMapper()))
            .setControllerAdvice(new OpenAiErrorHandler())
            .build();
        when(service.authorize(anyString(), any())).thenReturn(key);
    }

    @Test
    void nonStreamingReturnsJsonWithToolCalls() throws Exception {
        ChatCompletionResponse resp = ChatCompletionResponse.builder()
            .id("r").object("chat.completion").model("Qwen/Qwen3-32B")
            .choices(List.of(ChatCompletionResponse.Choice.builder().index(0).finishReason("tool_calls")
                .message(ChatCompletionResponse.Message.builder().role("assistant")
                    .toolCalls(List.of(ChatCompletionResponse.ToolCall.builder().id("call_1").type("function")
                        .function(ChatCompletionResponse.FunctionCall.builder().name("get_weather").arguments("{}").build())
                        .build()))
                    .build())
                .build()))
            .build();
        when(service.chatCompletion(eq(key), any(ChatCompletionRequest.class))).thenReturn(resp);

        mvc.perform(post("/v1/chat/completions").header("Authorization", "Bearer sk-x")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(BODY.formatted("false")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].finish_reason").value("tool_calls"))
            .andExpect(jsonPath("$.choices[0].message.tool_calls[0].function.name").value("get_weather"))
            .andExpect(jsonPath("$.choices[0].message.content").doesNotExist());
    }

    @Test
    void streamingWritesSseEvenWhenClientAcceptsJson() throws Exception {
        doAnswer(inv -> {
            StreamSink sink = inv.getArgument(2);
            sink.send("{\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\"}]}}]}");
            sink.send("{\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"tool_calls\"}]}");
            return new StreamResult();
        }).when(service).streamChatCompletion(eq(key), any(ChatCompletionRequest.class), any());

        MvcResult result = mvc.perform(post("/v1/chat/completions").header("Authorization", "Bearer sk-x")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(BODY.formatted("true")))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getContentType()).startsWith("text/event-stream");
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).isEqualTo(
            "data: {\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\"}]}}]}\n\n"
                + "data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"tool_calls\"}]}\n\n"
                + "data: [DONE]\n\n");
    }

    @Test
    void streamingErrorBeforeFirstChunkIsAJsonError() throws Exception {
        doAnswer(inv -> {
            throw new UpstreamException(502, "upstream_unreachable", "Cannot reach upstream http://gpu-host:8000/v1", true);
        }).when(service).streamChatCompletion(eq(key), any(ChatCompletionRequest.class), any());

        mvc.perform(post("/v1/chat/completions").header("Authorization", "Bearer sk-x")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted("true")))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error.code").value("upstream_unreachable"))
            .andExpect(jsonPath("$.error.type").value("api_error"));
    }

    @Test
    void streamingErrorAfterFirstChunkIsAnErrorEvent() throws Exception {
        doAnswer(inv -> {
            StreamSink sink = inv.getArgument(2);
            sink.send("{\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hel\"}}]}");
            throw new UpstreamException(502, "upstream_stream_error", "connection reset", false);
        }).when(service).streamChatCompletion(eq(key), any(ChatCompletionRequest.class), any());

        String body = mvc.perform(post("/v1/chat/completions").header("Authorization", "Bearer sk-x")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted("true")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"code\":\"upstream_stream_error\"").endsWith("data: [DONE]\n\n");
    }

    @Test
    void authAndRoutingErrorsUseOpenAiErrorFormat() throws Exception {
        when(service.authorize(anyString(), any())).thenThrow(
            new ChannelApiException(404, "invalid_request_error", "model_not_found", "The model 'x' is not configured"));
        mvc.perform(post("/v1/chat/completions").header("Authorization", "Bearer sk-x")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted("true")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("model_not_found"));

        mvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted("false")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("invalid_api_key"));
    }
}
