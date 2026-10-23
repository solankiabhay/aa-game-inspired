CREATE TABLE IF NOT EXISTS demo_credit_accounts (
  user_id BIGINT NOT NULL PRIMARY KEY,
  balance BIGINT NOT NULL DEFAULT 1000,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_demo_credit_account_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
  CONSTRAINT chk_demo_credit_nonnegative CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS demo_credit_ledger (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  delta BIGINT NOT NULL,
  balance_after BIGINT NOT NULL,
  reason VARCHAR(40) NOT NULL,
  reference_id VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_demo_ledger_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);
CREATE INDEX idx_demo_ledger_user_created ON demo_credit_ledger(user_id, created_at);

CREATE TABLE IF NOT EXISTS game_sessions (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  game_id VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  score BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  CONSTRAINT fk_game_session_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);
CREATE INDEX idx_game_sessions_user_created ON game_sessions(user_id, created_at);
