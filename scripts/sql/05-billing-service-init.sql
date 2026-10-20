-- AI Panel Billing Service Database Schema
USE ai_panel_billing;

-- User Balance table
CREATE TABLE IF NOT EXISTS t_user_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(12, 6) DEFAULT 0.000000,
    total_charged DECIMAL(12, 6) DEFAULT 0.000000,
    total_used DECIMAL(12, 6) DEFAULT 0.000000,
    version INT DEFAULT 0 COMMENT 'Optimistic lock version',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Balance Transaction table
CREATE TABLE IF NOT EXISTS t_balance_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL COMMENT 'CHARGE, DEDUCT, REFUND',
    amount DECIMAL(12, 6) NOT NULL,
    balance_after DECIMAL(12, 6) NOT NULL,
    reference_id VARCHAR(100),
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Usage Record table (main billing table, high-write)
CREATE TABLE IF NOT EXISTS t_usage_record (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    api_key_id BIGINT,
    request_id VARCHAR(64) NOT NULL,
    model VARCHAR(100) NOT NULL,
    provider VARCHAR(50),
    input_tokens INT DEFAULT 0,
    output_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    input_cost DECIMAL(12, 6) DEFAULT 0.000000,
    output_cost DECIMAL(12, 6) DEFAULT 0.000000,
    total_cost DECIMAL(12, 6) DEFAULT 0.000000,
    latency_ms BIGINT,
    status VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, create_time),
    INDEX idx_user_id (user_id),
    INDEX idx_api_key_id (api_key_id),
    INDEX idx_request_id (request_id),
    INDEX idx_model (model),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- Daily Usage Summary table (aggregated for reporting)
CREATE TABLE IF NOT EXISTS t_daily_usage_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    model VARCHAR(100),
    request_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    error_count INT DEFAULT 0,
    total_input_tokens BIGINT DEFAULT 0,
    total_output_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    total_cost DECIMAL(12, 6) DEFAULT 0.000000,
    avg_latency_ms BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date_model (user_id, date, model),
    INDEX idx_user_id (user_id),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Monthly Invoice table
CREATE TABLE IF NOT EXISTS t_monthly_invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    `year_month` VARCHAR(7) NOT NULL COMMENT 'YYYY-MM',
    total_requests BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    total_cost DECIMAL(12, 6) DEFAULT 0.000000,
    invoice_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, PAID, OVERDUE',
    paid_at DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, `year_month`),
    INDEX idx_user_id (user_id),
    INDEX idx_year_month (`year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
