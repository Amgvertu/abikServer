-- Список получателей ручного начисления за событие.
-- Если список задан, начисление «вручную» идёт именно этим участникам,
-- независимо от отметившихся/заявленных.
CREATE TABLE event_charge_members (
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id  BIGINT NOT NULL,
    PRIMARY KEY (event_id, user_id)
);
