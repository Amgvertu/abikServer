ALTER TABLE events
    ADD COLUMN voter_list_id BIGINT REFERENCES charge_lists(id) ON DELETE SET NULL;

CREATE INDEX idx_events_voter_list ON events(voter_list_id);

CREATE TABLE holiday_notification_log (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    celebrated_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    holiday_type VARCHAR(30) NOT NULL,
    stage VARCHAR(20) NOT NULL,
    holiday_year INTEGER NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_holiday_notification UNIQUE (
        recipient_user_id, celebrated_user_id, team_id,
        holiday_type, stage, holiday_year
    )
);

CREATE INDEX idx_holiday_notification_celebrated
    ON holiday_notification_log(celebrated_user_id, holiday_type, holiday_year);

COMMENT ON COLUMN notifications.type IS
    'Тип уведомления, включая HOLIDAY_REMINDER и HOLIDAY_GREETING';
