-- AI Panel Dispatch Service Database Schema
USE ai_panel_dispatch;

-- Model Provider table
CREATE TABLE IF NOT EXISTS t_model_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code VARCHAR(50) NOT NULL UNIQUE,
    provider_name VARCHAR(100) NOT NULL,
    base_url VARCHAR(500),
    api_key VARCHAR(500) COMMENT 'Encrypted API key',
    priority INT DEFAULT 100 COMMENT 'Lower = higher priority',
    rate_limit_per_minute INT DEFAULT 60,
    timeout INT DEFAULT 60000 COMMENT 'Timeout in milliseconds',
    enabled TINYINT DEFAULT 1,
    health_status TINYINT DEFAULT 0 COMMENT '0=healthy, 1=degraded, 2=unhealthy',
    last_health_check DATETIME,
    config JSON,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_provider_code (provider_code),
    INDEX idx_enabled (enabled),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Model Info table
CREATE TABLE IF NOT EXISTS t_model_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    provider_code VARCHAR(50) NOT NULL,
    model_type VARCHAR(50) NOT NULL COMMENT 'CHAT, EMBEDDING, IMAGE_GENERATION, etc.',
    context_window INT,
    max_output_tokens INT,
    input_price DECIMAL(12, 6) COMMENT 'Price per 1K tokens',
    output_price DECIMAL(12, 6) COMMENT 'Price per 1K tokens',
    features JSON COMMENT 'Supported features array',
    fallback_chain JSON COMMENT 'Fallback model IDs array',
    enabled TINYINT DEFAULT 1,
    deprecated TINYINT DEFAULT 0,
    description TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_model_id (model_id),
    INDEX idx_provider_code (provider_code),
    INDEX idx_model_type (model_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Request Log table (high-write, consider sharding for production)
CREATE TABLE IF NOT EXISTS t_request_log (
    id BIGINT AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    api_key_id BIGINT,
    requested_model VARCHAR(100),
    actual_model VARCHAR(100),
    provider_code VARCHAR(50),
    request_type VARCHAR(20) COMMENT 'chat, stream, embedding',
    input_tokens INT DEFAULT 0,
    output_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    latency_ms BIGINT,
    status VARCHAR(20) COMMENT 'success, error, timeout',
    error_message VARCHAR(1000),
    client_ip VARCHAR(50),
    user_agent VARCHAR(500),
    fallback_used TINYINT DEFAULT 0,
    retry_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, create_time),
    INDEX idx_request_id (request_id),
    INDEX idx_user_id (user_id),
    INDEX idx_api_key_id (api_key_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- Insert default providers
INSERT INTO t_model_provider (provider_code, provider_name, base_url, priority) VALUES
('openai', 'OpenAI', 'https://api.openai.com/v1', 10),
('anthropic', 'Anthropic', 'https://api.anthropic.com', 20),
('google', 'Google AI', 'https://generativelanguage.googleapis.com', 30),
('deepseek', 'DeepSeek', 'https://api.deepseek.com', 40),
('moonshot', 'Moonshot', 'https://api.moonshot.cn/v1', 50),
('zhipu', 'Zhipu AI', 'https://open.bigmodel.cn/api/paas/v4', 60),
('baichuan', 'Baichuan', 'https://api.baichuan-ai.com/v1', 70),
('qwen', 'Qwen', 'https://dashscope.aliyuncs.com/api/v1', 80)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- Insert default models
INSERT INTO t_model_info (model_id, display_name, provider_code, model_type, context_window, max_output_tokens, input_price, output_price, features) VALUES
('gpt-4o', 'GPT-4o', 'openai', 'CHAT', 128000, 4096, 0.005, 0.015, '["vision", "function_calling"]'),
('gpt-4o-mini', 'GPT-4o Mini', 'openai', 'CHAT', 128000, 16384, 0.00015, 0.0006, '["vision", "function_calling"]'),
('gpt-4-turbo', 'GPT-4 Turbo', 'openai', 'CHAT', 128000, 4096, 0.01, 0.03, '["vision", "function_calling"]'),
('gpt-3.5-turbo', 'GPT-3.5 Turbo', 'openai', 'CHAT', 16385, 4096, 0.0005, 0.0015, '["function_calling"]'),
('claude-3-5-sonnet-20241022', 'Claude 3.5 Sonnet', 'anthropic', 'CHAT', 200000, 8192, 0.003, 0.015, '["vision"]'),
('claude-3-opus-20240229', 'Claude 3 Opus', 'anthropic', 'CHAT', 200000, 4096, 0.015, 0.075, '["vision"]'),
('claude-3-haiku-20240307', 'Claude 3 Haiku', 'anthropic', 'CHAT', 200000, 4096, 0.00025, 0.00125, '["vision"]'),
('deepseek-chat', 'DeepSeek Chat', 'deepseek', 'CHAT', 64000, 4096, 0.00014, 0.00028, '[]'),
('deepseek-coder', 'DeepSeek Coder', 'deepseek', 'CHAT', 64000, 4096, 0.00014, 0.00028, '[]'),
('moonshot-v1-8k', 'Moonshot 8K', 'moonshot', 'CHAT', 8000, 4096, 0.012, 0.012, '[]'),
('moonshot-v1-32k', 'Moonshot 32K', 'moonshot', 'CHAT', 32000, 4096, 0.024, 0.024, '[]'),
('glm-4', 'GLM-4', 'zhipu', 'CHAT', 128000, 4096, 0.1, 0.1, '["vision"]')
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
