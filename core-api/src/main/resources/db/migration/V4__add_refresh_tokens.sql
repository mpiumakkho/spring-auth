-- Refresh tokens for long-lived re-authentication
CREATE TABLE IF NOT EXISTS sample_app.refresh_tokens (
    refresh_id   VARCHAR(255) PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    token        VARCHAR(255) NOT NULL UNIQUE,
    expires_at   TIMESTAMP NOT NULL,
    revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES sample_app.users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON sample_app.refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON sample_app.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON sample_app.refresh_tokens(expires_at);

-- Issuer field on user_sessions so each access token knows its parent refresh
ALTER TABLE sample_app.user_sessions
    ADD COLUMN IF NOT EXISTS refresh_token_id VARCHAR(255) NULL;
