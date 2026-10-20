-- External Order & Auto-Delivery Tables for AI Panel
-- Supports third-party platform sales (Taobao, Xianyu, etc.)

USE ai_panel_user;

-- Platform configuration (淘宝、闲鱼等平台配置)
CREATE TABLE IF NOT EXISTS platform_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform VARCHAR(30) NOT NULL COMMENT 'Platform identifier: taobao, xianyu, pdd, custom',
    name VARCHAR(50) NOT NULL COMMENT 'Display name',
    app_key VARCHAR(100) COMMENT 'Platform APP Key',
    app_secret VARCHAR(200) COMMENT 'Platform APP Secret (encrypted)',
    webhook_secret VARCHAR(100) COMMENT 'Webhook signature secret',
    webhook_url VARCHAR(255) COMMENT 'Generated webhook URL for this platform',
    delivery_template TEXT COMMENT 'Delivery message template',
    auto_create_user TINYINT(1) DEFAULT 1 COMMENT 'Auto create user if not exists',
    auto_delivery TINYINT(1) DEFAULT 1 COMMENT 'Auto deliver on order received',
    enabled TINYINT(1) DEFAULT 1,
    config JSON COMMENT 'Extra configuration as JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Third-party platform configuration';

-- Product SKU to quota mapping (商品SKU与配额映射)
CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform VARCHAR(30) COMMENT 'Platform, null means all platforms',
    sku VARCHAR(100) NOT NULL COMMENT 'Product SKU or ID',
    product_name VARCHAR(100) COMMENT 'Product display name',
    quota_type VARCHAR(20) DEFAULT 'tokens' COMMENT 'Quota type: tokens, days, unlimited',
    quota_tokens BIGINT COMMENT 'Token quota amount',
    validity_days INT DEFAULT 30 COMMENT 'Validity period in days',
    rate_limit_per_minute INT COMMENT 'Rate limit for generated keys',
    price DECIMAL(10,2) COMMENT 'Selling price',
    cost DECIMAL(10,2) COMMENT 'Cost price for margin calculation',
    enabled TINYINT(1) DEFAULT 1,
    description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_sku (platform, sku),
    INDEX idx_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product SKU to quota mapping';

-- External orders (第三方平台订单)
CREATE TABLE IF NOT EXISTS external_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform VARCHAR(30) NOT NULL COMMENT 'Platform: taobao, xianyu, pdd, custom',
    external_order_id VARCHAR(100) NOT NULL COMMENT 'Platform order ID',
    external_item_id VARCHAR(100) COMMENT 'Platform item/product ID',
    sku VARCHAR(100) COMMENT 'Product SKU',
    buyer_nick VARCHAR(100) COMMENT 'Buyer nickname',
    buyer_phone VARCHAR(20) COMMENT 'Buyer phone number',
    buyer_email VARCHAR(100) COMMENT 'Buyer email',
    buyer_message TEXT COMMENT 'Buyer order message',
    quantity INT DEFAULT 1 COMMENT 'Purchase quantity',
    amount DECIMAL(10,2) COMMENT 'Order amount',
    user_id BIGINT COMMENT 'Linked AI Panel user ID',
    api_key_id BIGINT COMMENT 'Generated API Key ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'Status: pending, processing, delivered, failed, cancelled',
    delivery_method VARCHAR(20) COMMENT 'Delivery method: platform_api, sms, email, manual',
    delivery_content TEXT COMMENT 'Delivered content/message',
    delivery_time DATETIME COMMENT 'Delivery timestamp',
    fail_reason VARCHAR(500) COMMENT 'Failure reason if failed',
    retry_count INT DEFAULT 0 COMMENT 'Delivery retry count',
    raw_data JSON COMMENT 'Raw order data from platform',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_order (platform, external_order_id),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='External platform orders';

-- Delivery log (发货日志)
CREATE TABLE IF NOT EXISTS delivery_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT 'External order ID',
    action VARCHAR(30) NOT NULL COMMENT 'Action: create_user, generate_key, send_delivery, retry',
    status VARCHAR(20) NOT NULL COMMENT 'Status: success, failed',
    message TEXT COMMENT 'Log message or error details',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Delivery action logs';

-- Insert default delivery template
INSERT INTO platform_config (platform, name, delivery_template, auto_create_user, auto_delivery, enabled) VALUES
('taobao', 'Taobao/Tmall', '【AI Panel API Key 发货通知】

🎉 感谢您的购买！

📌 您的 API Key：
{api_key}

📌 API 地址：
{api_base}

📌 使用额度：
{quota_tokens} Tokens / {validity_days} 天
有效期至：{expire_date}

📌 快速开始：
curl {api_base}/chat/completions \\
  -H "Authorization: Bearer {api_key}" \\
  -H "Content-Type: application/json" \\
  -d ''{"model":"gpt-4o","messages":[{"role":"user","content":"Hello"}]}''

📌 支持的模型：
GPT-4o, GPT-4-Turbo, Claude-3, DeepSeek 等

📌 使用文档：
{docs_url}

如有问题请联系客服，祝您使用愉快！', 1, 1, 1),

('xianyu', 'Xianyu', '【AI Panel API Key 发货通知】

🎉 感谢您的购买！

📌 API Key：{api_key}
📌 API 地址：{api_base}
📌 额度：{quota_tokens} Tokens / {validity_days} 天
📌 有效期至：{expire_date}
📌 文档：{docs_url}

支持模型：GPT-4o, Claude-3, DeepSeek
如有问题请联系客服！', 1, 1, 1),

('custom', 'Custom Webhook', '【API Key Delivery】

API Key: {api_key}
Base URL: {api_base}
Quota: {quota_tokens} tokens
Valid until: {expire_date}

Documentation: {docs_url}', 1, 1, 1);

-- Insert sample product SKUs
INSERT INTO product_sku (platform, sku, product_name, quota_type, quota_tokens, validity_days, rate_limit_per_minute, price, enabled) VALUES
(NULL, 'API-100K-30D', '10万Tokens/30天', 'tokens', 100000, 30, 60, 9.90, 1),
(NULL, 'API-500K-30D', '50万Tokens/30天', 'tokens', 500000, 30, 100, 39.90, 1),
(NULL, 'API-1M-30D', '100万Tokens/30天', 'tokens', 1000000, 30, 120, 69.90, 1),
(NULL, 'API-5M-30D', '500万Tokens/30天', 'tokens', 5000000, 30, 200, 299.00, 1),
(NULL, 'API-UNLIMITED-30D', '不限量/30天', 'unlimited', NULL, 30, 300, 499.00, 1);
