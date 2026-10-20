-- ============================================
-- AI Panel - Channel & Analytics Schema
-- Version: 2.0
-- Description: Database schema for sales channels,
--              API keys, usage logging, and analytics
-- ============================================

-- ----------------------------------------
-- Table: t_channel (Sales Channels)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `t_channel` (
    `id` BIGINT NOT NULL COMMENT 'Primary key',
    `name` VARCHAR(100) NOT NULL COMMENT 'Channel name',
    `type` VARCHAR(50) NOT NULL COMMENT 'Channel type (taobao, amazon, shopify, custom)',
    `description` VARCHAR(500) DEFAULT NULL COMMENT 'Channel description',
    `key_prefix` VARCHAR(20) DEFAULT 'sk-' COMMENT 'API key prefix for this channel',
    `price_multiplier` DECIMAL(5,2) DEFAULT 1.00 COMMENT 'Price multiplier for billing',
    `webhook_url` VARCHAR(500) DEFAULT NULL COMMENT 'Webhook URL for order notifications',
    `config` JSON DEFAULT NULL COMMENT 'Channel configuration as JSON',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0=disabled, 1=enabled',
    `total_keys` BIGINT DEFAULT 0 COMMENT 'Total keys generated',
    `active_keys` BIGINT DEFAULT 0 COMMENT 'Active keys count',
    `total_revenue` DECIMAL(15,2) DEFAULT 0.00 COMMENT 'Total revenue',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sales channels';

-- ----------------------------------------
-- Table: t_channel_api_key (Channel API Keys)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `t_channel_api_key` (
    `id` BIGINT NOT NULL COMMENT 'Primary key',
    `channel_id` BIGINT NOT NULL COMMENT 'Channel ID',
    `key_id` VARCHAR(32) NOT NULL COMMENT 'Unique key identifier',
    `key_hash` VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of the API key',
    `key_prefix` VARCHAR(20) NOT NULL COMMENT 'Key prefix for display (e.g., sk-abc...)',
    `name` VARCHAR(100) DEFAULT NULL COMMENT 'Key name',
    `plan_type` VARCHAR(20) NOT NULL DEFAULT 'basic' COMMENT 'Plan type (free, basic, pro, enterprise)',
    `monthly_quota` BIGINT NOT NULL DEFAULT 1000000 COMMENT 'Monthly token quota',
    `used_quota` BIGINT NOT NULL DEFAULT 0 COMMENT 'Used quota this month',
    `rate_limit` INT NOT NULL DEFAULT 60 COMMENT 'Rate limit (requests per minute)',
    `allowed_models` VARCHAR(500) DEFAULT NULL COMMENT 'Comma-separated list of allowed models',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0=disabled, 1=active, 2=expired',
    `price` DECIMAL(10,2) DEFAULT NULL COMMENT 'Price paid for this key',
    `sold_at` DATETIME DEFAULT NULL COMMENT 'When the key was sold',
    `buyer_info` JSON DEFAULT NULL COMMENT 'Buyer information',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'Expiration time (null = never)',
    `last_used_at` DATETIME DEFAULT NULL COMMENT 'Last usage time',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key_id` (`key_id`),
    UNIQUE KEY `uk_key_hash` (`key_hash`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_status` (`status`),
    KEY `idx_plan_type` (`plan_type`),
    KEY `idx_expires_at` (`expires_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Channel API keys';

-- ----------------------------------------
-- Table: t_usage_log (Usage Logs)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `t_usage_log` (
    `id` BIGINT NOT NULL COMMENT 'Primary key',
    `channel_id` BIGINT DEFAULT NULL COMMENT 'Channel ID',
    `key_id` VARCHAR(32) NOT NULL COMMENT 'API key ID',
    `request_id` VARCHAR(64) NOT NULL COMMENT 'Unique request identifier',
    `model` VARCHAR(50) NOT NULL COMMENT 'Requested model',
    `actual_model` VARCHAR(50) DEFAULT NULL COMMENT 'Actual model used (after routing)',
    `provider` VARCHAR(30) DEFAULT NULL COMMENT 'AI provider (openai, anthropic, etc.)',
    `prompt_tokens` INT DEFAULT 0 COMMENT 'Prompt tokens used',
    `completion_tokens` INT DEFAULT 0 COMMENT 'Completion tokens used',
    `total_tokens` INT DEFAULT 0 COMMENT 'Total tokens used',
    `cost` DECIMAL(10,6) DEFAULT 0.000000 COMMENT 'Calculated cost',
    `latency_ms` BIGINT DEFAULT 0 COMMENT 'Request latency in milliseconds',
    `status` VARCHAR(20) NOT NULL COMMENT 'Status: success, error',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT 'Error message if failed',
    `ip_address` VARCHAR(45) DEFAULT NULL COMMENT 'Client IP address',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'Client user agent',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    KEY `idx_key_id` (`key_id`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_model` (`model`),
    KEY `idx_provider` (`provider`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API usage logs';

-- ----------------------------------------
-- Table: t_usage_stats_hourly (Hourly Statistics)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `t_usage_stats_hourly` (
    `id` BIGINT NOT NULL COMMENT 'Primary key',
    `channel_id` BIGINT DEFAULT NULL COMMENT 'Channel ID (null for aggregate)',
    `key_id` VARCHAR(32) DEFAULT NULL COMMENT 'API key ID (null for aggregate)',
    `model` VARCHAR(50) DEFAULT NULL COMMENT 'Model (null for aggregate)',
    `provider` VARCHAR(30) DEFAULT NULL COMMENT 'Provider (null for aggregate)',
    `stat_hour` DATETIME NOT NULL COMMENT 'Statistics hour (truncated to hour)',
    `request_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total request count',
    `success_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Successful requests',
    `error_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Failed requests',
    `prompt_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total prompt tokens',
    `completion_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total completion tokens',
    `total_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total tokens',
    `total_cost` DECIMAL(15,6) NOT NULL DEFAULT 0.000000 COMMENT 'Total cost',
    `total_latency_ms` BIGINT NOT NULL DEFAULT 0 COMMENT 'Sum of latencies',
    `avg_latency_ms` DOUBLE DEFAULT NULL COMMENT 'Average latency',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stats` (`channel_id`, `key_id`, `model`, `provider`, `stat_hour`),
    KEY `idx_stat_hour` (`stat_hour`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_model` (`model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hourly usage statistics';

-- ----------------------------------------
-- Table: t_usage_stats_daily (Daily Statistics)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `t_usage_stats_daily` (
    `id` BIGINT NOT NULL COMMENT 'Primary key',
    `channel_id` BIGINT DEFAULT NULL COMMENT 'Channel ID (null for aggregate)',
    `key_id` VARCHAR(32) DEFAULT NULL COMMENT 'API key ID (null for aggregate)',
    `model` VARCHAR(50) DEFAULT NULL COMMENT 'Model (null for aggregate)',
    `provider` VARCHAR(30) DEFAULT NULL COMMENT 'Provider (null for aggregate)',
    `stat_date` DATE NOT NULL COMMENT 'Statistics date',
    `request_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total request count',
    `success_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Successful requests',
    `error_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Failed requests',
    `prompt_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total prompt tokens',
    `completion_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total completion tokens',
    `total_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total tokens',
    `total_cost` DECIMAL(15,6) NOT NULL DEFAULT 0.000000 COMMENT 'Total cost',
    `avg_latency_ms` DOUBLE DEFAULT NULL COMMENT 'Average latency',
    `unique_keys` BIGINT DEFAULT 0 COMMENT 'Unique keys used',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stats` (`channel_id`, `key_id`, `model`, `provider`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_model` (`model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Daily usage statistics';

-- ----------------------------------------
-- Table: t_ai_model (AI Model Registry)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `t_ai_model` (
    `id` BIGINT NOT NULL COMMENT 'Primary key',
    `model_id` VARCHAR(100) NOT NULL COMMENT 'Model identifier (e.g., gpt-4)',
    `model_name` VARCHAR(100) NOT NULL COMMENT 'Display name',
    `provider` VARCHAR(30) NOT NULL COMMENT 'Provider (openai, anthropic, etc.)',
    `category` VARCHAR(30) DEFAULT 'chat' COMMENT 'Category (chat, embedding, image)',
    `context_window` INT DEFAULT NULL COMMENT 'Context window size',
    `max_output_tokens` INT DEFAULT NULL COMMENT 'Maximum output tokens',
    `input_price` DECIMAL(10,6) DEFAULT NULL COMMENT 'Price per 1K input tokens',
    `output_price` DECIMAL(10,6) DEFAULT NULL COMMENT 'Price per 1K output tokens',
    `supports_vision` TINYINT DEFAULT 0 COMMENT 'Supports vision/images',
    `supports_function_calling` TINYINT DEFAULT 0 COMMENT 'Supports function calling',
    `supports_streaming` TINYINT DEFAULT 1 COMMENT 'Supports streaming',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0=disabled, 1=enabled',
    `sort_order` INT DEFAULT 0 COMMENT 'Display order',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_id` (`model_id`),
    KEY `idx_provider` (`provider`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI model registry';

-- ----------------------------------------
-- Table: t_model_routing (Model Routing Rules)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `t_model_routing` (
    `id` BIGINT NOT NULL COMMENT 'Primary key',
    `source_model` VARCHAR(100) NOT NULL COMMENT 'Source model ID (requested)',
    `target_model` VARCHAR(100) NOT NULL COMMENT 'Target model ID (routed to)',
    `target_provider` VARCHAR(30) NOT NULL COMMENT 'Target provider',
    `priority` INT NOT NULL DEFAULT 0 COMMENT 'Priority (lower = higher priority)',
    `weight` INT DEFAULT 100 COMMENT 'Weight for load balancing',
    `conditions` JSON DEFAULT NULL COMMENT 'Routing conditions as JSON',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0=disabled, 1=enabled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    KEY `idx_source_model` (`source_model`),
    KEY `idx_target_provider` (`target_provider`),
    KEY `idx_priority` (`priority`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Model routing rules';

-- ----------------------------------------
-- Insert default AI models
-- ----------------------------------------
INSERT INTO `t_ai_model` (`id`, `model_id`, `model_name`, `provider`, `category`, `context_window`, `max_output_tokens`, `input_price`, `output_price`, `supports_vision`, `supports_function_calling`, `supports_streaming`, `status`, `sort_order`) VALUES
-- OpenAI models
(1, 'gpt-4o', 'GPT-4o', 'openai', 'chat', 128000, 16384, 0.005000, 0.015000, 1, 1, 1, 1, 1),
(2, 'gpt-4o-mini', 'GPT-4o Mini', 'openai', 'chat', 128000, 16384, 0.000150, 0.000600, 1, 1, 1, 1, 2),
(3, 'gpt-4-turbo', 'GPT-4 Turbo', 'openai', 'chat', 128000, 4096, 0.010000, 0.030000, 1, 1, 1, 1, 3),
(4, 'gpt-4', 'GPT-4', 'openai', 'chat', 8192, 8192, 0.030000, 0.060000, 0, 1, 1, 1, 4),
(5, 'gpt-3.5-turbo', 'GPT-3.5 Turbo', 'openai', 'chat', 16385, 4096, 0.000500, 0.001500, 0, 1, 1, 1, 5),

-- Anthropic models
(10, 'claude-3-opus-20240229', 'Claude 3 Opus', 'anthropic', 'chat', 200000, 4096, 0.015000, 0.075000, 1, 1, 1, 1, 10),
(11, 'claude-3-sonnet-20240229', 'Claude 3 Sonnet', 'anthropic', 'chat', 200000, 4096, 0.003000, 0.015000, 1, 1, 1, 1, 11),
(12, 'claude-3-haiku-20240307', 'Claude 3 Haiku', 'anthropic', 'chat', 200000, 4096, 0.000250, 0.001250, 1, 1, 1, 1, 12),
(13, 'claude-3-5-sonnet-20240620', 'Claude 3.5 Sonnet', 'anthropic', 'chat', 200000, 8192, 0.003000, 0.015000, 1, 1, 1, 1, 13),

-- DeepSeek models
(20, 'deepseek-chat', 'DeepSeek Chat', 'deepseek', 'chat', 128000, 4096, 0.000140, 0.000280, 0, 1, 1, 1, 20),
(21, 'deepseek-coder', 'DeepSeek Coder', 'deepseek', 'chat', 128000, 4096, 0.000140, 0.000280, 0, 1, 1, 1, 21),

-- Model aliases for convenience
(30, 'claude-3-opus', 'Claude 3 Opus (alias)', 'anthropic', 'chat', 200000, 4096, 0.015000, 0.075000, 1, 1, 1, 1, 30),
(31, 'claude-3-sonnet', 'Claude 3 Sonnet (alias)', 'anthropic', 'chat', 200000, 4096, 0.003000, 0.015000, 1, 1, 1, 1, 31),
(32, 'claude-3-haiku', 'Claude 3 Haiku (alias)', 'anthropic', 'chat', 200000, 4096, 0.000250, 0.001250, 1, 1, 1, 1, 32)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- ----------------------------------------
-- Insert default routing rules
-- ----------------------------------------
INSERT INTO `t_model_routing` (`id`, `source_model`, `target_model`, `target_provider`, `priority`, `weight`, `status`) VALUES
-- GPT-4 fallback chain: OpenAI -> Anthropic -> DeepSeek
(1, 'gpt-4', 'gpt-4', 'openai', 1, 100, 1),
(2, 'gpt-4', 'claude-3-opus-20240229', 'anthropic', 2, 100, 1),
(3, 'gpt-4', 'deepseek-chat', 'deepseek', 3, 100, 1),

-- GPT-4o fallback
(4, 'gpt-4o', 'gpt-4o', 'openai', 1, 100, 1),
(5, 'gpt-4o', 'claude-3-5-sonnet-20240620', 'anthropic', 2, 100, 1),

-- Claude fallback: Anthropic -> OpenAI
(10, 'claude-3-opus', 'claude-3-opus-20240229', 'anthropic', 1, 100, 1),
(11, 'claude-3-opus', 'gpt-4', 'openai', 2, 100, 1),

(12, 'claude-3-sonnet', 'claude-3-sonnet-20240229', 'anthropic', 1, 100, 1),
(13, 'claude-3-sonnet', 'gpt-4-turbo', 'openai', 2, 100, 1),

-- DeepSeek fallback
(20, 'deepseek-chat', 'deepseek-chat', 'deepseek', 1, 100, 1),
(21, 'deepseek-chat', 'gpt-3.5-turbo', 'openai', 2, 100, 1)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- ----------------------------------------
-- Create partition for usage_log (optional, for high-volume production)
-- Uncomment if needed
-- ----------------------------------------
-- ALTER TABLE t_usage_log PARTITION BY RANGE (TO_DAYS(created_at)) (
--     PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
--     PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );
