-- V005: Expanded model catalog + model groups with price multipliers (enterprise internal resale)

USE ai_panel_dispatch;

-- Model groups (号池 / pricing tiers for employee sell price)
CREATE TABLE IF NOT EXISTS t_model_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    price_multiplier DECIMAL(8,4) NOT NULL DEFAULT 1.0000 COMMENT 'Employee sell price = cost * multiplier',
    description VARCHAR(512),
    enabled TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 100,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_group_code (group_code, deleted),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_model_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_model (group_id, model_id),
    INDEX idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_model_label (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id VARCHAR(100) NOT NULL,
    label VARCHAR(64) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_label (model_id, label),
    INDEX idx_label (label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Extra providers (CC Switch–aligned ecosystem)
INSERT INTO t_model_provider (provider_code, provider_name, base_url, priority) VALUES
('minimax', 'MiniMax', 'https://api.minimax.chat/v1', 90),
('openrouter', 'OpenRouter', 'https://openrouter.ai/api/v1', 100),
('siliconflow', 'SiliconFlow', 'https://api.siliconflow.cn/v1', 110),
('stepfun', 'StepFun', 'https://api.stepfun.com/v1', 120),
('doubao', 'Doubao / Volcengine', 'https://ark.cn-beijing.volces.com/api/v3', 130),
('azure', 'Azure OpenAI', 'https://YOUR_RESOURCE.openai.azure.com', 15)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- Expanded mainstream catalog (prices per 1K tokens USD cost basis)
INSERT INTO t_model_info (model_id, display_name, provider_code, model_type, context_window, max_output_tokens, input_price, output_price, features, description) VALUES
-- OpenAI
('gpt-4o', 'GPT-4o', 'openai', 'CHAT', 128000, 16384, 0.0025, 0.0100, '["vision","function_calling"]', 'OpenAI flagship multimodal'),
('gpt-4o-mini', 'GPT-4o Mini', 'openai', 'CHAT', 128000, 16384, 0.00015, 0.0006, '["vision","function_calling"]', 'Cost-efficient GPT-4o'),
('gpt-4-turbo', 'GPT-4 Turbo', 'openai', 'CHAT', 128000, 4096, 0.0100, 0.0300, '["vision","function_calling"]', 'GPT-4 Turbo'),
('gpt-4.1', 'GPT-4.1', 'openai', 'CHAT', 1047576, 32768, 0.0020, 0.0080, '["vision","function_calling"]', 'GPT-4.1'),
('o1', 'o1', 'openai', 'CHAT', 200000, 100000, 0.0150, 0.0600, '["reasoning"]', 'OpenAI o1 reasoning'),
('o1-mini', 'o1 Mini', 'openai', 'CHAT', 128000, 65536, 0.0030, 0.0120, '["reasoning"]', 'OpenAI o1 mini'),
('o3-mini', 'o3 Mini', 'openai', 'CHAT', 200000, 100000, 0.0011, 0.0044, '["reasoning"]', 'OpenAI o3 mini'),
-- Anthropic
('claude-opus-4', 'Claude Opus 4', 'anthropic', 'CHAT', 200000, 32000, 0.0150, 0.0750, '["vision","function_calling"]', 'Claude Opus 4'),
('claude-opus-4-1', 'Claude Opus 4.1', 'anthropic', 'CHAT', 200000, 32000, 0.0150, 0.0750, '["vision","function_calling"]', 'Claude Opus 4.1'),
('claude-sonnet-4', 'Claude Sonnet 4', 'anthropic', 'CHAT', 200000, 64000, 0.0030, 0.0150, '["vision","function_calling"]', 'Claude Sonnet 4'),
('claude-sonnet-4-5', 'Claude Sonnet 4.5', 'anthropic', 'CHAT', 200000, 64000, 0.0030, 0.0150, '["vision","function_calling"]', 'Claude Sonnet 4.5'),
('claude-3-5-sonnet-20241022', 'Claude 3.5 Sonnet', 'anthropic', 'CHAT', 200000, 8192, 0.0030, 0.0150, '["vision","function_calling"]', 'Claude 3.5 Sonnet'),
('claude-3-5-haiku-20241022', 'Claude 3.5 Haiku', 'anthropic', 'CHAT', 200000, 8192, 0.0008, 0.0040, '["vision","function_calling"]', 'Claude 3.5 Haiku'),
('claude-3-opus-20240229', 'Claude 3 Opus', 'anthropic', 'CHAT', 200000, 4096, 0.0150, 0.0750, '["vision"]', 'Claude 3 Opus'),
('claude-3-haiku-20240307', 'Claude 3 Haiku', 'anthropic', 'CHAT', 200000, 4096, 0.00025, 0.00125, '["vision"]', 'Claude 3 Haiku'),
-- Google
('gemini-2.0-flash', 'Gemini 2.0 Flash', 'google', 'CHAT', 1048576, 8192, 0.0001, 0.0004, '["vision","function_calling"]', 'Gemini 2.0 Flash'),
('gemini-1.5-pro', 'Gemini 1.5 Pro', 'google', 'CHAT', 2097152, 8192, 0.00125, 0.0050, '["vision","function_calling"]', 'Gemini 1.5 Pro'),
('gemini-1.5-flash', 'Gemini 1.5 Flash', 'google', 'CHAT', 1048576, 8192, 0.000075, 0.0003, '["vision","function_calling"]', 'Gemini 1.5 Flash'),
-- DeepSeek
('deepseek-chat', 'DeepSeek Chat', 'deepseek', 'CHAT', 64000, 8192, 0.00014, 0.00028, '["function_calling"]', 'DeepSeek V3 chat'),
('deepseek-reasoner', 'DeepSeek Reasoner', 'deepseek', 'CHAT', 64000, 8192, 0.00055, 0.00219, '["reasoning"]', 'DeepSeek R1'),
('deepseek-coder', 'DeepSeek Coder', 'deepseek', 'CHAT', 64000, 8192, 0.00014, 0.00028, '["function_calling"]', 'DeepSeek Coder'),
-- Moonshot / Kimi
('moonshot-v1-8k', 'Moonshot v1 8K', 'moonshot', 'CHAT', 8000, 4096, 0.012, 0.012, '[]', 'Kimi 8K'),
('moonshot-v1-32k', 'Moonshot v1 32K', 'moonshot', 'CHAT', 32000, 4096, 0.024, 0.024, '[]', 'Kimi 32K'),
('moonshot-v1-128k', 'Moonshot v1 128K', 'moonshot', 'CHAT', 128000, 4096, 0.060, 0.060, '[]', 'Kimi 128K'),
('kimi-k2.5', 'Kimi K2.5', 'moonshot', 'CHAT', 131072, 8192, 0.0020, 0.0080, '["function_calling"]', 'Kimi K2.5'),
-- Zhipu
('glm-4', 'GLM-4', 'zhipu', 'CHAT', 128000, 4096, 0.0010, 0.0010, '["vision","function_calling"]', 'Zhipu GLM-4'),
('glm-4-flash', 'GLM-4 Flash', 'zhipu', 'CHAT', 128000, 4096, 0.0001, 0.0001, '["function_calling"]', 'Zhipu GLM-4 Flash'),
('glm-4-plus', 'GLM-4 Plus', 'zhipu', 'CHAT', 128000, 4096, 0.0050, 0.0050, '["vision","function_calling"]', 'Zhipu GLM-4 Plus'),
-- Qwen
('qwen-turbo', 'Qwen Turbo', 'qwen', 'CHAT', 131072, 8192, 0.0003, 0.0006, '["function_calling"]', 'Qwen Turbo'),
('qwen-plus', 'Qwen Plus', 'qwen', 'CHAT', 131072, 8192, 0.0008, 0.0020, '["function_calling"]', 'Qwen Plus'),
('qwen-max', 'Qwen Max', 'qwen', 'CHAT', 32768, 8192, 0.0020, 0.0060, '["function_calling"]', 'Qwen Max'),
('qwen-coder-plus', 'Qwen Coder Plus', 'qwen', 'CHAT', 131072, 8192, 0.0008, 0.0020, '["function_calling"]', 'Qwen Coder'),
-- MiniMax
('MiniMax-Text-01', 'MiniMax Text 01', 'minimax', 'CHAT', 1000000, 8192, 0.0010, 0.0010, '["function_calling"]', 'MiniMax Text'),
('abab6.5s-chat', 'ABAB 6.5s', 'minimax', 'CHAT', 245760, 8192, 0.0010, 0.0010, '[]', 'MiniMax ABAB 6.5s'),
-- OpenRouter / SiliconFlow sample routes
('openrouter/auto', 'OpenRouter Auto', 'openrouter', 'CHAT', 128000, 8192, 0.0020, 0.0080, '["function_calling"]', 'OpenRouter auto router'),
('deepseek-ai/DeepSeek-V3', 'DeepSeek V3 (SiliconFlow)', 'siliconflow', 'CHAT', 64000, 8192, 0.00014, 0.00028, '["function_calling"]', 'SiliconFlow DeepSeek V3'),
-- StepFun / Doubao
('step-2-16k', 'Step-2 16K', 'stepfun', 'CHAT', 16000, 4096, 0.0038, 0.0038, '[]', 'StepFun Step-2'),
('doubao-pro-32k', 'Doubao Pro 32K', 'doubao', 'CHAT', 32000, 4096, 0.0008, 0.0020, '["function_calling"]', 'Volcengine Doubao Pro')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    provider_code = VALUES(provider_code),
    context_window = VALUES(context_window),
    max_output_tokens = VALUES(max_output_tokens),
    input_price = VALUES(input_price),
    output_price = VALUES(output_price),
    features = VALUES(features),
    description = VALUES(description),
    update_time = CURRENT_TIMESTAMP;

-- Default groups (enterprise internal resale tiers)
INSERT INTO t_model_group (group_code, group_name, price_multiplier, description, enabled, sort_order) VALUES
('claude-official', 'Claude-Official-Main', 1.5000, 'Official Claude pool for internal premium use', 1, 10),
('openai-main', 'OpenAI-Main', 1.2000, 'OpenAI / Azure pool', 1, 20),
('domestic-agg', 'Domestic-Aggregation', 0.3000, 'DeepSeek / Kimi / GLM / Qwen aggregation', 1, 30),
('gemini-main', 'Gemini-Main', 1.1000, 'Google Gemini pool', 1, 40),
('welfare', 'Welfare-Pool', 0.0700, 'Low-cost welfare pool for light usage', 1, 50)
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    price_multiplier = VALUES(price_multiplier),
    description = VALUES(description),
    update_time = CURRENT_TIMESTAMP;

-- Group memberships
INSERT IGNORE INTO t_model_group_member (group_id, model_id)
SELECT g.id, m.model_id FROM t_model_group g
CROSS JOIN (
    SELECT 'claude-opus-4' AS model_id UNION ALL SELECT 'claude-opus-4-1' UNION ALL
    SELECT 'claude-sonnet-4' UNION ALL SELECT 'claude-sonnet-4-5' UNION ALL
    SELECT 'claude-3-5-sonnet-20241022' UNION ALL SELECT 'claude-3-5-haiku-20241022' UNION ALL
    SELECT 'claude-3-opus-20240229' UNION ALL SELECT 'claude-3-haiku-20240307'
) m WHERE g.group_code = 'claude-official';

INSERT IGNORE INTO t_model_group_member (group_id, model_id)
SELECT g.id, m.model_id FROM t_model_group g
CROSS JOIN (
    SELECT 'gpt-4o' AS model_id UNION ALL SELECT 'gpt-4o-mini' UNION ALL SELECT 'gpt-4-turbo' UNION ALL
    SELECT 'gpt-4.1' UNION ALL SELECT 'o1' UNION ALL SELECT 'o1-mini' UNION ALL SELECT 'o3-mini'
) m WHERE g.group_code = 'openai-main';

INSERT IGNORE INTO t_model_group_member (group_id, model_id)
SELECT g.id, m.model_id FROM t_model_group g
CROSS JOIN (
    SELECT 'deepseek-chat' AS model_id UNION ALL SELECT 'deepseek-reasoner' UNION ALL SELECT 'deepseek-coder' UNION ALL
    SELECT 'moonshot-v1-8k' UNION ALL SELECT 'moonshot-v1-32k' UNION ALL SELECT 'moonshot-v1-128k' UNION ALL
    SELECT 'kimi-k2.5' UNION ALL SELECT 'glm-4' UNION ALL SELECT 'glm-4-flash' UNION ALL SELECT 'glm-4-plus' UNION ALL
    SELECT 'qwen-turbo' UNION ALL SELECT 'qwen-plus' UNION ALL SELECT 'qwen-max' UNION ALL SELECT 'qwen-coder-plus' UNION ALL
    SELECT 'MiniMax-Text-01' UNION ALL SELECT 'abab6.5s-chat' UNION ALL
    SELECT 'deepseek-ai/DeepSeek-V3' UNION ALL SELECT 'step-2-16k' UNION ALL SELECT 'doubao-pro-32k'
) m WHERE g.group_code = 'domestic-agg';

INSERT IGNORE INTO t_model_group_member (group_id, model_id)
SELECT g.id, m.model_id FROM t_model_group g
CROSS JOIN (
    SELECT 'gemini-2.0-flash' AS model_id UNION ALL SELECT 'gemini-1.5-pro' UNION ALL SELECT 'gemini-1.5-flash'
) m WHERE g.group_code = 'gemini-main';

INSERT IGNORE INTO t_model_group_member (group_id, model_id)
SELECT g.id, m.model_id FROM t_model_group g
CROSS JOIN (
    SELECT 'gpt-4o-mini' AS model_id UNION ALL SELECT 'glm-4-flash' UNION ALL SELECT 'qwen-turbo' UNION ALL
    SELECT 'deepseek-chat' UNION ALL SELECT 'openrouter/auto'
) m WHERE g.group_code = 'welfare';

-- Endpoint protocol labels for filters
INSERT IGNORE INTO t_model_label (model_id, label)
SELECT model_id, 'chat' FROM t_model_info WHERE model_type = 'CHAT' AND deleted = 0;

INSERT IGNORE INTO t_model_label (model_id, label)
SELECT model_id, provider_code FROM t_model_info WHERE deleted = 0;
