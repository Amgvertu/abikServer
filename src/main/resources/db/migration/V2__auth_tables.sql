-- V2__auth_tables.sql
-- Таблицы модуля auth: пользователи, согласия, история телефонов, SMS-коды, refresh-токены.

-- ============================================================
-- users — основная таблица пользователей
-- ============================================================
CREATE TABLE users (
                       id              BIGSERIAL       PRIMARY KEY,
                       phone           VARCHAR(20)     NOT NULL,
                       password_hash   VARCHAR(255)    NOT NULL,
                       status          VARCHAR(20)     NOT NULL,
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       deleted_at      TIMESTAMPTZ,

                       CONSTRAINT uq_users_phone UNIQUE (phone),
                       CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED', 'DELETED'))
);

CREATE INDEX idx_users_phone ON users (phone);
CREATE INDEX idx_users_status ON users (status);

COMMENT ON TABLE users IS 'Пользователи (аккаунты)';
COMMENT ON COLUMN users.phone IS 'Телефон в формате E.164 (например +79991234567)';
COMMENT ON COLUMN users.status IS 'PENDING — не подтверждён SMS, ACTIVE — активен, BLOCKED — заблокирован, DELETED — удалён';


-- ============================================================
-- user_consents — согласия на обработку персональных данных
-- ============================================================
CREATE TABLE user_consents (
                               id              BIGSERIAL       PRIMARY KEY,
                               user_id         BIGINT          NOT NULL,
                               policy_version  VARCHAR(50)     NOT NULL,
                               accepted_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                               ip_address      VARCHAR(45),

                               CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_consents_user ON user_consents (user_id);

COMMENT ON TABLE user_consents IS 'История согласий на обработку ПД (152-ФЗ)';
COMMENT ON COLUMN user_consents.policy_version IS 'Версия политики, с которой согласился пользователь';


-- ============================================================
-- phone_history — история смены номеров
-- ============================================================
CREATE TABLE phone_history (
                               id              BIGSERIAL       PRIMARY KEY,
                               user_id         BIGINT          NOT NULL,
                               old_phone       VARCHAR(20),
                               new_phone       VARCHAR(20)     NOT NULL,
                               changed_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                               CONSTRAINT fk_phone_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_phone_history_user ON phone_history (user_id);

COMMENT ON TABLE phone_history IS 'История изменения телефонных номеров';


-- ============================================================
-- sms_codes — коды подтверждения, отправленные по SMS
-- ============================================================
CREATE TABLE sms_codes (
                           id              BIGSERIAL       PRIMARY KEY,
                           phone           VARCHAR(20)     NOT NULL,
                           code            VARCHAR(10)     NOT NULL,
                           purpose         VARCHAR(30)     NOT NULL,
                           expires_at      TIMESTAMPTZ     NOT NULL,
                           used            BOOLEAN         NOT NULL DEFAULT FALSE,
                           attempts        SMALLINT        NOT NULL DEFAULT 0,
                           created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                           CONSTRAINT ck_sms_codes_purpose CHECK (purpose IN ('REGISTRATION', 'PASSWORD_RESET', 'PHONE_CHANGE_OLD', 'PHONE_CHANGE_NEW'))
);

CREATE INDEX idx_sms_codes_phone ON sms_codes (phone);
CREATE INDEX idx_sms_codes_phone_purpose ON sms_codes (phone, purpose);

COMMENT ON TABLE sms_codes IS 'Отправленные SMS-коды подтверждения';
COMMENT ON COLUMN sms_codes.purpose IS 'Назначение кода: регистрация, восстановление пароля, смена номера';


-- ============================================================
-- refresh_tokens — refresh-токены с поддержкой ротации
-- ============================================================
CREATE TABLE refresh_tokens (
                                id              BIGSERIAL       PRIMARY KEY,
                                user_id         BIGINT          NOT NULL,
                                token_hash      VARCHAR(255)    NOT NULL,
                                expires_at      TIMESTAMPTZ     NOT NULL,
                                revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
                                created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                revoked_at      TIMESTAMPTZ,

                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);

COMMENT ON TABLE refresh_tokens IS 'Refresh-токены (хранится хеш, не сам токен)';
COMMENT ON COLUMN refresh_tokens.revoked IS 'TRUE — токен отозван (logout, ротация или компрометация)';