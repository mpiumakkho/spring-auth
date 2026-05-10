-- Initialize database schema
CREATE SCHEMA IF NOT EXISTS sample_app;

-- Set default schema
SET search_path TO sample_app;

-- The tables will be created by Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- This file is just to ensure the schema exists
