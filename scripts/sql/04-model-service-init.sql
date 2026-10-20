-- AI Panel Model Service Database Schema
USE ai_panel_model;

-- Model table (can reference dispatch t_model_info or be standalone)
CREATE TABLE IF NOT EXISTS t_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    provider_code VARCHAR(50) NOT NULL,
    model_type VARCHAR(50) NOT NULL,
    context_window INT,
    max_output_tokens INT,
    input_price DECIMAL(12, 6),
    output_price DECIMAL(12, 6),
    features JSON,
    fallback_chain JSON,
    enabled TINYINT DEFAULT 1,
    deprecated TINYINT DEFAULT 0,
    description TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_model_id (model_id),
    INDEX idx_provider_code (provider_code),
    INDEX idx_model_type (model_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Provider table
CREATE TABLE IF NOT EXISTS t_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code VARCHAR(50) NOT NULL UNIQUE,
    provider_name VARCHAR(100) NOT NULL,
    base_url VARCHAR(500),
    api_key VARCHAR(500),
    priority INT DEFAULT 100,
    rate_limit_per_minute INT DEFAULT 60,
    timeout INT DEFAULT 60000,
    enabled TINYINT DEFAULT 1,
    health_status TINYINT DEFAULT 0,
    last_health_check DATETIME,
    config JSON,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_provider_code (provider_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Model capability mapping
CREATE TABLE IF NOT EXISTS t_model_capability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id VARCHAR(100) NOT NULL,
    capability VARCHAR(50) NOT NULL COMMENT 'chat, embedding, vision, function_calling, etc.',
    enabled TINYINT DEFAULT 1,
    config JSON,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_capability (model_id, capability),
    INDEX idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
