CREATE TABLE organizations (
                               id BIGSERIAL PRIMARY KEY,
                               name VARCHAR(255) NOT NULL,
                               industry VARCHAR(255),
                               website VARCHAR(255),
                               contact_email VARCHAR(255),
                               address VARCHAR(500),
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE venues (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        address VARCHAR(500),
                        capacity INTEGER,
                        venue_type VARCHAR(20) NOT NULL,
                        contact_info VARCHAR(255),
                        online_meeting_link VARCHAR(500),
                        created_at TIMESTAMP NOT NULL DEFAULT now(),
                        updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE guest_speakers (
                                id BIGSERIAL PRIMARY KEY,
                                full_name VARCHAR(255) NOT NULL,
                                title VARCHAR(255),
                                bio TEXT,
                                email VARCHAR(255),
                                phone VARCHAR(50),
                                photo_url VARCHAR(500),
                                organization_id BIGINT REFERENCES organizations(id),
                                created_at TIMESTAMP NOT NULL DEFAULT now(),
                                updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE events (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        event_type VARCHAR(100),
                        status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                        start_date_time TIMESTAMP NOT NULL,
                        end_date_time TIMESTAMP,
                        venue_id BIGINT REFERENCES venues(id),
                        organization_id BIGINT REFERENCES organizations(id),
                        created_at TIMESTAMP NOT NULL DEFAULT now(),
                        updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE agendas (
                         id BIGSERIAL PRIMARY KEY,
                         event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         speaker_id BIGINT REFERENCES guest_speakers(id),
                         start_time TIMESTAMP NOT NULL,
                         end_time TIMESTAMP,
                         sequence_order INTEGER,
                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE event_speakers (
                                id BIGSERIAL PRIMARY KEY,
                                event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                speaker_id BIGINT NOT NULL REFERENCES guest_speakers(id) ON DELETE CASCADE,
                                role VARCHAR(20) NOT NULL
);

CREATE TABLE certificate_eligibility_criteria (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  event_id BIGINT NOT NULL UNIQUE REFERENCES events(id) ON DELETE CASCADE,
                                                  min_attendance_percentage INTEGER,
                                                  requires_feedback_submission BOOLEAN NOT NULL DEFAULT false,
                                                  min_sessions_attended INTEGER,
                                                  other_criteria_notes TEXT,
                                                  created_at TIMESTAMP NOT NULL DEFAULT now(),
                                                  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_start_date ON events(start_date_time);
CREATE INDEX idx_agendas_event_id ON agendas(event_id);
CREATE INDEX idx_event_speakers_event_id ON event_speakers(event_id);