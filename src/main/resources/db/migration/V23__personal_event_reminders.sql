CREATE TABLE user_notification_settings (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    reminders_enabled BOOLEAN NOT NULL DEFAULT true,
    reminder_count INTEGER NOT NULL DEFAULT 1 CHECK (reminder_count BETWEEN 1 AND 10),
    lead_time_minutes INTEGER NOT NULL DEFAULT 1440 CHECK (lead_time_minutes BETWEEN 1 AND 43200),
    interval_minutes INTEGER NOT NULL DEFAULT 60 CHECK (interval_minutes BETWEEN 1 AND 43200),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE event_reminder_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    reminder_index INTEGER NOT NULL,
    scheduled_for TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_reminder UNIQUE (user_id, event_id, reminder_index)
);
CREATE INDEX idx_event_reminder_event ON event_reminder_log(event_id);
