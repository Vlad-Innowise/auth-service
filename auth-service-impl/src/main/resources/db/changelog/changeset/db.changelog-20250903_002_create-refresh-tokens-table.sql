--liquibase formatted sql

--changeset Vlad:20250903_001_1947

CREATE TABLE app.refresh_tokens
(
id UUID,
hashed_token VARCHAR(64) NOT NULL,
expires_at TIMESTAMP(3) NOT NULL,
user_id BIGINT NOT NULL,
created_at TIMESTAMP(3) NOT NULL,
updated_at TIMESTAMP(3) NOT NULL,
version BIGINT NOT NULL,
CONSTRAINT refresh_tokens_id_pk PRIMARY KEY(id),
CONSTRAINT refresh_tokens_auth_users_fk FOREIGN KEY (user_id) REFERENCES app.auth_users(id),
CONSTRAINT refresh_tokens_hashed_token_unq UNIQUE(hashed_token)
);
