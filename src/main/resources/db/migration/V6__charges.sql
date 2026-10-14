-- V6__charges.sql
-- Модуль начислений: группы, подгруппы, шаблоны, начисления.

-- ============================================================
-- 1. charge_groups — группы начислений (внутри команды)
-- ============================================================
CREATE TABLE charge_groups (
                               id              BIGSERIAL       PRIMARY KEY,
                               team_id         BIGINT          NOT NULL,
                               name            VARCHAR(150)    NOT NULL,
                               created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                               CONSTRAINT fk_charge_groups_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
                               CONSTRAINT uq_charge_groups_team_name UNIQUE (team_id, name)
);

CREATE INDEX idx_charge_groups_team ON charge_groups (team_id);

COMMENT ON TABLE charge_groups IS 'Группы начислений внутри команды (например: "Лёд", "Форма", "Взносы")';


-- ============================================================
-- 2. charge_subgroups — подгруппы (внутри группы)
-- ============================================================
CREATE TABLE charge_subgroups (
                                  id              BIGSERIAL       PRIMARY KEY,
                                  group_id        BIGINT          NOT NULL,
                                  name            VARCHAR(150)    NOT NULL,
                                  created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                  CONSTRAINT fk_charge_subgroups_group FOREIGN KEY (group_id) REFERENCES charge_groups (id) ON DELETE CASCADE,
                                  CONSTRAINT uq_charge_subgroups_group_name UNIQUE (group_id, name)
);

CREATE INDEX idx_charge_subgroups_group ON charge_subgroups (group_id);

COMMENT ON TABLE charge_subgroups IS 'Подгруппы внутри групп (например: "Основной состав", "Вратари")';


-- ============================================================
-- 3. charge_templates — шаблоны начислений
-- ============================================================
CREATE TABLE charge_templates (
                                  id              BIGSERIAL       PRIMARY KEY,
                                  team_id         BIGINT          NOT NULL,
                                  group_id        BIGINT          NOT NULL,
                                  subgroup_id     BIGINT,
                                  name            VARCHAR(200)    NOT NULL,
                                  amount_kopecks  BIGINT          NOT NULL,
                                  type            VARCHAR(20)     NOT NULL,
                                  periodicity     VARCHAR(20)     NOT NULL,
                                  day_of_month    SMALLINT,
                                  start_date      DATE,
                                  end_date        DATE,
                                  distribution    VARCHAR(20)     NOT NULL,
                                  active          BOOLEAN         NOT NULL DEFAULT TRUE,
                                  created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                  CONSTRAINT fk_charge_templates_team     FOREIGN KEY (team_id)     REFERENCES teams (id)          ON DELETE CASCADE,
                                  CONSTRAINT fk_charge_templates_group    FOREIGN KEY (group_id)    REFERENCES charge_groups (id)  ON DELETE RESTRICT,
                                  CONSTRAINT fk_charge_templates_subgroup FOREIGN KEY (subgroup_id) REFERENCES charge_subgroups (id) ON DELETE RESTRICT,
                                  CONSTRAINT ck_charge_templates_amount   CHECK (amount_kopecks > 0),
                                  CONSTRAINT ck_charge_templates_type     CHECK (type IN ('SYSTEMATIC', 'ONE_TIME')),
                                  CONSTRAINT ck_charge_templates_period   CHECK (periodicity IN ('MONTHLY', 'ONE_TIME')),
                                  CONSTRAINT ck_charge_templates_distr    CHECK (distribution IN ('EQUAL', 'FIXED')),
                                  CONSTRAINT ck_charge_templates_dom      CHECK (day_of_month IS NULL OR (day_of_month BETWEEN 1 AND 28)),
                                  CONSTRAINT ck_charge_templates_dates    CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
                                  CONSTRAINT ck_charge_templates_systematic CHECK (
                                      (type = 'SYSTEMATIC' AND periodicity = 'MONTHLY' AND day_of_month IS NOT NULL)
                                          OR
                                      (type = 'ONE_TIME' AND periodicity = 'ONE_TIME')
                                      )
);

CREATE INDEX idx_charge_templates_team ON charge_templates (team_id);
CREATE INDEX idx_charge_templates_group ON charge_templates (group_id);
CREATE INDEX idx_charge_templates_active ON charge_templates (team_id, active);

COMMENT ON TABLE charge_templates IS 'Шаблоны начислений (правила)';
COMMENT ON COLUMN charge_templates.amount_kopecks IS 'Сумма в копейках (в копейках, чтобы избежать float)';
COMMENT ON COLUMN charge_templates.type IS 'SYSTEMATIC — систематическое, ONE_TIME — разовое';
COMMENT ON COLUMN charge_templates.periodicity IS 'MONTHLY (ежемесячно) или ONE_TIME (однократно)';
COMMENT ON COLUMN charge_templates.day_of_month IS 'День месяца 1-28, только для SYSTEMATIC';
COMMENT ON COLUMN charge_templates.distribution IS 'EQUAL — поровну между участниками, FIXED — фиксированная сумма с каждого';


-- ============================================================
-- 4. charge_template_members — участники шаблона
-- ============================================================
CREATE TABLE charge_template_members (
                                         template_id     BIGINT          NOT NULL,
                                         user_id         BIGINT          NOT NULL,

                                         CONSTRAINT pk_charge_template_members PRIMARY KEY (template_id, user_id),
                                         CONSTRAINT fk_ctm_template FOREIGN KEY (template_id) REFERENCES charge_templates (id) ON DELETE CASCADE,
                                         CONSTRAINT fk_ctm_user     FOREIGN KEY (user_id)     REFERENCES users (id)            ON DELETE CASCADE
);

CREATE INDEX idx_ctm_user ON charge_template_members (user_id);

COMMENT ON TABLE charge_template_members IS 'Список участников, к которым применяется шаблон';


-- ============================================================
-- 5. charges — начисления
-- ============================================================
CREATE TABLE charges (
                         id              BIGSERIAL       PRIMARY KEY,
                         team_id         BIGINT          NOT NULL,
                         template_id     BIGINT,
                         group_id        BIGINT          NOT NULL,
                         subgroup_id     BIGINT,
                         user_id         BIGINT          NOT NULL,
                         amount_kopecks  BIGINT          NOT NULL,
                         paid_kopecks    BIGINT          NOT NULL DEFAULT 0,
                         status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                         description     VARCHAR(500),
                         due_date        DATE            NOT NULL,
                         period_year     SMALLINT,
                         period_month    SMALLINT,
                         created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                         updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                         cancelled_at    TIMESTAMPTZ,
                         cancel_reason   VARCHAR(500),

                         CONSTRAINT fk_charges_team     FOREIGN KEY (team_id)     REFERENCES teams (id)             ON DELETE CASCADE,
                         CONSTRAINT fk_charges_template FOREIGN KEY (template_id) REFERENCES charge_templates (id)  ON DELETE SET NULL,
                         CONSTRAINT fk_charges_group    FOREIGN KEY (group_id)    REFERENCES charge_groups (id)     ON DELETE RESTRICT,
                         CONSTRAINT fk_charges_subgroup FOREIGN KEY (subgroup_id) REFERENCES charge_subgroups (id)  ON DELETE RESTRICT,
                         CONSTRAINT fk_charges_user     FOREIGN KEY (user_id)     REFERENCES users (id)             ON DELETE CASCADE,
                         CONSTRAINT ck_charges_amount   CHECK (amount_kopecks > 0),
                         CONSTRAINT ck_charges_paid     CHECK (paid_kopecks >= 0 AND paid_kopecks <= amount_kopecks),
                         CONSTRAINT ck_charges_status   CHECK (status IN ('ACTIVE', 'PARTIALLY_PAID', 'PAID', 'CANCELLED', 'ARCHIVED')),
                         CONSTRAINT ck_charges_month    CHECK (period_month IS NULL OR period_month BETWEEN 1 AND 12)
);

CREATE INDEX idx_charges_team    ON charges (team_id);
CREATE INDEX idx_charges_user    ON charges (user_id);
CREATE INDEX idx_charges_group   ON charges (group_id);
CREATE INDEX idx_charges_status  ON charges (status);
CREATE INDEX idx_charges_due     ON charges (due_date);
CREATE INDEX idx_charges_template ON charges (template_id);

-- Уникальность: одно начисление на (шаблон, user, год, месяц)
-- Иначе генерация может создать дубли
CREATE UNIQUE INDEX uq_charges_template_period
    ON charges (template_id, user_id, period_year, period_month)
    WHERE template_id IS NOT NULL AND period_year IS NOT NULL AND period_month IS NOT NULL;

COMMENT ON TABLE charges IS 'Начисления — конкретный долг пользователя перед командой';
COMMENT ON COLUMN charges.amount_kopecks IS 'Сумма долга в копейках';
COMMENT ON COLUMN charges.paid_kopecks IS 'Оплачено в копейках';
COMMENT ON COLUMN charges.status IS 'ACTIVE, PARTIALLY_PAID, PAID, CANCELLED, ARCHIVED';
COMMENT ON COLUMN charges.period_year IS 'Год периода (для систематических)';
COMMENT ON COLUMN charges.period_month IS 'Месяц периода 1-12 (для систематических)';