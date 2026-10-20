CREATE TABLE t_model (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  model_id TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL,
  provider_code TEXT NOT NULL,
  model_type TEXT NOT NULL,
  context_window INTEGER,
  max_output_tokens INTEGER,
  input_price REAL NOT NULL,
  output_price REAL NOT NULL,
  features TEXT NOT NULL DEFAULT '[]',
  fallback_chain TEXT NOT NULL DEFAULT '[]',
  enabled INTEGER NOT NULL DEFAULT 1,
  deprecated INTEGER NOT NULL DEFAULT 0,
  description TEXT,
  create_time INTEGER NOT NULL,
  update_time INTEGER NOT NULL,
  deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_model_provider_code ON t_model (provider_code);
CREATE INDEX idx_model_type ON t_model (model_type);

CREATE TABLE t_provider (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  provider_code TEXT NOT NULL UNIQUE,
  provider_name TEXT NOT NULL,
  base_url TEXT NOT NULL,
  api_key TEXT,
  priority INTEGER NOT NULL DEFAULT 0,
  rate_limit_per_minute INTEGER,
  timeout INTEGER,
  enabled INTEGER NOT NULL DEFAULT 1,
  health_status INTEGER NOT NULL DEFAULT 0,
  last_health_check INTEGER,
  config TEXT,
  create_time INTEGER NOT NULL,
  update_time INTEGER NOT NULL,
  deleted INTEGER NOT NULL DEFAULT 0
);
