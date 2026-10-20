-- Provider Configuration Tables for AI Panel Channel Service
-- Supports multiple providers and key pools

USE ai_panel_channel;

-- Provider type configuration (e.g., openai, anthropic, deepseek)
CREATE TABLE IF NOT EXISTS provider_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT 'Provider name for display',
    type VARCHAR(30) NOT NULL COMMENT 'Provider type: openai, anthropic, deepseek, moonshot, etc.',
    base_url VARCHAR(255) COMMENT 'Base URL for API calls',
    enabled TINYINT(1) DEFAULT 1 COMMENT 'Whether this provider is enabled',
    priority INT DEFAULT 100 COMMENT 'Priority for fallback routing (lower = higher priority)',
    timeout_seconds INT DEFAULT 60 COMMENT 'Request timeout in seconds',
    retry_count INT DEFAULT 3 COMMENT 'Number of retries on failure',
    description VARCHAR(500) COMMENT 'Description of this provider',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI Provider Configuration';

-- API Key pool for each provider
CREATE TABLE IF NOT EXISTS provider_api_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_id BIGINT NOT NULL COMMENT 'Reference to provider_config.id',
    key_name VARCHAR(100) NOT NULL COMMENT 'Name/alias for this key',
    api_key VARCHAR(500) NOT NULL COMMENT 'Encrypted API key',
    key_prefix VARCHAR(20) COMMENT 'Key prefix for display (e.g., sk-****1234)',
    enabled TINYINT(1) DEFAULT 1 COMMENT 'Whether this key is enabled',
    weight INT DEFAULT 100 COMMENT 'Weight for load balancing (higher = more traffic)',
    rate_limit_per_minute INT COMMENT 'Rate limit for this specific key',
    daily_quota BIGINT COMMENT 'Daily token quota for this key',
    used_quota_today BIGINT DEFAULT 0 COMMENT 'Tokens used today',
    total_requests BIGINT DEFAULT 0 COMMENT 'Total API requests made with this key',
    total_tokens BIGINT DEFAULT 0 COMMENT 'Total tokens consumed by this key',
    last_used_at DATETIME COMMENT 'Last time this key was used',
    last_error_at DATETIME COMMENT 'Last time this key encountered an error',
    last_error_message VARCHAR(500) COMMENT 'Last error message',
    consecutive_errors INT DEFAULT 0 COMMENT 'Consecutive error count (for circuit breaker)',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'Status: active, rate_limited, error, disabled',
    expires_at DATETIME COMMENT 'Key expiration date',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_provider_id (provider_id),
    INDEX idx_status (status),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Provider API Key Pool';

-- Model configuration for each provider
CREATE TABLE IF NOT EXISTS provider_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_id BIGINT NOT NULL COMMENT 'Reference to provider_config.id',
    model_id VARCHAR(100) NOT NULL COMMENT 'Model identifier (e.g., gpt-4, claude-3-opus)',
    model_name VARCHAR(100) COMMENT 'Display name',
    enabled TINYINT(1) DEFAULT 1 COMMENT 'Whether this model is enabled',
    input_price_per_million DECIMAL(10,4) COMMENT 'Input token price per million tokens (USD)',
    output_price_per_million DECIMAL(10,4) COMMENT 'Output token price per million tokens (USD)',
    max_tokens INT COMMENT 'Maximum tokens for this model',
    context_window INT COMMENT 'Context window size',
    supports_vision TINYINT(1) DEFAULT 0 COMMENT 'Whether model supports vision/images',
    supports_function_call TINYINT(1) DEFAULT 0 COMMENT 'Whether model supports function calling',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_provider_model (provider_id, model_id, deleted),
    INDEX idx_provider_id (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Provider Model Configuration';

-- Model alias mapping (e.g., gpt-4 -> gpt-4-turbo-preview)
CREATE TABLE IF NOT EXISTS model_alias (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias VARCHAR(100) NOT NULL COMMENT 'Alias name (e.g., gpt-4)',
    target_provider_id BIGINT NOT NULL COMMENT 'Target provider',
    target_model_id VARCHAR(100) NOT NULL COMMENT 'Target model ID',
    enabled TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_alias (alias, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Model Alias Mapping';

-- Insert default providers
INSERT INTO provider_config (name, type, base_url, enabled, priority, description) VALUES
('OpenAI', 'openai', 'https://api.openai.com', 1, 100, 'OpenAI GPT models'),
('Anthropic', 'anthropic', 'https://api.anthropic.com', 1, 100, 'Anthropic Claude models'),
('DeepSeek', 'deepseek', 'https://api.deepseek.com', 1, 200, 'DeepSeek models'),
('Moonshot', 'moonshot', 'https://api.moonshot.cn', 1, 200, 'Moonshot AI models');

-- Insert default models for OpenAI
INSERT INTO provider_model (provider_id, model_id, model_name, enabled, input_price_per_million, output_price_per_million, max_tokens, context_window, supports_vision, supports_function_call) VALUES
(1, 'gpt-4o', 'GPT-4o', 1, 2.50, 10.00, 16384, 128000, 1, 1),
(1, 'gpt-4o-mini', 'GPT-4o Mini', 1, 0.15, 0.60, 16384, 128000, 1, 1),
(1, 'gpt-4-turbo', 'GPT-4 Turbo', 1, 10.00, 30.00, 4096, 128000, 1, 1),
(1, 'gpt-4', 'GPT-4', 1, 30.00, 60.00, 8192, 8192, 0, 1),
(1, 'gpt-3.5-turbo', 'GPT-3.5 Turbo', 1, 0.50, 1.50, 4096, 16385, 0, 1);

-- Insert default models for Anthropic
INSERT INTO provider_model (provider_id, model_id, model_name, enabled, input_price_per_million, output_price_per_million, max_tokens, context_window, supports_vision, supports_function_call) VALUES
(2, 'claude-3-5-sonnet-20241022', 'Claude 3.5 Sonnet', 1, 3.00, 15.00, 8192, 200000, 1, 1),
(2, 'claude-3-opus-20240229', 'Claude 3 Opus', 1, 15.00, 75.00, 4096, 200000, 1, 1),
(2, 'claude-3-sonnet-20240229', 'Claude 3 Sonnet', 1, 3.00, 15.00, 4096, 200000, 1, 1),
(2, 'claude-3-haiku-20240307', 'Claude 3 Haiku', 1, 0.25, 1.25, 4096, 200000, 1, 1);

-- Insert default models for DeepSeek
INSERT INTO provider_model (provider_id, model_id, model_name, enabled, input_price_per_million, output_price_per_million, max_tokens, context_window, supports_vision, supports_function_call) VALUES
(3, 'deepseek-chat', 'DeepSeek Chat', 1, 0.14, 0.28, 8192, 64000, 0, 1),
(3, 'deepseek-coder', 'DeepSeek Coder', 1, 0.14, 0.28, 8192, 64000, 0, 1);

-- Insert model aliases
INSERT INTO model_alias (alias, target_provider_id, target_model_id, enabled) VALUES
('gpt-4', 1, 'gpt-4-turbo', 1),
('claude-3-opus', 2, 'claude-3-opus-20240229', 1),
('claude-3-sonnet', 2, 'claude-3-sonnet-20240229', 1),
('claude-3-haiku', 2, 'claude-3-haiku-20240307', 1),
('claude-3.5-sonnet', 2, 'claude-3-5-sonnet-20241022', 1);
