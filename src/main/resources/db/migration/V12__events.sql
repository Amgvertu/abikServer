-- V12__events.sql
-- Тренировки, игры, матчи: серии, события, голоса, цвета свитеров, звенья.

-- 1. Серии (для регулярных событий)
CREATE TABLE event_series (
                              id          BIGSERIAL       PRIMARY KEY,
                              team_id     BIGINT          NOT NULL,
                              type        VARCHAR(20)     NOT NULL,
                              time_of_day VARCHAR(5)      NOT NULL,
                              place       VARCHAR(500)    NOT NULL,
                              note        VARCHAR(1000),
                              weekdays    SMALLINT[]      NOT NULL,
                              weeks_ahead INT             NOT NULL,
                              created_by  BIGINT          NOT NULL,
                              created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                              CONSTRAINT fk_event_series_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
                              CONSTRAINT fk_event_series_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
                              CONSTRAINT ck_event_series_type CHECK (type IN ('TRAINING', 'GAME', 'MATCH'))
);

CREATE INDEX idx_event_series_team ON event_series (team_id);

-- 2. События
CREATE TABLE events (
                        id          BIGSERIAL       PRIMARY KEY,
                        team_id     BIGINT          NOT NULL,
                        series_id   BIGINT,
                        type        VARCHAR(20)     NOT NULL,
                        event_date  DATE            NOT NULL,
                        time_of_day VARCHAR(5)      NOT NULL,
                        place       VARCHAR(500)    NOT NULL,
                        note        VARCHAR(1000),
                        status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                        created_by  BIGINT          NOT NULL,
                        created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                        updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                        CONSTRAINT fk_events_team      FOREIGN KEY (team_id)    REFERENCES teams (id)        ON DELETE CASCADE,
                        CONSTRAINT fk_events_series    FOREIGN KEY (series_id)  REFERENCES event_series (id) ON DELETE SET NULL,
                        CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users (id)        ON DELETE RESTRICT,
                        CONSTRAINT ck_events_type   CHECK (type   IN ('TRAINING', 'GAME', 'MATCH')),
                        CONSTRAINT ck_events_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

CREATE INDEX idx_events_team_date ON events (team_id, event_date);
CREATE INDEX idx_events_series    ON events (series_id);

-- 3. Голоса
CREATE TABLE event_votes (
                             event_id    BIGINT          NOT NULL,
                             user_id     BIGINT          NOT NULL,
                             answer      VARCHAR(10)     NOT NULL,
                             updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                             CONSTRAINT pk_event_votes PRIMARY KEY (event_id, user_id),
                             CONSTRAINT fk_event_votes_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
                             CONSTRAINT fk_event_votes_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
                             CONSTRAINT ck_event_votes_answer CHECK (answer IN ('YES', 'NO'))
);

-- 4. Цвета свитеров
CREATE TABLE event_sweaters (
                                event_id    BIGINT          NOT NULL,
                                user_id     BIGINT          NOT NULL,
                                color       VARCHAR(20)     NOT NULL,

                                CONSTRAINT pk_event_sweaters PRIMARY KEY (event_id, user_id),
                                CONSTRAINT fk_event_sweaters_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
                                CONSTRAINT fk_event_sweaters_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
                                CONSTRAINT ck_event_sweaters_color CHECK (color IN ('WHITE', 'BLACK', 'RED'))
);

-- 5. Звенья (lineup)
CREATE TABLE event_lineups (
                               id          BIGSERIAL       PRIMARY KEY,
                               event_id    BIGINT          NOT NULL,
                               user_id     BIGINT          NOT NULL,
                               role        VARCHAR(20)     NOT NULL,
                               line_number SMALLINT,

                               CONSTRAINT fk_event_lineups_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
                               CONSTRAINT fk_event_lineups_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
                               CONSTRAINT ck_event_lineups_role CHECK (role IN ('GOALKEEPER', 'DEFENDER', 'FORWARD')),
                               CONSTRAINT ck_event_lineups_line CHECK (line_number IS NULL OR line_number BETWEEN 1 AND 99)
);

CREATE INDEX idx_event_lineups_event ON event_lineups (event_id);