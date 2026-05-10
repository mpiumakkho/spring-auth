-- ABAC: per-user attributes (department, location, level, ...)
CREATE TABLE IF NOT EXISTS sample_app.user_attributes (
    attribute_id  VARCHAR(255) PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    attr_key      VARCHAR(100) NOT NULL,
    attr_value    VARCHAR(500),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    CONSTRAINT fk_user_attributes_user FOREIGN KEY (user_id) REFERENCES sample_app.users(user_id) ON DELETE CASCADE,
    CONSTRAINT uk_user_attributes_user_key UNIQUE (user_id, attr_key)
);

CREATE INDEX IF NOT EXISTS idx_user_attributes_user_id ON sample_app.user_attributes(user_id);
CREATE INDEX IF NOT EXISTS idx_user_attributes_key     ON sample_app.user_attributes(attr_key);
