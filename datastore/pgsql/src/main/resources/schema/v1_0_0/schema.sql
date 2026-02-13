CREATE TABLE IF NOT EXISTS extraction_registry (
    id BIGSERIAL PRIMARY KEY,
    processed_until TIMESTAMP
);

CREATE TABLE IF NOT EXISTS extraction_failures (
    id BIGSERIAL PRIMARY KEY,
    registry_id VARCHAR(36) UNIQUE NOT NULL,
    retrials INTEGER NOT NULL DEFAULT(0)
);

CREATE TABLE IF NOT EXISTS dpp_data (
    id BIGSERIAL PRIMARY KEY,
    upi VARCHAR(36) UNIQUE NOT NULL,
    live_url VARCHAR(1000),
    search_data JSONB NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS json_configs_seq;

CREATE TABLE IF NOT EXISTS json_configs (id BIGINT PRIMARY KEY DEFAULT nextval('json_configs_seq'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_config JSONB NOT NULL
);
