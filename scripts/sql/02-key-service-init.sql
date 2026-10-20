-- AI Panel Key Service Database Schema
USE ai_panel_key;

-- API Key table
CREATE TABLE IF NOT EXISTS t_api_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    key_name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(10) NOT NULL COMMENT 'First 8 chars for identification',
    key_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of full key',
    key_suffix VARCHAR(4) NOT NULL COMMENT 'Last 4 chars for display',
    description VARCHAR(500),
    scopes VARCHAR(500) DEFAULT '*' COMMENT 'Comma-separated scopes',
    status TINYINT DEFAULT 1 COMMENT '0=disabled, 1=active, 2=expired',
    rate_limit_per_minute INT,
    rate_limit_per_day INT,
    monthly_quota BIGINT,
    used_quota BIGINT DEFAULT 0,
    expires_at DATETIME,
    last_used_at DATETIME,
    last_used_ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_key_hash (key_hash),
    INDEX idx_user_id (user_id),
    INDEX idx_key_prefix (key_prefix),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- API Key usage log table (partitioned by month)
CREATE TABLE IF NOT EXISTS t_api_key_usage_log (
    id BIGINT AUTO_INCREMENT,
    api_key_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    request_id VARCHAR(64),
    model VARCHAR(100),
    tokens_used INT DEFAULT 0,
    request_time DATETIME NOT NULL,
    client_ip VARCHAR(50),
    user_agent VARCHAR(500),
    PRIMARY KEY (id, request_time),
    INDEX idx_api_key_id (api_key_id),
    INDEX idx_user_id (user_id),
    INDEX idx_request_time (request_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (TO_DAYS(request_time)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202507 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
