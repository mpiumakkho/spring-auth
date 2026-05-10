-- External OAuth provider links to internal user accounts
CREATE TABLE IF NOT EXISTS sample_app.user_oauth_links (
    link_id      VARCHAR(255) PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    provider     VARCHAR(50) NOT NULL,
    provider_uid VARCHAR(255) NOT NULL,
    email        VARCHAR(255),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_oauth_links_user FOREIGN KEY (user_id) REFERENCES sample_app.users(user_id),
    CONSTRAINT uk_user_oauth_links_provider_uid UNIQUE (provider, provider_uid)
);

CREATE INDEX IF NOT EXISTS idx_user_oauth_links_user_id ON sample_app.user_oauth_links(user_id);
CREATE INDEX IF NOT EXISTS idx_user_oauth_links_provider ON sample_app.user_oauth_links(provider);
