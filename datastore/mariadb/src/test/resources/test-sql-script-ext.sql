SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS extraction_registry;

DROP TABLE IF EXISTS extraction_failures;

DROP TABLE IF EXISTS dpp_data;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS extraction_registry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  processed_until TIMESTAMP
);

CREATE TABLE IF NOT EXISTS extraction_failures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registry_id VARCHAR(36) UNIQUE NOT NULL,
    retrials INT NOT NULL DEFAULT(0)
);

CREATE TABLE IF NOT EXISTS dpp_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    upi VARCHAR(36) UNIQUE NOT NULL,
    live_url VARCHAR(1000),
    search_data JSON NOT NULL
);

CREATE TABLE IF NOT EXISTS json_configs (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            data_config JSON NOT NULL
);
INSERT INTO extraction_failures(registry_id,retrials) VALUES('550e8400-e29b-41d4-a716-446655442222',0);
