INSERT INTO t_provider (provider_code, provider_name, base_url, priority, rate_limit_per_minute, timeout, enabled, health_status, create_time, update_time)
VALUES
  ('openai', 'OpenAI', 'https://api.openai.com/v1', 1, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('anthropic', 'Anthropic', 'https://api.anthropic.com/v1', 2, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('google', 'Google AI', 'https://generativelanguage.googleapis.com/v1beta/openai/', 3, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('deepseek', 'DeepSeek', 'https://api.deepseek.com', 4, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('moonshot', 'Moonshot', 'https://api.moonshot.cn/v1', 5, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('zhipu', 'Zhipu AI', 'https://open.bigmodel.cn/api/paas/v4', 6, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('qwen', 'Qwen', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 7, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('minimax', 'MiniMax', 'https://api.minimax.chat/v1', 8, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('openrouter', 'OpenRouter', 'https://openrouter.ai/api/v1', 9, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000);

INSERT INTO t_model (model_id, display_name, provider_code, model_type, context_window, max_output_tokens, input_price, output_price, features, fallback_chain, enabled, deprecated, description, create_time, update_time)
VALUES
  ('gpt-4o', 'GPT-4o', 'openai', 'CHAT', 128000, 16384, 0.0025, 0.0100, '["vision","function_calling"]', '[]', 1, 0, 'OpenAI flagship multimodal', unixepoch() * 1000, unixepoch() * 1000),
  ('gpt-4o-mini', 'GPT-4o Mini', 'openai', 'CHAT', 128000, 16384, 0.00015, 0.0006, '["vision","function_calling"]', '[]', 1, 0, 'Cost-efficient GPT-4o', unixepoch() * 1000, unixepoch() * 1000),
  ('o3-mini', 'o3 Mini', 'openai', 'CHAT', 200000, 100000, 0.0011, 0.0044, '["reasoning"]', '[]', 1, 0, 'OpenAI o3 mini', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-sonnet-4', 'Claude Sonnet 4', 'anthropic', 'CHAT', 200000, 64000, 0.003, 0.015, '["vision","function_calling"]', '["gpt-4o"]', 1, 0, 'Claude Sonnet 4', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-opus-4', 'Claude Opus 4', 'anthropic', 'CHAT', 200000, 32000, 0.015, 0.075, '["vision","function_calling"]', '[]', 1, 0, 'Claude Opus 4', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-3-5-sonnet-20241022', 'Claude 3.5 Sonnet', 'anthropic', 'CHAT', 200000, 8192, 0.003, 0.015, '["vision","function_calling"]', '["gpt-4o"]', 1, 0, 'Claude 3.5 Sonnet', unixepoch() * 1000, unixepoch() * 1000),
  ('gemini-2.0-flash', 'Gemini 2.0 Flash', 'google', 'CHAT', 1048576, 8192, 0.0001, 0.0004, '["vision","function_calling"]', '[]', 1, 0, 'Gemini 2.0 Flash', unixepoch() * 1000, unixepoch() * 1000),
  ('deepseek-chat', 'DeepSeek Chat', 'deepseek', 'CHAT', 64000, 8192, 0.00014, 0.00028, '["function_calling"]', '[]', 1, 0, 'DeepSeek chat', unixepoch() * 1000, unixepoch() * 1000),
  ('kimi-k2.5', 'Kimi K2.5', 'moonshot', 'CHAT', 131072, 8192, 0.002, 0.008, '["function_calling"]', '[]', 1, 0, 'Kimi K2.5', unixepoch() * 1000, unixepoch() * 1000),
  ('glm-4', 'GLM-4', 'zhipu', 'CHAT', 128000, 4096, 0.001, 0.001, '["vision","function_calling"]', '[]', 1, 0, 'Zhipu GLM-4', unixepoch() * 1000, unixepoch() * 1000),
  ('qwen-plus', 'Qwen Plus', 'qwen', 'CHAT', 131072, 8192, 0.0008, 0.002, '["function_calling"]', '[]', 1, 0, 'Qwen Plus', unixepoch() * 1000, unixepoch() * 1000),
  ('MiniMax-Text-01', 'MiniMax Text 01', 'minimax', 'CHAT', 1000000, 8192, 0.001, 0.001, '["function_calling"]', '[]', 1, 0, 'MiniMax Text', unixepoch() * 1000, unixepoch() * 1000),
  ('openrouter/auto', 'OpenRouter Auto', 'openrouter', 'CHAT', 128000, 8192, 0.002, 0.008, '["function_calling"]', '[]', 1, 0, 'OpenRouter auto', unixepoch() * 1000, unixepoch() * 1000);
