SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS dpp_metadata;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS dpp_metadata (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            registry_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    metadata JSON NOT NULL
    );

INSERT INTO dpp_metadata (registry_id, metadata)
VALUES ('550e8400-e29b-41d4-a716-446655440000', '{"upi":"urn:epc:id:sgtin:0614141.107346.2017","reoId":"LEI-529900T8BM49AURSDO55","liveURL":"http://localhost:8080/dpp1","granularityLevel":"MODEL"}');
INSERT INTO dpp_metadata (registry_id, metadata)
VALUES ('550e8400-e29b-41d4-a716-446655441111', '{"upi":"urn:epc:id:sgtin:0614242.107346.2018","reoId":"LEI-529900T8BM49AURSDO55","liveURL":"http://localhost:8080/dpp2","granularityLevel":"MODEL"}');

INSERT INTO dpp_metadata (registry_id, metadata)
VALUES ('550e8400-e29b-41d4-a716-446655442222', '{"upi":"urn:epc:id:sgtin:0614242.107346.2022","reoId":"LEI-529900T8BM49AURSDO55","liveURL":"http://localhost:8080/dpp3","granularityLevel":"MODEL"}');
