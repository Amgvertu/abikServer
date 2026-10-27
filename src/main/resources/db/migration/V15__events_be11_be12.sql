-- V15__events_be11_be12.sql
-- События: title + section, MAYBE в голосовании, until_date в сериях, slot_number в lineup.

-- 1. Название и раздел события
ALTER TABLE events ADD COLUMN title VARCHAR(200);
UPDATE events SET title = '' WHERE title IS NULL;
ALTER TABLE events ALTER COLUMN title SET NOT NULL;
ALTER TABLE events ALTER COLUMN title SET DEFAULT '';

ALTER TABLE events ADD COLUMN section VARCHAR(20);
UPDATE events SET section = CASE WHEN type = 'TRAINING' THEN 'TRAINING' ELSE 'REGULAR' END;
ALTER TABLE events ALTER COLUMN section SET NOT NULL;
ALTER TABLE events ADD CONSTRAINT ck_events_section CHECK (section IN ('TRAINING', 'REGULAR'));

-- 2. Название, раздел и дата окончания серии
ALTER TABLE event_series ADD COLUMN title VARCHAR(200);
UPDATE event_series SET title = '' WHERE title IS NULL;
ALTER TABLE event_series ALTER COLUMN title SET NOT NULL;
ALTER TABLE event_series ALTER COLUMN title SET DEFAULT '';

ALTER TABLE event_series ADD COLUMN section VARCHAR(20);
UPDATE event_series SET section = CASE WHEN type = 'TRAINING' THEN 'TRAINING' ELSE 'REGULAR' END;
ALTER TABLE event_series ALTER COLUMN section SET NOT NULL;
ALTER TABLE event_series ADD CONSTRAINT ck_event_series_section CHECK (section IN ('TRAINING', 'REGULAR'));

ALTER TABLE event_series ADD COLUMN until_date DATE;
UPDATE event_series SET until_date = created_at::date + (weeks_ahead * 7);
ALTER TABLE event_series ALTER COLUMN until_date SET NOT NULL;
ALTER TABLE event_series DROP COLUMN weeks_ahead;

-- 3. Третий ответ в голосовании
ALTER TABLE event_votes DROP CONSTRAINT ck_event_votes_answer;
ALTER TABLE event_votes ADD CONSTRAINT ck_event_votes_answer CHECK (answer IN ('YES', 'NO', 'MAYBE'));

-- 4. Номер слота в заявке на игру
ALTER TABLE event_lineups ADD COLUMN slot_number SMALLINT NOT NULL DEFAULT 1;
CREATE INDEX idx_event_lineups_order ON event_lineups (event_id, line_number, slot_number);