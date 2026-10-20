-- Adds a role column to t_user so that /api/v1/admin/** can be restricted to admins.
-- Fresh installs get this from scripts/sql/01-user-service-init.sql; run this only
-- against databases created before the column existed.
USE ai_panel_user;

ALTER TABLE t_user
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'
        COMMENT 'USER or ADMIN; ADMIN may call /api/v1/admin/**' AFTER password_hash;

-- Promote the seeded admin account. Adjust to your own admin username(s) as needed.
UPDATE t_user SET role = 'ADMIN' WHERE username = 'admin';
