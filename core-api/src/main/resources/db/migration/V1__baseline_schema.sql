-- Baseline schema for UMS (User Management System)
-- Schema: sample_app

CREATE SCHEMA IF NOT EXISTS sample_app;

-- Users table
CREATE TABLE IF NOT EXISTS sample_app.users (
    user_id       VARCHAR(255) PRIMARY KEY,
    username      VARCHAR(255),
    password      VARCHAR(255),
    email         VARCHAR(255),
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    status        VARCHAR(255) DEFAULT 'inactive',
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

-- Roles table
CREATE TABLE IF NOT EXISTS sample_app.roles (
    role_id       VARCHAR(255) PRIMARY KEY,
    name          VARCHAR(255),
    description   VARCHAR(255),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

-- Permissions table
CREATE TABLE IF NOT EXISTS sample_app.permissions (
    permission_id VARCHAR(255) PRIMARY KEY,
    name          VARCHAR(255),
    description   VARCHAR(255),
    resource      VARCHAR(255),
    action        VARCHAR(255),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

-- User sessions table
CREATE TABLE IF NOT EXISTS sample_app.user_sessions (
    session_id      VARCHAR(255) PRIMARY KEY,
    user_id         VARCHAR(255),
    token           VARCHAR(255),
    status          VARCHAR(255) NOT NULL DEFAULT 'active',
    created_at      TIMESTAMP,
    last_activity_at TIMESTAMP
);

-- Join table: user_roles
CREATE TABLE IF NOT EXISTS sample_app.user_roles (
    user_id VARCHAR(255) NOT NULL,
    role_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES sample_app.users(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES sample_app.roles(role_id)
);

-- Join table: role_permissions
CREATE TABLE IF NOT EXISTS sample_app.role_permissions (
    role_id       VARCHAR(255) NOT NULL,
    permission_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES sample_app.roles(role_id),
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES sample_app.permissions(permission_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON sample_app.users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON sample_app.users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON sample_app.users(status);
CREATE INDEX IF NOT EXISTS idx_user_sessions_token ON sample_app.user_sessions(token);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON sample_app.user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_status ON sample_app.user_sessions(status);
CREATE INDEX IF NOT EXISTS idx_roles_name ON sample_app.roles(name);
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON sample_app.permissions(resource);
CREATE INDEX IF NOT EXISTS idx_permissions_resource_action ON sample_app.permissions(resource, action);