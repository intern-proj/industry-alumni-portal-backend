ALTER TABLE events
    ADD COLUMN coordinator_user_id BIGINT,
    ADD COLUMN coordinator_name VARCHAR(255),
    ADD COLUMN coordinator_email VARCHAR(255);