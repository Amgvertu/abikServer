ALTER TABLE events
    ADD COLUMN exclude_subscription_holders BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE charge_templates
    ADD COLUMN poll_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN poll_start_day SMALLINT,
    ADD COLUMN poll_end_day SMALLINT;

ALTER TABLE charge_templates
    ADD CONSTRAINT ck_charge_templates_poll_days CHECK (
        (poll_start_day IS NULL AND poll_end_day IS NULL)
        OR (poll_start_day BETWEEN 1 AND 31 AND poll_end_day BETWEEN 1 AND 31)
    ),
    ADD CONSTRAINT ck_charge_templates_poll_order CHECK (
        poll_start_day IS NULL OR poll_end_day IS NULL OR poll_end_day >= poll_start_day
    );

CREATE UNIQUE INDEX uq_charge_templates_active_poll
    ON charge_templates (team_id)
    WHERE poll_enabled = TRUE AND active = TRUE;

CREATE TABLE subscription_polls (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    group_id BIGINT NOT NULL,
    amount_kopecks BIGINT NOT NULL,
    distribution VARCHAR(20) NOT NULL,
    period_year SMALLINT NOT NULL,
    period_month SMALLINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_subscription_polls_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_polls_template FOREIGN KEY (template_id) REFERENCES charge_templates (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_polls_group FOREIGN KEY (group_id) REFERENCES charge_groups (id) ON DELETE RESTRICT,
    CONSTRAINT uq_subscription_polls_period UNIQUE (template_id, period_year, period_month),
    CONSTRAINT ck_subscription_polls_amount CHECK (amount_kopecks > 0),
    CONSTRAINT ck_subscription_polls_distribution CHECK (distribution IN ('EQUAL', 'FIXED')),
    CONSTRAINT ck_subscription_polls_dates CHECK (end_date >= start_date AND valid_until >= valid_from)
);

CREATE INDEX idx_subscription_polls_team_dates ON subscription_polls (team_id, start_date, end_date);
CREATE INDEX idx_subscription_polls_unprocessed ON subscription_polls (processed_at, end_date);

CREATE TABLE subscription_poll_votes (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    answer VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_subscription_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES subscription_polls (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_poll_votes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_subscription_poll_votes_user UNIQUE (poll_id, user_id),
    CONSTRAINT ck_subscription_poll_votes_answer CHECK (answer IN ('YES', 'NO'))
);

CREATE INDEX idx_subscription_poll_votes_poll_answer ON subscription_poll_votes (poll_id, answer);
