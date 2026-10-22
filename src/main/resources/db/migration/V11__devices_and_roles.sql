-- V11__devices_and_roles.sql
-- FCM-токены устройств, роль ADMIN в команде, флаг тренера.

-- 1. Устройства для push
CREATE TABLE user_devices (
                              id           BIGSERIAL       PRIMARY KEY,
                              user_id      BIGINT          NOT NULL,
                              token        VARCHAR(500)    NOT NULL,
                              platform     VARCHAR(20)     NOT NULL,
                              created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                              last_seen_at TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                              CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                              CONSTRAINT uq_user_devices_token UNIQUE (token),
                              CONSTRAINT ck_user_devices_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

CREATE INDEX idx_user_devices_user ON user_devices (user_id);
COMMENT ON TABLE user_devices IS 'Токены устройств для отправки FCM push';

-- 2. Роль ADMIN
ALTER TABLE team_members DROP CONSTRAINT ck_team_members_role;
ALTER TABLE team_members ADD CONSTRAINT ck_team_members_role
    CHECK (role IN ('OWNER', 'ADMIN', 'MODERATOR', 'MEMBER'));

COMMENT ON COLUMN team_members.role IS
    'OWNER — владелец, ADMIN — админ команды, MODERATOR — модератор, MEMBER — участник';

-- 3. Флаг тренера
ALTER TABLE team_members ADD COLUMN is_trainer BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN team_members.is_trainer IS 'Флаг «тренер» — назначается владельцем/админом';