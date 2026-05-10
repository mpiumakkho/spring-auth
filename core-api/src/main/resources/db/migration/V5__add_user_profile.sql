-- User profile fields (avatar, phone, bio)
ALTER TABLE sample_app.users
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS bio TEXT NULL;
