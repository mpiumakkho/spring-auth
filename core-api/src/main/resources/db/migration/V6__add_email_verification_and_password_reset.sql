-- Email verification tokens
CREATE TABLE IF NOT EXISTS sample_app.email_verifications (
    verification_id VARCHAR(255) PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMP NOT NULL,
    consumed        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verifications_user FOREIGN KEY (user_id) REFERENCES sample_app.users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_email_verifications_token ON sample_app.email_verifications(token);
CREATE INDEX IF NOT EXISTS idx_email_verifications_user_id ON sample_app.email_verifications(user_id);

-- Password reset tokens
CREATE TABLE IF NOT EXISTS sample_app.password_resets (
    reset_id    VARCHAR(255) PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    consumed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_resets_user FOREIGN KEY (user_id) REFERENCES sample_app.users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_password_resets_token ON sample_app.password_resets(token);
CREATE INDEX IF NOT EXISTS idx_password_resets_user_id ON sample_app.password_resets(user_id);
