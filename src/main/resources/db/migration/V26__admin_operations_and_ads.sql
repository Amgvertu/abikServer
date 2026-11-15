-- Администрирование: роли, блокировки, рассылки, реклама и журнал регистраций.

ALTER TABLE users ADD COLUMN is_ad_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN block_reason VARCHAR(500);
ALTER TABLE users ADD COLUMN blocked_at TIMESTAMPTZ;

ALTER TABLE teams ADD COLUMN block_reason VARCHAR(500);
ALTER TABLE teams ADD COLUMN blocked_at TIMESTAMPTZ;
ALTER TABLE teams DROP CONSTRAINT ck_teams_status;
ALTER TABLE teams ADD CONSTRAINT ck_teams_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'BLOCKED'));

CREATE TABLE admin_messages (
    id BIGSERIAL PRIMARY KEY,
    sender_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    link_url VARCHAR(2000),
    region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL,
    city_id BIGINT REFERENCES cities(id) ON DELETE SET NULL,
    team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    position VARCHAR(30),
    recipient_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_messages_created ON admin_messages(created_at DESC);
CREATE INDEX idx_admin_messages_sender ON admin_messages(sender_user_id);

CREATE TABLE admin_message_recipients (
    message_id BIGINT NOT NULL REFERENCES admin_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_id BIGINT REFERENCES notifications(id) ON DELETE SET NULL,
    read_at TIMESTAMPTZ,
    PRIMARY KEY(message_id, user_id)
);
CREATE INDEX idx_admin_message_recipients_user ON admin_message_recipients(user_id);

CREATE TABLE advertisements (
    id BIGSERIAL PRIMARY KEY,
    creator_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(100),
    body VARCHAR(1000),
    image_url VARCHAR(500),
    link_url VARCHAR(2000),
    button_text VARCHAR(100),
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_ad_target CHECK (target_type IN ('GLOBAL', 'REGION', 'CITY', 'TEAM')),
    CONSTRAINT ck_ad_target_value CHECK ((target_type = 'GLOBAL' AND target_id IS NULL) OR (target_type <> 'GLOBAL' AND target_id IS NOT NULL)),
    CONSTRAINT ck_ad_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_ad_content CHECK (NULLIF(BTRIM(title), '') IS NOT NULL OR NULLIF(BTRIM(body), '') IS NOT NULL OR NULLIF(BTRIM(image_url), '') IS NOT NULL),
    CONSTRAINT ck_ad_link_button CHECK (NULLIF(BTRIM(link_url), '') IS NULL OR NULLIF(BTRIM(button_text), '') IS NOT NULL)
);
CREATE INDEX idx_ads_active_period ON advertisements(active, starts_at, ends_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_ads_creator ON advertisements(creator_user_id);
CREATE INDEX idx_ads_target ON advertisements(target_type, target_id) WHERE deleted_at IS NULL;

CREATE TABLE advertisement_events (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(10) NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL,
    city_id BIGINT REFERENCES cities(id) ON DELETE SET NULL,
    team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_ad_event_type CHECK (event_type IN ('VIEW', 'CLICK'))
);
CREATE UNIQUE INDEX uq_ad_view_session ON advertisement_events(ad_id, user_id, session_id) WHERE event_type = 'VIEW';
CREATE INDEX idx_ad_events_ad_time ON advertisement_events(ad_id, occurred_at);
CREATE INDEX idx_ad_events_geo ON advertisement_events(region_id, city_id, team_id);

-- В журнал попадают только технические данные: пароли и SMS-коды запрещены контрактом API.
CREATE TABLE registration_events (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(100),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    phone_masked VARCHAR(30),
    stage VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    code VARCHAR(80),
    details VARCHAR(1000),
    client VARCHAR(30),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_registration_events_time ON registration_events(occurred_at DESC);
CREATE INDEX idx_registration_events_request ON registration_events(request_id);
CREATE INDEX idx_registration_events_status ON registration_events(status, occurred_at);

CREATE TABLE admin_alert_log (
    id BIGSERIAL PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_alert_log_type_time ON admin_alert_log(alert_type, created_at DESC);
