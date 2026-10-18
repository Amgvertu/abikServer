-- V9__admin_and_adjustments.sql
-- Добавляем флаг суперадмина и таблицу корректировок баланса.

-- ============================================================
-- 1. Суперадмин в users
-- ============================================================
ALTER TABLE users ADD COLUMN is_super_admin BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.is_super_admin IS 'TRUE, если пользователь является суперадмином';

-- ============================================================
-- 2. balance_adjustments — ручные корректировки баланса
-- ============================================================
CREATE TABLE balance_adjustments (
                                     id                  BIGSERIAL       PRIMARY KEY,
                                     team_id             BIGINT          NOT NULL,
                                     user_id             BIGINT          NOT NULL,
                                     amount_kopecks      BIGINT          NOT NULL,   -- + переплата, - долг
                                     reason              VARCHAR(500)    NOT NULL,
                                     created_by_user_id  BIGINT          NOT NULL,
                                     created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                     CONSTRAINT fk_ba_team       FOREIGN KEY (team_id)            REFERENCES teams (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_ba_user       FOREIGN KEY (user_id)            REFERENCES users (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_ba_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_ba_team_user ON balance_adjustments (team_id, user_id);

COMMENT ON TABLE balance_adjustments IS 'Ручные корректировки баланса участников';
COMMENT ON COLUMN balance_adjustments.amount_kopecks IS 'Сумма в копейках: положительная — переплата, отрицательная — долг';