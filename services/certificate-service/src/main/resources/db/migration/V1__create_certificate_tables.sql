CREATE TABLE IF NOT EXISTS certificate_templates (
    id UUID PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL UNIQUE,
    template_file_path VARCHAR(500),
    fields_config TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certificates (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    event_id UUID NOT NULL,
    template_id UUID NOT NULL,
    verification_code VARCHAR(100) NOT NULL UNIQUE,
    pdf_file_path VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    issued_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_certificates_template FOREIGN KEY (template_id) REFERENCES certificate_templates(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS certificate_verification_logs (
    id UUID PRIMARY KEY,
    certificate_id UUID NOT NULL,
    verified_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(100),
    CONSTRAINT fk_verification_certificate FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_certificates_student_id ON certificates(student_id);
CREATE INDEX IF NOT EXISTS idx_certificates_event_id ON certificates(event_id);
CREATE INDEX IF NOT EXISTS idx_certificates_verification_code ON certificates(verification_code);
