CREATE TABLE IF NOT EXISTS approval_status_history (
    id                  UUID PRIMARY KEY,
    approval_type        VARCHAR(32) NOT NULL,
    approval_id           UUID NOT NULL,
    from_status           VARCHAR(40),
    to_status              VARCHAR(40) NOT NULL,
    changed_by_user_id     UUID,
    changed_at             TIMESTAMP NOT NULL,
    remarks                TEXT
);

CREATE INDEX IF NOT EXISTS idx_approval_status_history_lookup ON approval_status_history (approval_type, approval_id);