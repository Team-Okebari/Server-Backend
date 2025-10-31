-- V1__create_user_social_logins_table.sql

-- Create user_social_logins table
CREATE TABLE user_social_logins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_social_login_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_provider_provider_id UNIQUE (provider, provider_id)
);

-- Remove provider and provider_id columns from users table
ALTER TABLE users
DROP COLUMN provider,
DROP COLUMN provider_id;
