ALTER TABLE users ADD COLUMN previous_password_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP;
UPDATE users SET password_changed_at = CURRENT_TIMESTAMP WHERE password_changed_at IS NULL;
ALTER TABLE users ALTER COLUMN password_changed_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ALTER COLUMN password_changed_at SET NOT NULL;
