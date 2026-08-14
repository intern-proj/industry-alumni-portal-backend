CREATE TABLE event_registrations (
    registration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    registered_at TIMESTAMP NOT NULL DEFAULT now()
);