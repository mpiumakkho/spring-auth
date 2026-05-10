-- In-app notifications
CREATE TABLE IF NOT EXISTS sample_app.notifications (
    notification_id VARCHAR(255) PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    read_at         TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES sample_app.users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON sample_app.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read_at ON sample_app.notifications(read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON sample_app.notifications(created_at);
