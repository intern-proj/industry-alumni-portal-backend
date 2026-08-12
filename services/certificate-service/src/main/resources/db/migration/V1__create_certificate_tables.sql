CREATE TABLE IF NOT EXISTS certificate_templates (
    id UUID PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    template_file_path VARCHAR(500),
    fields_config TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certificates (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    event_id UUID NOT NULL,
    template_id UUID NOT NULL,
    pdf_file_path VARCHAR(500),
    verification_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_certificates_template FOREIGN KEY (template_id) REFERENCES certificate_templates(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS certificate_verification_logs (
    id UUID PRIMARY KEY,
    certificate_id UUID NOT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    CONSTRAINT fk_logs_certificate FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE
);