-- V7__payments.sql
-- Модуль платежей: платежи, их распределение по начислениям, балансы.

-- ============================================================
-- 1. payments — внесённые платежи
-- ============================================================
CREATE TABLE payments (
                          id                  BIGSERIAL       PRIMARY KEY,
                          team_id             BIGINT          NOT NULL,
                          user_id             BIGINT          NOT NULL,   -- за кого внесён платёж
                          amount_kopecks      BIGINT          NOT NULL,
                          description         VARCHAR(500),
                          created_by_user_id  BIGINT          NOT NULL,   -- кто внёс (владелец/модератор)
                          created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                          updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                          deleted_at          TIMESTAMPTZ,

                          CONSTRAINT fk_payments_team       FOREIGN KEY (team_id)             REFERENCES teams (id) ON DELETE CASCADE,
                          CONSTRAINT fk_payments_user       FOREIGN KEY (user_id)             REFERENCES users (id) ON DELETE CASCADE,
                          CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by_user_id)  REFERENCES users (id) ON DELETE RESTRICT,
                          CONSTRAINT ck_payments_amount     CHECK (amount_kopecks > 0)
);

CREATE INDEX idx_payments_team       ON payments (team_id);
CREATE INDEX idx_payments_user       ON payments (user_id);
CREATE INDEX idx_payments_team_user  ON payments (team_id, user_id);
CREATE INDEX idx_payments_created_at ON payments (created_at);
CREATE INDEX idx_payments_deleted    ON payments (deleted_at);

COMMENT ON TABLE payments IS 'Платежи участников в команду';
COMMENT ON COLUMN payments.user_id IS 'За какого участника внесён платёж';
COMMENT ON COLUMN payments.created_by_user_id IS 'Кто внёс (владелец/модератор)';
COMMENT ON COLUMN payments.deleted_at IS 'Soft-delete: NULL — активен';


-- ============================================================
-- 2. payment_allocations — распределение платежа по начислениям
-- ============================================================
CREATE TABLE payment_allocations (
                                     id              BIGSERIAL       PRIMARY KEY,
                                     payment_id      BIGINT          NOT NULL,
                                     charge_id       BIGINT          NOT NULL,
                                     amount_kopecks  BIGINT          NOT NULL,
                                     created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                     CONSTRAINT fk_pa_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_pa_charge  FOREIGN KEY (charge_id)  REFERENCES charges  (id) ON DELETE CASCADE,
                                     CONSTRAINT ck_pa_amount  CHECK (amount_kopecks > 0)
);

CREATE INDEX idx_pa_payment ON payment_allocations (payment_id);
CREATE INDEX idx_pa_charge  ON payment_allocations (charge_id);

COMMENT ON TABLE payment_allocations IS 'Распределение платежа по конкретным начислениям';


-- ============================================================
-- 3. balances — кеш баланса участника в команде
-- ============================================================
CREATE TABLE balances (
                          team_id         BIGINT          NOT NULL,
                          user_id         BIGINT          NOT NULL,
                          amount_kopecks  BIGINT          NOT NULL DEFAULT 0,
                          updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                          CONSTRAINT pk_balances PRIMARY KEY (team_id, user_id),
                          CONSTRAINT fk_balances_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
                          CONSTRAINT fk_balances_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE balances IS 'Кеш баланса: платежи − активные начисления. Отрицательный — долг, положительный — переплата';
COMMENT ON COLUMN balances.amount_kopecks IS 'Баланс в копейках';