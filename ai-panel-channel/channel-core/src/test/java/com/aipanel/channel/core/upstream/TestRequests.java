package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;

/**
 * Builds requests from raw OpenAI JSON, the way the controller receives them.
 */
final class TestRequests {

    private TestRequests() {
    }

    static ChatCompletionRequest parse(String json) {
        try {
            return UpstreamHttp.MAPPER.readValue(json, ChatCompletionRequest.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A request with a full tool-calling conversation, an image part and extra fields. */
    static final String TOOL_CONVERSATION = """
        {
          "model": "Qwen/Qwen3-32B",
          "messages": [
            {"role": "system", "content": "You are a weather bot."},
            {"role": "user", "content": [
              {"type": "text", "text": "Weather in Paris and Tokyo? See photo."},
              {"type": "image_url", "image_url": {"url": "data:image/png;base64,iVBORw0KGgo="}}
            ]},
            {"role": "assistant", "content": null, "tool_calls": [
              {"id": "call_1", "type": "function", "function": {"name": "get_weather", "arguments": "{\\"city\\":\\"Paris\\"}"}},
              {"id": "call_2", "type": "function", "function": {"name": "get_weather", "arguments": "{\\"city\\":\\"Tokyo\\"}"}}
            ]},
            {"role": "tool", "tool_call_id": "call_1", "content": "18C sunny"},
            {"role": "tool", "tool_call_id": "call_2", "content": "22C rain"}
          ],
          "tools": [
            {"type": "function", "function": {
              "name": "get_weather",
              "description": "Get the weather for a city",
              "parameters": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]},
              "strict": true
            }}
          ],
          "tool_choice": "required",
          "parallel_tool_calls": false,
          "top_k": 20,
          "chat_template_kwargs": {"enable_thinking": false}
        }
        """;
}
