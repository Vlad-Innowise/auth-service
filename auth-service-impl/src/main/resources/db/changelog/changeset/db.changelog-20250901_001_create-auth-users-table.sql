--liquibase formatted sql

--changeset Vlad:20250901_001_1900

CREATE TABLE app.auth_users
(
id BIGSERIAL,
email VARCHAR(255) NOT NULL,
password VARCHAR(128) NOT NULL,
role VARCHAR(32) NOT NULL,
status VARCHAR(32) NOT NULL,
created_at TIMESTAMP(3) NOT NULL,
updated_at TIMESTAMP(3) NOT NULL,
version BIGINT NOT NULL,
CONSTRAINT auth_user_id_pk PRIMARY KEY(id)
);
