ALTER TABLE app_users
  ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE auth_action_tokens (
  token_hash CHAR(64) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  purpose VARCHAR(24) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_auth_action_tokens_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_action_tokens_user_purpose ON auth_action_tokens(user_id, purpose);
CREATE INDEX idx_auth_action_tokens_expiry ON auth_action_tokens(expires_at);
