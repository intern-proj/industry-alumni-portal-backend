ALTER TABLE events
    ADD COLUMN IF NOT EXISTS coordinator_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS coordinator_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS coordinator_email VARCHAR(255);