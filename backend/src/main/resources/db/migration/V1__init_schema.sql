-- Baseline schema for Urlbit
-- Tables: users, short_urls, guest_quotas, refresh_tokens, url_click_daily

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'TENANT', 'ADMIN'))
);

CREATE TABLE short_urls (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id             UUID REFERENCES users(id),
    code                 VARCHAR(32)  NOT NULL,
    original_url         TEXT         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at           TIMESTAMPTZ,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    disabled_reason      VARCHAR(40),
    last_safety_check_at TIMESTAMPTZ,

    CONSTRAINT uq_short_urls_code UNIQUE (code),
    CONSTRAINT ck_short_urls_status CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    CONSTRAINT ck_short_urls_disabled_reason CHECK (
        disabled_reason IS NULL
        OR disabled_reason IN ('OWNER', 'ABUSE_TAKEDOWN')
    )
);

CREATE INDEX idx_short_urls_owner_created
    ON short_urls (owner_id, created_at DESC, id DESC);

CREATE INDEX idx_short_urls_expires_at
    ON short_urls (expires_at)
    WHERE expires_at IS NOT NULL;

CREATE TABLE guest_quotas (
    guest_id_hash        VARCHAR(128) PRIMARY KEY,
    successful_creations SMALLINT     NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_guest_quotas_creations CHECK (successful_creations BETWEEN 0 AND 5)
);

CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id),
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    family_id  UUID        NOT NULL,

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE TABLE url_click_daily (
    short_url_id BIGINT NOT NULL REFERENCES short_urls(id),
    day          DATE   NOT NULL,
    click_count  BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (short_url_id, day)
);
