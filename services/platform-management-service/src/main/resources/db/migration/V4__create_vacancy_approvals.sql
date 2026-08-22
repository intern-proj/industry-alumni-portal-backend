CREATE TABLE vacancy_approvals (
    id                     UUID PRIMARY KEY,
    vacancy_id             UUID NOT NULL UNIQUE,
    company_user_id        UUID NOT NULL,
    submitted_by_user_id   UUID,
    vacancy_title_snapshot VARCHAR(255),
    company_name_snapshot  VARCHAR(255),
    status                 VARCHAR(32) NOT NULL,
    submitted_at           TIMESTAMP NOT NULL,
    assigned_reviewer_id   UUID,
    reviewed_at            TIMESTAMP,
    reviewed_by_user_id    UUID,
    decision_notes         TEXT,
    rejection_reason       TEXT,
    sync_status            VARCHAR(20) NOT NULL DEFAULT 'SYNCED',
    version                BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_vacancy_approvals_status ON vacancy_approvals (status);
CREATE INDEX idx_vacancy_approvals_sync_status ON vacancy_approvals (sync_status);
