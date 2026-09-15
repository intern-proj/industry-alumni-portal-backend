CREATE TABLE IF NOT EXISTS partner_documents (
    id                       UUID PRIMARY KEY,
    partner_verification_id UUID NOT NULL REFERENCES partner_verifications (id) ON DELETE CASCADE,
    document_type            VARCHAR(40) NOT NULL,
    storage_file_id          UUID NOT NULL,
    original_filename        VARCHAR(255),
    content_type             VARCHAR(100),
    size_bytes                BIGINT,
    uploaded_at               TIMESTAMP NOT NULL,
    is_verified               BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_partner_documents_verification_id ON partner_documents (partner_verification_id);