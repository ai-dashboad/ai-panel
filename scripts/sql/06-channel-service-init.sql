-- ============================================
-- AI Panel - Channel service tables (ai_panel_channel)
-- Runs at MySQL container init, before V003__channel_provider_config.sql.
-- Same definitions as migrations/V2__channel_analytics_schema.sql
-- (channel, channel API keys, usage log) so a fresh `docker compose up`
-- gives ai-panel-channel a working schema.
-- ============================================

USE ai_panel_channel;

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
