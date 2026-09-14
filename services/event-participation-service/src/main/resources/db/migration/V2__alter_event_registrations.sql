ALTER TABLE event_registrations 
    ALTER COLUMN event_id TYPE VARCHAR(255) USING event_id::text,
    ALTER COLUMN student_id TYPE VARCHAR(255) USING student_id::text;

ALTER TABLE event_registrations
    ADD COLUMN IF NOT EXISTS event_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS venue_name VARCHAR(255);

ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS event_registrations_status_check;
ALTER TABLE event_registrations ADD CONSTRAINT event_registrations_status_check 
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REGISTERED'));

