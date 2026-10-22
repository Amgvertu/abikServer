-- V13__championship_roster.sql
-- Состав регулярного чемпионата по сезонам.

CREATE TABLE championship_roster (
                                     id          BIGSERIAL       PRIMARY KEY,
                                     team_id     BIGINT          NOT NULL,
                                     season      VARCHAR(20)     NOT NULL,
                                     user_id     BIGINT          NOT NULL,
                                     status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                                     added_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                     removed_at  TIMESTAMPTZ,

                                     CONSTRAINT fk_championship_roster_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_championship_roster_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                     CONSTRAINT ck_championship_roster_status CHECK (status IN ('ACTIVE', 'REMOVED'))
);

CREATE UNIQUE INDEX uq_championship_roster_team_season_user
    ON championship_roster (team_id, season, user_id);

CREATE INDEX idx_championship_roster_team_season
    ON championship_roster (team_id, season);

COMMENT ON TABLE championship_roster IS
    'Состав игроков регулярного чемпионата по сезонам. REMOVED — история';