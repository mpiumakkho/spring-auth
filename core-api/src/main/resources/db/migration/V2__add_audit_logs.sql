CREATE TABLE IF NOT EXISTS sample_app.audit_logs (
    audit_id      VARCHAR(255) PRIMARY KEY,
    actor         VARCHAR(255),
    action        VARCHAR(50) NOT NULL,
    target_type   VARCHAR(50) NOT NULL,
    target_id     VARCHAR(255),
    detail        TEXT,
    ip_address    VARCHAR(50),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON sample_app.audit_logs(actor);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON sample_app.audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target_type ON sample_app.audit_logs(target_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON sample_app.audit_logs(created_at);
