CREATE TABLE t_model_group (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  group_code TEXT NOT NULL UNIQUE,
  group_name TEXT NOT NULL,
  price_multiplier REAL NOT NULL DEFAULT 1.0,
  description TEXT,
  enabled INTEGER NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 100,
  create_time INTEGER NOT NULL,
  update_time INTEGER NOT NULL,
  deleted INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE t_model_group_member (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  group_id INTEGER NOT NULL,
  model_id TEXT NOT NULL,
  create_time INTEGER NOT NULL,
  UNIQUE (group_id, model_id)
);

CREATE INDEX idx_group_member_model ON t_model_group_member (model_id);

INSERT INTO t_model_group (group_code, group_name, price_multiplier, description, enabled, sort_order, create_time, update_time)
VALUES
  ('claude-official', 'Claude-Official-Main', 1.5, 'Official Claude pool for internal premium use', 1, 10, unixepoch() * 1000, unixepoch() * 1000),
  ('openai-main', 'OpenAI-Main', 1.2, 'OpenAI / Azure pool', 1, 20, unixepoch() * 1000, unixepoch() * 1000),
  ('domestic-agg', 'Domestic-Aggregation', 0.3, 'DeepSeek / Kimi / GLM / Qwen aggregation', 1, 30, unixepoch() * 1000, unixepoch() * 1000),
  ('gemini-main', 'Gemini-Main', 1.1, 'Google Gemini pool', 1, 40, unixepoch() * 1000, unixepoch() * 1000),
  ('welfare', 'Welfare-Pool', 0.07, 'Low-cost welfare pool for light usage', 1, 50, unixepoch() * 1000, unixepoch() * 1000);

INSERT OR IGNORE INTO t_provider (provider_code, provider_name, base_url, priority, rate_limit_per_minute, timeout, enabled, health_status, create_time, update_time)
VALUES
  ('siliconflow', 'SiliconFlow', 'https://api.siliconflow.cn/v1', 10, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('stepfun', 'StepFun', 'https://api.stepfun.com/v1', 11, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000),
  ('doubao', 'Doubao / Volcengine', 'https://ark.cn-beijing.volces.com/api/v3', 12, 500, 30000, 1, 1, unixepoch() * 1000, unixepoch() * 1000);

INSERT OR IGNORE INTO t_model (model_id, display_name, provider_code, model_type, context_window, max_output_tokens, input_price, output_price, features, fallback_chain, enabled, deprecated, description, create_time, update_time)
VALUES
  ('gpt-4-turbo', 'GPT-4 Turbo', 'openai', 'CHAT', 128000, 4096, 0.01, 0.03, '["vision","function_calling"]', '[]', 1, 0, 'GPT-4 Turbo', unixepoch() * 1000, unixepoch() * 1000),
  ('gpt-4.1', 'GPT-4.1', 'openai', 'CHAT', 1047576, 32768, 0.002, 0.008, '["vision","function_calling"]', '[]', 1, 0, 'GPT-4.1', unixepoch() * 1000, unixepoch() * 1000),
  ('o1', 'o1', 'openai', 'CHAT', 200000, 100000, 0.015, 0.06, '["reasoning"]', '[]', 1, 0, 'OpenAI o1', unixepoch() * 1000, unixepoch() * 1000),
  ('o1-mini', 'o1 Mini', 'openai', 'CHAT', 128000, 65536, 0.003, 0.012, '["reasoning"]', '[]', 1, 0, 'OpenAI o1 mini', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-opus-4-1', 'Claude Opus 4.1', 'anthropic', 'CHAT', 200000, 32000, 0.015, 0.075, '["vision","function_calling"]', '[]', 1, 0, 'Claude Opus 4.1', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-sonnet-4-5', 'Claude Sonnet 4.5', 'anthropic', 'CHAT', 200000, 64000, 0.003, 0.015, '["vision","function_calling"]', '[]', 1, 0, 'Claude Sonnet 4.5', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-3-5-haiku-20241022', 'Claude 3.5 Haiku', 'anthropic', 'CHAT', 200000, 8192, 0.0008, 0.004, '["vision","function_calling"]', '[]', 1, 0, 'Claude 3.5 Haiku', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-3-opus-20240229', 'Claude 3 Opus', 'anthropic', 'CHAT', 200000, 4096, 0.015, 0.075, '["vision"]', '[]', 1, 0, 'Claude 3 Opus', unixepoch() * 1000, unixepoch() * 1000),
  ('claude-3-haiku-20240307', 'Claude 3 Haiku', 'anthropic', 'CHAT', 200000, 4096, 0.00025, 0.00125, '["vision"]', '[]', 1, 0, 'Claude 3 Haiku', unixepoch() * 1000, unixepoch() * 1000),
  ('gemini-1.5-pro', 'Gemini 1.5 Pro', 'google', 'CHAT', 2097152, 8192, 0.00125, 0.005, '["vision","function_calling"]', '[]', 1, 0, 'Gemini 1.5 Pro', unixepoch() * 1000, unixepoch() * 1000),
  ('gemini-1.5-flash', 'Gemini 1.5 Flash', 'google', 'CHAT', 1048576, 8192, 0.000075, 0.0003, '["vision","function_calling"]', '[]', 1, 0, 'Gemini 1.5 Flash', unixepoch() * 1000, unixepoch() * 1000),
  ('deepseek-reasoner', 'DeepSeek Reasoner', 'deepseek', 'CHAT', 64000, 8192, 0.00055, 0.00219, '["reasoning"]', '[]', 1, 0, 'DeepSeek R1', unixepoch() * 1000, unixepoch() * 1000);

INSERT OR IGNORE INTO t_model (model_id, display_name, provider_code, model_type, context_window, max_output_tokens, input_price, output_price, features, fallback_chain, enabled, deprecated, description, create_time, update_time)
VALUES
  ('deepseek-coder', 'DeepSeek Coder', 'deepseek', 'CHAT', 64000, 8192, 0.00014, 0.00028, '["function_calling"]', '[]', 1, 0, 'DeepSeek Coder', unixepoch() * 1000, unixepoch() * 1000),
  ('moonshot-v1-8k', 'Moonshot v1 8K', 'moonshot', 'CHAT', 8000, 4096, 0.012, 0.012, '[]', '[]', 1, 0, 'Kimi 8K', unixepoch() * 1000, unixepoch() * 1000),
  ('moonshot-v1-32k', 'Moonshot v1 32K', 'moonshot', 'CHAT', 32000, 4096, 0.024, 0.024, '[]', '[]', 1, 0, 'Kimi 32K', unixepoch() * 1000, unixepoch() * 1000),
  ('moonshot-v1-128k', 'Moonshot v1 128K', 'moonshot', 'CHAT', 128000, 4096, 0.06, 0.06, '[]', '[]', 1, 0, 'Kimi 128K', unixepoch() * 1000, unixepoch() * 1000),
  ('glm-4-flash', 'GLM-4 Flash', 'zhipu', 'CHAT', 128000, 4096, 0.0001, 0.0001, '["function_calling"]', '[]', 1, 0, 'Zhipu GLM-4 Flash', unixepoch() * 1000, unixepoch() * 1000),
  ('glm-4-plus', 'GLM-4 Plus', 'zhipu', 'CHAT', 128000, 4096, 0.005, 0.005, '["vision","function_calling"]', '[]', 1, 0, 'Zhipu GLM-4 Plus', unixepoch() * 1000, unixepoch() * 1000),
  ('qwen-turbo', 'Qwen Turbo', 'qwen', 'CHAT', 131072, 8192, 0.0003, 0.0006, '["function_calling"]', '[]', 1, 0, 'Qwen Turbo', unixepoch() * 1000, unixepoch() * 1000),
  ('qwen-max', 'Qwen Max', 'qwen', 'CHAT', 32768, 8192, 0.002, 0.006, '["function_calling"]', '[]', 1, 0, 'Qwen Max', unixepoch() * 1000, unixepoch() * 1000),
  ('qwen-coder-plus', 'Qwen Coder Plus', 'qwen', 'CHAT', 131072, 8192, 0.0008, 0.002, '["function_calling"]', '[]', 1, 0, 'Qwen Coder', unixepoch() * 1000, unixepoch() * 1000),
  ('abab6.5s-chat', 'ABAB 6.5s', 'minimax', 'CHAT', 245760, 8192, 0.001, 0.001, '[]', '[]', 1, 0, 'MiniMax ABAB 6.5s', unixepoch() * 1000, unixepoch() * 1000),
  ('deepseek-ai/DeepSeek-V3', 'DeepSeek V3 (SiliconFlow)', 'siliconflow', 'CHAT', 64000, 8192, 0.00014, 0.00028, '["function_calling"]', '[]', 1, 0, 'SiliconFlow DeepSeek V3', unixepoch() * 1000, unixepoch() * 1000),
  ('step-2-16k', 'Step-2 16K', 'stepfun', 'CHAT', 16000, 4096, 0.0038, 0.0038, '[]', '[]', 1, 0, 'StepFun Step-2', unixepoch() * 1000, unixepoch() * 1000),
  ('doubao-pro-32k', 'Doubao Pro 32K', 'doubao', 'CHAT', 32000, 4096, 0.0008, 0.002, '["function_calling"]', '[]', 1, 0, 'Volcengine Doubao Pro', unixepoch() * 1000, unixepoch() * 1000);

-- Group members via simple inserts (avoid large compound SELECTs)
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-opus-4', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-opus-4-1', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-sonnet-4', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-sonnet-4-5', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-3-5-sonnet-20241022', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-3-5-haiku-20241022', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-3-opus-20240229', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'claude-3-haiku-20240307', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'claude-official';

INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gpt-4o', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'openai-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gpt-4o-mini', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'openai-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gpt-4-turbo', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'openai-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gpt-4.1', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'openai-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'o1', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'openai-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'o1-mini', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'openai-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'o3-mini', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'openai-main';

INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'deepseek-chat', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'deepseek-reasoner', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'deepseek-coder', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'moonshot-v1-8k', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'moonshot-v1-32k', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'moonshot-v1-128k', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'kimi-k2.5', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'glm-4', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'glm-4-flash', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'glm-4-plus', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'qwen-turbo', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'qwen-plus', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'qwen-max', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'qwen-coder-plus', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'MiniMax-Text-01', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'abab6.5s-chat', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'deepseek-ai/DeepSeek-V3', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'step-2-16k', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'doubao-pro-32k', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'domestic-agg';

INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gemini-2.0-flash', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'gemini-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gemini-1.5-pro', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'gemini-main';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gemini-1.5-flash', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'gemini-main';

INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'gpt-4o-mini', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'welfare';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'glm-4-flash', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'welfare';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'qwen-turbo', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'welfare';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'deepseek-chat', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'welfare';
INSERT INTO t_model_group_member (group_id, model_id, create_time)
SELECT id, 'openrouter/auto', unixepoch() * 1000 FROM t_model_group WHERE group_code = 'welfare';
