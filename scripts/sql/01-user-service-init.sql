-- AI Panel User Service Database Schema
USE ai_panel_user;

-- User table (columns match com.aipanel.user.data.mysql.po.UserPO)
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER or ADMIN; ADMIN may call /api/v1/admin/**',
    status TINYINT DEFAULT 1 COMMENT '0=disabled, 1=active, 2=suspended',
    timezone VARCHAR(50) DEFAULT 'UTC',
    language VARCHAR(10) DEFAULT 'en',
    last_login_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User plan table
CREATE TABLE IF NOT EXISTS t_user_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_type VARCHAR(20) NOT NULL COMMENT 'FREE, BASIC, PRO, ENTERPRISE',
    monthly_quota BIGINT DEFAULT 0 COMMENT 'Monthly token quota',
    used_quota BIGINT DEFAULT 0 COMMENT 'Used tokens this month',
    rate_limit_per_minute INT DEFAULT 60,
    rate_limit_per_day INT DEFAULT 10000,
    max_api_keys INT DEFAULT 5,
    start_date DATE,
    end_date DATE,
    auto_renew TINYINT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_plan_type (plan_type),
    INDEX idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default admin user.
-- Default credentials: admin / admin123  (bcrypt, cost 10)
-- SECURITY: this password is public. Change it immediately after first login,
-- or replace this hash before deploying anywhere reachable from the network.
INSERT INTO t_user (username, email, password_hash, display_name, role, status)
VALUES ('admin', 'admin@aipanel.com', '$2a$10$osspWes8LbHxo1UePWJKf.BVWKgw.zDAz6dn4CmvR9NPs36xgv/Ii', 'Administrator', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE role = 'ADMIN';

-- Insert default plan for admin
INSERT INTO t_user_plan (user_id, plan_type, monthly_quota, rate_limit_per_minute, rate_limit_per_day, max_api_keys)
SELECT id, 'ENTERPRISE', 999999999, 1000, 1000000, 100 FROM t_user WHERE username = 'admin'
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
