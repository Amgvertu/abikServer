-- V16__events_ext.sql
-- 1. Разрешаем секцию SCRIMMAGE (Двухсторонка) в событиях и сериях
ALTER TABLE events DROP CONSTRAINT ck_events_section;
ALTER TABLE events ADD CONSTRAINT ck_events_section
    CHECK (section IN ('TRAINING', 'REGULAR', 'SCRIMMAGE'));

ALTER TABLE event_series DROP CONSTRAINT ck_event_series_section;
ALTER TABLE event_series ADD CONSTRAINT ck_event_series_section
    CHECK (section IN ('TRAINING', 'REGULAR', 'SCRIMMAGE'));

-- 2. Новые поля события
ALTER TABLE events ADD COLUMN required_players INTEGER;
ALTER TABLE events ADD COLUMN charge_mode VARCHAR(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE events ADD COLUMN charge_amount_kopecks BIGINT;
ALTER TABLE events ADD COLUMN charge_distribution VARCHAR(20);
ALTER TABLE events ADD COLUMN charge_template_id BIGINT;
ALTER TABLE events ADD COLUMN charged_at TIMESTAMPTZ;

COMMENT ON COLUMN events.required_players IS 'Требуемое кол-во игроков (регулярка/двухсторонка); сверх лимита — резерв';
COMMENT ON COLUMN events.charge_mode IS 'NONE, MANUAL, TEMPLATE';
COMMENT ON COLUMN events.charge_distribution IS 'EACH_ROSTER, EACH_ATTENDED, SPLIT_ATTENDED, SPLIT_ROSTER';
COMMENT ON COLUMN events.charged_at IS 'Когда выполнено начисление (защита от повтора)';