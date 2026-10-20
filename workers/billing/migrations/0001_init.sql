CREATE TABLE t_usage_record (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  api_key_id INTEGER,
  request_id TEXT NOT NULL,
  model TEXT NOT NULL,
  provider TEXT NOT NULL,
  input_tokens INTEGER NOT NULL,
  output_tokens INTEGER NOT NULL,
  total_tokens INTEGER NOT NULL,
  input_cost REAL NOT NULL,
  output_cost REAL NOT NULL,
  total_cost REAL NOT NULL,
  latency_ms INTEGER,
  status TEXT NOT NULL,
  create_time INTEGER NOT NULL
);

CREATE INDEX idx_usage_record_user_id ON t_usage_record (user_id);
CREATE INDEX idx_usage_record_create_time ON t_usage_record (create_time);

CREATE TABLE t_user_balance (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL UNIQUE,
  balance REAL NOT NULL DEFAULT 0,
  total_charged REAL NOT NULL DEFAULT 0,
  total_used REAL NOT NULL DEFAULT 0,
  version INTEGER NOT NULL DEFAULT 0,
  create_time INTEGER NOT NULL,
  update_time INTEGER NOT NULL
);

CREATE TABLE t_balance_transaction (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  transaction_type TEXT NOT NULL,
  amount REAL NOT NULL,
  balance_after REAL NOT NULL,
  reference_id TEXT,
  description TEXT,
  create_time INTEGER NOT NULL
);
