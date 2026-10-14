-- V5__teams.sql
-- Команды, участники, приглашения, передача владения.
-- Также добавляем часовой пояс в regions (нужно для расчёта дат начислений).

-- ============================================================
-- 1. Добавляем часовой пояс в regions
-- ============================================================
ALTER TABLE regions ADD COLUMN timezone VARCHAR(50);

UPDATE regions SET timezone = 'Europe/Moscow'
WHERE code IN ('RU-MOW', 'RU-SPE', 'RU-MOS', 'RU-LEN', 'RU-KDA', 'RU-TA', 'RU-ROS');

UPDATE regions SET timezone = 'Asia/Yekaterinburg'
WHERE code IN ('RU-SVE', 'RU-CHE');

UPDATE regions SET timezone = 'Asia/Novosibirsk'
WHERE code = 'RU-NVS';

ALTER TABLE regions ALTER COLUMN timezone SET NOT NULL;

COMMENT ON COLUMN regions.timezone IS 'IANA-идентификатор часового пояса (Europe/Moscow и т.д.)';


-- ============================================================
-- 2. teams — команды
-- ============================================================
CREATE TABLE teams (
                       id              BIGSERIAL       PRIMARY KEY,
                       name            VARCHAR(150)    NOT NULL,
                       city_id         BIGINT          NOT NULL,
                       home_region_id  BIGINT          NOT NULL,
                       logo_url        VARCHAR(500)    NOT NULL,
                       description     TEXT,
                       season          VARCHAR(50),
                       status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       archived_at     TIMESTAMPTZ,

                       CONSTRAINT fk_teams_city        FOREIGN KEY (city_id)        REFERENCES cities  (id) ON DELETE RESTRICT,
                       CONSTRAINT fk_teams_home_region FOREIGN KEY (home_region_id) REFERENCES regions (id) ON DELETE RESTRICT,
                       CONSTRAINT ck_teams_status      CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_teams_city   ON teams (city_id);
CREATE INDEX idx_teams_status ON teams (status);
CREATE INDEX idx_teams_region ON teams (home_region_id);

COMMENT ON TABLE teams IS 'Хоккейные команды';
COMMENT ON COLUMN teams.home_region_id IS 'Домашний регион — определяет часовой пояс для начислений';
COMMENT ON COLUMN teams.logo_url IS 'URL логотипа команды';
COMMENT ON COLUMN teams.season IS 'Сезон (например, "2025/2026"), опционально';


-- ============================================================
-- 3. team_members — участники команды
-- ============================================================
CREATE TABLE team_members (
                              id              BIGSERIAL       PRIMARY KEY,
                              team_id         BIGINT          NOT NULL,
                              user_id         BIGINT          NOT NULL,
                              role            VARCHAR(20)     NOT NULL,
                              status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                              jersey_number   SMALLINT,
                              joined_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                              left_at         TIMESTAMPTZ,
                              excluded_at     TIMESTAMPTZ,

                              CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
                              CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                              CONSTRAINT ck_team_members_role   CHECK (role   IN ('OWNER', 'MODERATOR', 'MEMBER')),
                              CONSTRAINT ck_team_members_status CHECK (status IN ('ACTIVE', 'EXCLUDED', 'LEFT')),
                              CONSTRAINT ck_team_members_jersey CHECK (jersey_number IS NULL OR (jersey_number BETWEEN 1 AND 99))
);

CREATE UNIQUE INDEX uq_team_members_team_user ON team_members (team_id, user_id);

CREATE UNIQUE INDEX uq_team_members_jersey
    ON team_members (team_id, jersey_number)
    WHERE jersey_number IS NOT NULL;

CREATE UNIQUE INDEX uq_team_members_one_owner
    ON team_members (team_id)
    WHERE role = 'OWNER' AND status = 'ACTIVE';

CREATE INDEX idx_team_members_team ON team_members (team_id);
CREATE INDEX idx_team_members_user ON team_members (user_id);

COMMENT ON TABLE team_members IS 'Участники команд с ролями и игровыми номерами';
COMMENT ON COLUMN team_members.role IS 'OWNER — владелец (один), MODERATOR — модератор (несколько), MEMBER — обычный';
COMMENT ON COLUMN team_members.status IS 'ACTIVE, EXCLUDED (исключён — долги сохраняются), LEFT (вышел сам)';


-- ============================================================
-- 4. team_invitations — приглашения в команду
-- ============================================================
CREATE TABLE team_invitations (
                                  id                  BIGSERIAL       PRIMARY KEY,
                                  team_id             BIGINT          NOT NULL,
                                  inviter_user_id     BIGINT          NOT NULL,
                                  invited_user_id     BIGINT          NOT NULL,
                                  invited_phone       VARCHAR(20)     NOT NULL,
                                  status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                                  created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  expires_at          TIMESTAMPTZ     NOT NULL,
                                  responded_at        TIMESTAMPTZ,

                                  CONSTRAINT fk_team_invitations_team    FOREIGN KEY (team_id)         REFERENCES teams (id) ON DELETE CASCADE,
                                  CONSTRAINT fk_team_invitations_inviter FOREIGN KEY (inviter_user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  CONSTRAINT fk_team_invitations_invited FOREIGN KEY (invited_user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  CONSTRAINT ck_team_invitations_status  CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_team_invitations_pending
    ON team_invitations (team_id, invited_user_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_team_invitations_team    ON team_invitations (team_id);
CREATE INDEX idx_team_invitations_invited ON team_invitations (invited_user_id);
CREATE INDEX idx_team_invitations_status  ON team_invitations (status);

COMMENT ON TABLE team_invitations IS 'Приглашения в команду (только зарегистрированным пользователям)';
COMMENT ON COLUMN team_invitations.invited_phone IS 'Снапшот номера на момент приглашения (не меняется при смене номера у user)';
COMMENT ON COLUMN team_invitations.expires_at IS 'Срок действия — 7 дней с момента создания';


-- ============================================================
-- 5. ownership_transfers — история передачи владения
-- ============================================================
CREATE TABLE ownership_transfers (
                                     id              BIGSERIAL       PRIMARY KEY,
                                     team_id         BIGINT          NOT NULL,
                                     from_user_id    BIGINT          NOT NULL,
                                     to_user_id      BIGINT          NOT NULL,
                                     transferred_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                     CONSTRAINT fk_ownership_transfers_team FOREIGN KEY (team_id)      REFERENCES teams (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_ownership_transfers_from FOREIGN KEY (from_user_id) REFERENCES users (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_ownership_transfers_to   FOREIGN KEY (to_user_id)   REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_ownership_transfers_team ON ownership_transfers (team_id);

COMMENT ON TABLE ownership_transfers IS 'История передачи владения командой';