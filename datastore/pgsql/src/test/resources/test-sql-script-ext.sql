DO $$ DECLARE r RECORD; BEGIN FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE'; END LOOP; END $$;

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

INSERT INTO extraction_failures(registry_id,retrials) VALUES('550e8400-e29b-41d4-a716-446655442222',0);
