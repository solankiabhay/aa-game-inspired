CREATE TABLE game_configs (
  game_id VARCHAR(64) NOT NULL PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(32) NOT NULL,
  description VARCHAR(500) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  updated_by BIGINT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_game_config_updated_by FOREIGN KEY (updated_by) REFERENCES app_users(id)
);

INSERT INTO game_configs(game_id,title,category,description,enabled) VALUES
  ('skyline-crash','Skyline Crash','ARCADE','Watch the multiplier rise in a simulated round.',TRUE),
  ('neon-reels','Neon Reels','CASUAL','A colourful slot-style demo using non-redeemable credits.',TRUE),
  ('reef-ranger','Reef Ranger','ARCADE','A score-chasing underwater arcade demo.',TRUE);
