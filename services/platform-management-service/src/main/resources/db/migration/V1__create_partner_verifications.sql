CREATE TABLE partner_verifications (
    id                          UUID PRIMARY KEY,
    user_id                     UUID NOT NULL UNIQUE,
    organization_name_snapshot VARCHAR(255),
    contact_email_snapshot     VARCHAR(255),
    status                      VARCHAR(32) NOT NULL,
    submitted_at                TIMESTAMP NOT NULL,
    reviewed_at                 TIMESTAMP,
    reviewed_by_user_id         UUID,
    decision_notes               TEXT,
    rejection_reason            TEXT,
    sync_status                  VARCHAR(20) NOT NULL DEFAULT 'SYNCED',
    version                      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_partner_verifications_status ON partner_verifications (status);
CREATE INDEX idx_partner_verifications_sync_status ON partner_verifications (sync_status);