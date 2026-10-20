# Self-hosted models (vLLM, Ollama, any OpenAI-compatible server)

ai-panel's channel service (`ai-panel-channel`, port `8086`) can route requests to models you run yourself,
next to hosted providers such as OpenAI or Anthropic. Clients keep using one OpenAI-compatible endpoint and one
ai-panel API key, and you get key management, quotas, rate limits and usage logs for your own GPUs too.

What works:

- Provider types `vllm`, `ollama`, `openai_compatible` (plus `sglang`, `lmstudio`, …) with a custom base URL.
  The API key is optional for these types.
- Any model name: `Qwen/Qwen3-32B`, `glm-4.5`, `deepseek-r1:32b`, your own fine-tune. A model is routable as soon as
  it is registered on an enabled provider.
- Several providers of the same type, for example two vLLM boxes. Lower `priority` is tried first, and retryable
  errors (connection refused, timeouts, 429, 5xx) fail over to the next provider that serves the model.
- Streaming and non-streaming responses.
- Tool calling: `tools`, `tool_choice`, `parallel_tool_calls`, assistant `tool_calls` and `tool` messages are passed
  through unchanged, and so are image content parts and unknown extra fields such as `top_k` or `chat_template_kwargs`.
  Streaming `tool_calls` deltas and the real `finish_reason` come back to the client.
- No hidden defaults. ai-panel does not inject `temperature`, `max_tokens` or `top_p`, so the server's own defaults
  (for example the model's `generation_config.json` in vLLM) apply unless the client sets them.

## 1. Start a model server

### vLLM (Qwen3, DeepSeek-R1 distills, GLM, …)

Tool calling in vLLM needs `--enable-auto-tool-choice` and a parser that matches the model's chat template:

```bash
# Qwen3 (hermes-style tool calls)
vllm serve Qwen/Qwen3-32B \
  --host 0.0.0.0 --port 8000 \
  --enable-auto-tool-choice --tool-call-parser hermes \
  --reasoning-parser qwen3            # optional: returns reasoning in `reasoning_content`

# DeepSeek-R1 distill (reasoning model, no native tool parser)
vllm serve deepseek-ai/DeepSeek-R1-Distill-Qwen-32B \
  --host 0.0.0.0 --port 8001 \
  --reasoning-parser deepseek_r1

# Optional: require a key on the vLLM side too
#   --api-key "$VLLM_API_KEY"   (then enter the same key in ai-panel)
```

Check that it works with `curl http://gpu-host:8000/v1/models`. The parser names change between vLLM releases, so see the
[vLLM tool calling docs](https://docs.vllm.ai/en/latest/features/tool_calling.html) for the right
`--tool-call-parser` for your model (`hermes`, `llama3_json`, `mistral`, `glm45`, `deepseek_v3`, …).

### Ollama

```bash
ollama pull qwen3:32b
ollama pull deepseek-r1:32b
# Ollama serves an OpenAI-compatible API on :11434/v1. Listen on all interfaces if ai-panel runs elsewhere:
OLLAMA_HOST=0.0.0.0:11434 ollama serve
```

Ollama supports tool calling for models whose template supports it (for example Qwen3 and Llama 3.1+).

## 2. Register the server in ai-panel

### From the console

1. Sign in as an admin and open **Providers**, then click **Add Provider**.
2. Pick **vLLM**, **Ollama** or **Other OpenAI-compatible server**.
3. **Base URL**: the OpenAI-compatible root, for example `http://gpu-host:8000/v1` (vLLM) or `http://gpu-host:11434/v1` (Ollama).
   If the URL has no path, ai-panel adds `/v1`. When ai-panel runs in Docker and the model server runs on the same
   machine, use `http://host.docker.internal:8000/v1` (`docker-compose.dev.yml` maps that name to the host).
4. **API key**: leave it empty unless the server was started with `--api-key`.
5. **Models**: type the exact model ids clients will send (`Qwen/Qwen3-32B`, `deepseek-r1:32b`, …) and press Enter.
   You can add more later from the provider's **Models** dialog, where **Fetch from server** lists what the server
   reports on `GET /v1/models`.

### With the admin API

The provider APIs need an admin JWT. They are served by the channel service and also routed through the gateway
(`/api/v1/providers/**`, admin only).

```bash
# Admin access token from the user service (through the gateway)
TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<your-admin-password>"}' | jq -r .data.accessToken)

# Register a vLLM server and its model
curl -s http://localhost:8080/api/v1/providers \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
        "name": "GPU box 1 (vLLM)",
        "type": "vllm",
        "baseUrl": "http://gpu-host:8000/v1",
        "models": ["Qwen/Qwen3-32B"],
        "priority": 10,
        "timeoutSeconds": 300
      }'

# Ask the server which models it serves (nothing is saved), then add one
curl -s http://localhost:8080/api/v1/providers/<id>/models/discover -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/v1/providers/<id>/models \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"modelId": "deepseek-r1:32b"}'
```

Other endpoints: `GET /api/v1/providers/{id}/models`, `PUT`/`DELETE /api/v1/providers/{id}/models/{modelPk}`, and
`POST /api/v1/providers/keys` to attach a key to a provider later.

Providers and models are cached in memory. Changes made through the API take effect immediately, and direct database
edits are picked up within 60 seconds (or call `POST /api/v1/providers/reload`).

### Routing rules

For a request with `"model": "X"`:

1. enabled providers that list `X` as an enabled model, in `priority` order (lowest first);
2. otherwise, enabled providers whose type matches the model-name prefix (`gpt-*` → openai, `claude-*` → anthropic,
   `deepseek*` → deepseek, `glm*` → zhipu, `qwen*` → qwen, …);
3. otherwise, the `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` environment fallback when `OPENAI_ENABLED` /
   `ANTHROPIC_ENABLED` is `true`.

Providers that need a key (openai, deepseek, …) are skipped while they have no usable key. Self-hosted types are
always eligible. If nothing matches, the API returns `404` with `"code": "model_not_found"`.

## 3. Call it

Clients authenticate with an ai-panel **channel API key** (create a channel and generate keys under
`/api/v1/channels`), not with the vLLM key.

### curl

```bash
curl http://localhost:8086/v1/chat/completions \
  -H "Authorization: Bearer $AI_PANEL_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "Qwen/Qwen3-32B",
    "messages": [{"role": "user", "content": "What is the weather in Paris?"}],
    "tools": [{
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Get the current weather for a city",
        "parameters": {
          "type": "object",
          "properties": {"city": {"type": "string"}},
          "required": ["city"]
        }
      }
    }],
    "tool_choice": "auto"
  }'
```

A tool call comes back as `choices[0].message.tool_calls` with `"finish_reason": "tool_calls"`. Add `"stream": true`
to get Server-Sent Events with incremental `tool_calls` deltas, terminated by `data: [DONE]`.

### OpenAI Python SDK

```python
import json
from openai import OpenAI

client = OpenAI(base_url="http://localhost:8086/v1", api_key="<your ai-panel key>")

tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "Get the current weather for a city",
        "parameters": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]},
    },
}]
messages = [{"role": "user", "content": "What is the weather in Paris?"}]

first = client.chat.completions.create(model="Qwen/Qwen3-32B", messages=messages, tools=tools)
msg = first.choices[0].message
print(first.choices[0].finish_reason)          # "tool_calls"

# Run the tool yourself, then send the result back
call = msg.tool_calls[0]
result = {"city": json.loads(call.function.arguments)["city"], "temp_c": 18, "sky": "sunny"}
messages += [msg.model_dump(exclude_none=True),
             {"role": "tool", "tool_call_id": call.id, "content": json.dumps(result)}]

final = client.chat.completions.create(model="Qwen/Qwen3-32B", messages=messages, tools=tools)
print(final.choices[0].message.content)

# Streaming
for chunk in client.chat.completions.create(model="Qwen/Qwen3-32B", messages=messages[:1],
                                            tools=tools, stream=True):
    if chunk.choices and chunk.choices[0].delta.tool_calls:
        print(chunk.choices[0].delta.tool_calls[0])
```

The same works for `deepseek-r1:32b` on Ollama or any other registered model: only `model` changes.

## Anthropic and other hosted providers

The same `/v1/chat/completions` request also works for `claude-*` models on an `anthropic` provider. ai-panel
translates `tools` / `tool_choice` / `tool` messages / image parts to Anthropic's `tool_use` / `tool_result`
format and translates the response and stream back, including `tool_calls` deltas. The Anthropic provider honours
a custom base URL (for example a proxy). Anthropic requires `max_tokens`, so ai-panel sends 4096 when the client
didn't set it.

## Troubleshooting

| Symptom | Cause |
|---|---|
| `404 model_not_found` | The model id isn't registered on an enabled provider (ids are matched exactly, then case-insensitively). |
| `400 … "auto" tool choice requires --enable-auto-tool-choice …` | Start vLLM with `--enable-auto-tool-choice --tool-call-parser …`. Upstream `4xx` errors are returned to the client unchanged. |
| `502 upstream_unreachable` | ai-panel can't reach the base URL. From Docker, use `host.docker.internal` or the host's LAN IP, not `localhost`. |
| Usage shows estimated tokens | Some servers don't report usage when streaming. ai-panel then estimates it with a tokenizer. Clients can send `"stream_options": {"include_usage": true}` to get exact numbers from vLLM. |
