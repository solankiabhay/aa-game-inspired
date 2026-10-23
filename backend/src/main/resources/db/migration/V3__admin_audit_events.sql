CREATE TABLE IF NOT EXISTS audit_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  actor_user_id BIGINT NOT NULL,
  action VARCHAR(80) NOT NULL,
  target_type VARCHAR(40) NOT NULL,
  target_id VARCHAR(80) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  before_value VARCHAR(500),
  after_value VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_users(id)
);
CREATE INDEX idx_audit_created ON audit_events(created_at);
CREATE INDEX idx_audit_target ON audit_events(target_type,target_id);
