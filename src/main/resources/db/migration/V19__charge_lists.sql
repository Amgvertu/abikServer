-- Именованные списки получателей начислений (п.9).
-- Список = название + набор участников команды; используется как быстрый
-- выбор получателей при создании начислений и шаблонов.
CREATE TABLE charge_lists (
    id         BIGSERIAL PRIMARY KEY,
    team_id    BIGINT       NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_charge_lists_team ON charge_lists(team_id);

CREATE TABLE charge_list_members (
    list_id BIGINT NOT NULL REFERENCES charge_lists(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (list_id, user_id)
);
