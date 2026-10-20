CREATE TABLE subscription_request (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  username TEXT,
  email TEXT NOT NULL,
  service TEXT NOT NULL,
  note TEXT,
  status TEXT NOT NULL DEFAULT 'pending',
  admin_note TEXT,
  reviewed_by INTEGER,
  reviewed_at INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX idx_subscription_request_user_id ON subscription_request (user_id);
CREATE INDEX idx_subscription_request_status ON subscription_request (status);
