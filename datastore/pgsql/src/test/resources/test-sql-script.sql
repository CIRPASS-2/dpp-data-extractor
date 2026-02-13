DO $$ DECLARE r RECORD; BEGIN FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE'; END LOOP; END $$;
CREATE SEQUENCE IF NOT EXISTS dpp_metadata_seq;

CREATE TABLE IF NOT EXISTS dpp_metadata (
id BIGINT PRIMARY KEY DEFAULT nextval('dpp_metadata_seq'),
registry_id VARCHAR(36) NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
metadata JSONB NOT NULL
);

INSERT INTO dpp_metadata (registry_id, metadata)
VALUES ('550e8400-e29b-41d4-a716-446655440000', '{"upi":"urn:epc:id:sgtin:0614141.107346.2017","reoId":"LEI-529900T8BM49AURSDO55","liveURL":"http://localhost:8080/dpp1","granularityLevel":"MODEL"}'::jsonb);

INSERT INTO dpp_metadata (registry_id, metadata)
VALUES ('550e8400-e29b-41d4-a716-446655441111', '{"upi":"urn:epc:id:sgtin:0615151.107346.2018","reoId":"LEI-529900T8BM49AURSDO55","liveURL":"http://localhost:8080/dpp2","granularityLevel":"MODEL"}'::jsonb);

INSERT INTO dpp_metadata (registry_id, metadata)
VALUES ('550e8400-e29b-41d4-a716-446655442222', '{"upi":"urn:epc:id:sgtin:0615151.107346.2022","reoId":"LEI-529900T8BM49AURSDO55","liveURL":"http://localhost:8080/dpp3","granularityLevel":"MODEL"}'::jsonb);